package com.kizen.tasks.ui.list

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kizen.tasks.domain.model.Priority
import com.kizen.tasks.domain.model.Task
import com.kizen.tasks.domain.model.TaskList
import com.kizen.tasks.domain.repository.ListRepository
import com.kizen.tasks.domain.repository.TaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class ListUiState(
    val list: TaskList? = null,
    val active: List<Task> = emptyList(),
    val done: List<Task> = emptyList(),
    val celebration: String? = null,
    val draft: String = "",
)

@HiltViewModel
class ListViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    listRepository: ListRepository,
    private val taskRepository: TaskRepository,
) : ViewModel() {

    private val listId: String = checkNotNull(savedStateHandle["listId"])
    private val draft = MutableStateFlow("")
    private val celebration = MutableStateFlow<String?>(null)

    val uiState = combine(
        listRepository.observeLists(),
        taskRepository.observeByList(listId),
        draft,
        celebration,
    ) { lists, tasks, text, cheer ->
        ListUiState(
            list = lists.find { it.id == listId },
            active = tasks.filter { !it.isDone },
            done = tasks.filter { it.isDone },
            celebration = cheer,
            draft = text,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ListUiState())

    fun onDraftChange(value: String) {
        draft.value = value
    }

    fun quickAdd() {
        val title = draft.value.trim()
        if (title.isEmpty()) return
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            taskRepository.upsert(
                Task(
                    id = UUID.randomUUID().toString(),
                    listId = listId,
                    title = title,
                    notes = "",
                    priority = Priority.MEDIUM,
                    isDone = false,
                    dueAt = null,
                    reminderAt = null,
                    completedAt = null,
                    createdAt = now,
                    updatedAt = now,
                ),
            )
            draft.value = ""
        }
    }

    fun toggleDone(task: Task) {
        viewModelScope.launch {
            val completing = !task.isDone
            taskRepository.setDone(task.id, completing)
            celebration.value = if (completing) "¡Una menos! “${task.title}”" else null
        }
    }

    fun clearCelebration() {
        celebration.value = null
    }
}
