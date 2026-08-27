package com.kizen.tasks.ui.task

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kizen.tasks.domain.model.Priority
import com.kizen.tasks.domain.model.Subtask
import com.kizen.tasks.domain.model.Task
import com.kizen.tasks.domain.model.TaskList
import com.kizen.tasks.domain.repository.ListRepository
import com.kizen.tasks.domain.repository.TaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class EditorUiState(
    val isNew: Boolean = true,
    val id: String = "",
    val title: String = "",
    val notes: String = "",
    val listId: String = "",
    val priority: Priority = Priority.MEDIUM,
    val dueAt: Long? = null,
    val reminderAt: Long? = null,
    val subtasks: List<Subtask> = emptyList(),
    val lists: List<TaskList> = emptyList(),
    val saved: Boolean = false,
    val deleted: Boolean = false,
    val alarmArmed: Boolean = false,
    val draftSubtask: String = "",
)

@HiltViewModel
class TaskEditorViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val taskRepository: TaskRepository,
    listRepository: ListRepository,
) : ViewModel() {

    private val existingId = savedStateHandle.get<String>("taskId").orEmpty().ifBlank { null }
    private val presetListId = savedStateHandle.get<String>("listId").orEmpty().ifBlank { null }
    private val taskId = existingId ?: UUID.randomUUID().toString()
    private val isNew = existingId == null

    private val form = MutableStateFlow(
        EditorUiState(isNew = isNew, id = taskId, listId = presetListId.orEmpty()),
    )

    val uiState = combine(
        form,
        listRepository.observeLists(),
        taskRepository.observeSubtasks(taskId),
    ) { editor, lists, storedSubtasks ->
        editor.copy(
            id = taskId,
            lists = lists,
            listId = editor.listId.ifBlank { lists.firstOrNull()?.id.orEmpty() },
            subtasks = if (isNew) editor.subtasks else storedSubtasks,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), EditorUiState(isNew = isNew))

    init {
        if (!isNew) {
            viewModelScope.launch {
                val task = taskRepository.getTask(taskId) ?: return@launch
                form.update {
                    it.copy(
                        title = task.title,
                        notes = task.notes,
                        listId = task.listId,
                        priority = task.priority,
                        dueAt = task.dueAt,
                        reminderAt = task.reminderAt,
                    )
                }
            }
        }
    }

    fun onTitle(value: String) = form.update { it.copy(title = value) }
    fun onNotes(value: String) = form.update { it.copy(notes = value) }
    fun onList(id: String) = form.update { it.copy(listId = id) }
    fun onPriority(priority: Priority) = form.update { it.copy(priority = priority) }
    fun onDue(value: Long?) = form.update { it.copy(dueAt = value, reminderAt = value) }
    fun onReminder(value: Long?) = form.update { it.copy(reminderAt = value) }
    fun onDraftSubtask(value: String) = form.update { it.copy(draftSubtask = value) }

    fun addSubtask() {
        val title = form.value.draftSubtask.trim()
        if (title.isEmpty()) return
        viewModelScope.launch {
            val subtask = Subtask(
                id = UUID.randomUUID().toString(),
                taskId = taskId,
                title = title,
                isDone = false,
                position = form.value.subtasks.size,
                updatedAt = System.currentTimeMillis(),
            )
            if (isNew) {
                form.update { it.copy(subtasks = it.subtasks + subtask, draftSubtask = "") }
            } else {
                taskRepository.upsertSubtask(subtask)
                form.update { it.copy(draftSubtask = "") }
            }
        }
    }

    fun toggleSubtask(subtask: Subtask) {
        viewModelScope.launch {
            if (isNew) {
                form.update { state ->
                    state.copy(
                        subtasks = state.subtasks.map {
                            if (it.id == subtask.id) it.copy(isDone = !it.isDone) else it
                        },
                    )
                }
            } else {
                taskRepository.setSubtaskDone(subtask.id, !subtask.isDone)
            }
        }
    }

    fun deleteSubtask(id: String) {
        viewModelScope.launch {
            if (isNew) {
                form.update { it.copy(subtasks = it.subtasks.filterNot { subtask -> subtask.id == id }) }
            } else {
                taskRepository.deleteSubtask(id)
            }
        }
    }

    fun save() {
        val state = uiState.value
        val title = state.title.trim()
        if (title.isEmpty() || state.listId.isBlank()) return
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val previous = if (isNew) null else taskRepository.getTask(taskId)
            val reminderAt = state.reminderAt ?: state.dueAt
            taskRepository.upsert(
                Task(
                    id = taskId,
                    listId = state.listId,
                    title = title,
                    notes = state.notes,
                    priority = state.priority,
                    isDone = previous?.isDone ?: false,
                    dueAt = state.dueAt,
                    reminderAt = reminderAt,
                    completedAt = previous?.completedAt,
                    createdAt = previous?.createdAt ?: now,
                    updatedAt = now,
                    remoteId = previous?.remoteId,
                ),
            )
            if (isNew) {
                state.subtasks.forEach { taskRepository.upsertSubtask(it.copy(taskId = taskId)) }
            }
            val armed = reminderAt != null && reminderAt > now
            form.update { it.copy(saved = true, alarmArmed = armed) }
        }
    }

    fun delete() {
        if (isNew) {
            form.update { it.copy(deleted = true) }
            return
        }
        viewModelScope.launch {
            taskRepository.delete(taskId)
            form.update { it.copy(deleted = true) }
        }
    }
}
