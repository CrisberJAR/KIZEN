package com.kizen.tasks.ui.nudge

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kizen.tasks.domain.model.DayNudge
import com.kizen.tasks.domain.model.NudgeItem
import com.kizen.tasks.domain.repository.DayNudgeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.kizen.tasks.domain.model.KizenDates
import java.util.UUID
import javax.inject.Inject

data class NudgeEditorUiState(
    val isNew: Boolean = true,
    val title: String = "",
    val notes: String = "",
    val startAt: Long = defaultStartAt(),
    val intervalMinutes: Int = 20,
    val items: List<NudgeItem> = emptyList(),
    val draftItem: String = "",
    val saved: Boolean = false,
    val deleted: Boolean = false,
)

@HiltViewModel
class NudgeEditorViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val nudgeRepository: DayNudgeRepository,
) : ViewModel() {

    private val existingId = savedStateHandle.get<String>("nudgeId").orEmpty().ifBlank { null }
    private val nudgeId = existingId ?: UUID.randomUUID().toString()
    private val isNew = existingId == null

    private val form = MutableStateFlow(NudgeEditorUiState(isNew = isNew))

    val uiState = combine(
        form,
        nudgeRepository.observeItems(nudgeId),
    ) { editor, stored ->
        editor.copy(items = if (isNew) editor.items else stored)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), NudgeEditorUiState(isNew = isNew))

    init {
        if (!isNew) {
            viewModelScope.launch {
                val nudge = nudgeRepository.get(nudgeId) ?: return@launch
                form.update {
                    it.copy(
                        title = nudge.title,
                        notes = nudge.notes,
                        startAt = nudge.startAt,
                        intervalMinutes = nudge.intervalMinutes,
                    )
                }
            }
        }
    }

    fun onTitle(value: String) = form.update { it.copy(title = value) }
    fun onNotes(value: String) = form.update { it.copy(notes = value) }
    fun onStartAt(value: Long) = form.update { it.copy(startAt = value) }
    fun onInterval(value: Int) = form.update { it.copy(intervalMinutes = value.coerceIn(5, 120)) }
    fun onDraftItem(value: String) = form.update { it.copy(draftItem = value) }

    fun addItem() {
        val title = form.value.draftItem.trim()
        if (title.isEmpty()) return
        viewModelScope.launch {
            val item = NudgeItem(
                id = UUID.randomUUID().toString(),
                nudgeId = nudgeId,
                title = title,
                isDone = false,
                position = form.value.items.size,
                updatedAt = System.currentTimeMillis(),
            )
            if (isNew) {
                form.update { it.copy(items = it.items + item, draftItem = "") }
            } else {
                nudgeRepository.upsertItem(item)
                form.update { it.copy(draftItem = "") }
            }
        }
    }

    fun toggleItem(item: NudgeItem) {
        viewModelScope.launch {
            if (isNew) {
                form.update { state ->
                    state.copy(items = state.items.map { if (it.id == item.id) it.copy(isDone = !it.isDone) else it })
                }
            } else {
                nudgeRepository.setItemDone(item.id, !item.isDone)
            }
        }
    }

    fun deleteItem(id: String) {
        viewModelScope.launch {
            if (isNew) {
                form.update { it.copy(items = it.items.filterNot { item -> item.id == id }) }
            } else {
                nudgeRepository.deleteItem(id)
            }
        }
    }

    fun save() {
        val state = uiState.value
        val title = state.title.trim()
        if (title.isEmpty()) return
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val previous = if (isNew) null else nudgeRepository.get(nudgeId)
            nudgeRepository.upsert(
                DayNudge(
                    id = nudgeId,
                    title = title,
                    notes = state.notes,
                    startAt = state.startAt,
                    intervalMinutes = state.intervalMinutes,
                    isDone = previous?.isDone ?: false,
                    dayEpoch = previous?.dayEpoch ?: KizenDates.todayEpoch(),
                    createdAt = previous?.createdAt ?: now,
                    updatedAt = now,
                    items = state.items,
                ),
            )
            if (isNew) {
                state.items.forEach { nudgeRepository.upsertItem(it.copy(nudgeId = nudgeId)) }
            }
            form.update { it.copy(saved = true) }
        }
    }

    fun delete() {
        if (isNew) {
            form.update { it.copy(deleted = true) }
            return
        }
        viewModelScope.launch {
            nudgeRepository.delete(nudgeId)
            form.update { it.copy(deleted = true) }
        }
    }
}

private fun defaultStartAt(): Long =
    java.time.ZonedDateTime.now().plusMinutes(30).withSecond(0).withNano(0).toInstant().toEpochMilli()

