package com.kizen.tasks.ui.habit

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kizen.tasks.domain.model.Habit
import com.kizen.tasks.domain.model.RepeatDays
import com.kizen.tasks.domain.repository.HabitRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.util.UUID
import javax.inject.Inject

data class HabitEditorUiState(
    val isNew: Boolean = true,
    val id: String = "",
    val title: String = "",
    val notes: String = "",
    val emoji: String = "💧",
    val colorHex: String = "#A8D8EA",
    val repeatDays: Set<DayOfWeek> = DayOfWeek.entries.toSet(),
    val reminderMinutes: Int? = 9 * 60,
    val timesPerDay: Int = 1,
    val saved: Boolean = false,
    val deleted: Boolean = false,
)

@HiltViewModel
class HabitEditorViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val habitRepository: HabitRepository,
) : ViewModel() {

    private val existingId = savedStateHandle.get<String>("habitId").orEmpty().ifBlank { null }
    private val habitId = existingId ?: UUID.randomUUID().toString()
    private val isNew = existingId == null

    private val _uiState = MutableStateFlow(HabitEditorUiState(isNew = isNew, id = habitId))
    val uiState: StateFlow<HabitEditorUiState> = _uiState

    init {
        if (!isNew) {
            viewModelScope.launch {
                val habit = habitRepository.getHabit(habitId) ?: return@launch
                _uiState.update {
                    it.copy(
                        title = habit.title,
                        notes = habit.notes,
                        emoji = habit.emoji,
                        colorHex = habit.colorHex,
                        repeatDays = habit.repeatDays.ifEmpty { DayOfWeek.entries.toSet() },
                        reminderMinutes = habit.reminderMinutes,
                        timesPerDay = habit.goal,
                    )
                }
            }
        }
    }

    fun onTitle(value: String) = _uiState.update { it.copy(title = value) }
    fun onNotes(value: String) = _uiState.update { it.copy(notes = value) }
    fun onEmoji(value: String) = _uiState.update { it.copy(emoji = value) }
    fun onColor(value: String) = _uiState.update { it.copy(colorHex = value) }
    fun onReminder(value: Int?) = _uiState.update { it.copy(reminderMinutes = value) }
    fun onTimesPerDay(value: Int) = _uiState.update { it.copy(timesPerDay = value.coerceIn(1, 24)) }

    fun toggleDay(day: DayOfWeek) {
        _uiState.update { state ->
            val next = state.repeatDays.toMutableSet()
            if (!next.add(day)) next.remove(day)
            if (next.isEmpty()) next.add(day)
            state.copy(repeatDays = next)
        }
    }

    fun save() {
        val state = _uiState.value
        val title = state.title.trim()
        if (title.isEmpty()) return
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val previous = if (isNew) null else habitRepository.getHabit(habitId)
            habitRepository.upsert(
                Habit(
                    id = habitId,
                    title = title,
                    notes = state.notes,
                    emoji = state.emoji,
                    colorHex = state.colorHex,
                    repeatDays = state.repeatDays.ifEmpty { RepeatDays.fromMask(RepeatDays.ALL) },
                    reminderMinutes = state.reminderMinutes,
                    timesPerDay = state.timesPerDay.coerceIn(1, 24),
                    isActive = previous?.isActive ?: true,
                    currentStreak = previous?.currentStreak ?: 0,
                    longestStreak = previous?.longestStreak ?: 0,
                    createdAt = previous?.createdAt ?: now,
                    updatedAt = now,
                    remoteId = previous?.remoteId,
                    doneCount = previous?.doneCount ?: 0,
                ),
            )
            _uiState.update { it.copy(saved = true) }
        }
    }

    fun delete() {
        if (isNew) {
            _uiState.update { it.copy(deleted = true) }
            return
        }
        viewModelScope.launch {
            habitRepository.delete(habitId)
            _uiState.update { it.copy(deleted = true) }
        }
    }
}
