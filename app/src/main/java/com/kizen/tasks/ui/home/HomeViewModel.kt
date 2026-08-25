package com.kizen.tasks.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kizen.tasks.domain.model.Habit
import com.kizen.tasks.domain.model.Task
import com.kizen.tasks.domain.model.TaskList
import com.kizen.tasks.domain.repository.HabitRepository
import com.kizen.tasks.domain.repository.ListRepository
import com.kizen.tasks.domain.repository.TaskRepository
import com.kizen.tasks.sync.InsightCopy
import com.kizen.tasks.sync.SyncPort
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val lists: List<TaskList> = emptyList(),
    val active: List<Task> = emptyList(),
    val done: List<Task> = emptyList(),
    val habits: List<Habit> = emptyList(),
    val insight: String = "",
    val celebration: String? = null,
    val ready: Boolean = false,
) {
    val doneCount: Int get() = done.size + habits.count { it.doneToday }
    val total: Int get() = active.size + done.size + habits.size
    val progress: Float get() = if (total == 0) 0f else doneCount / total.toFloat()
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val listRepository: ListRepository,
    private val taskRepository: TaskRepository,
    private val habitRepository: HabitRepository,
    private val syncPort: SyncPort,
) : ViewModel() {

    private val celebration = MutableStateFlow<String?>(null)

    val uiState: StateFlow<HomeUiState> = combine(
        listRepository.observeLists(),
        taskRepository.observeToday(),
        habitRepository.observeToday(),
        celebration,
    ) { lists, tasks, habits, cheer ->
        val openTasks = tasks.count { !it.isDone }
        val habitsDone = habits.count { it.doneToday }
        val bestStreak = habits.maxOfOrNull { maxOf(it.currentStreak, it.longestStreak) } ?: 0
        HomeUiState(
            lists = lists,
            active = tasks.filter { !it.isDone },
            done = tasks.filter { it.isDone },
            habits = habits,
            insight = InsightCopy.spanish(habitsDone, habits.size, bestStreak, openTasks),
            celebration = cheer,
            ready = true,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    init {
        viewModelScope.launch {
            listRepository.ensureSeeded()
            habitRepository.ensureSeeded()
            pullCloud()
        }
    }

    fun pullCloud() {
        viewModelScope.launch {
            if (syncPort.isEnabled) syncPort.pull()
        }
    }

    fun toggleDone(task: Task) {
        viewModelScope.launch {
            val completing = !task.isDone
            taskRepository.setDone(task.id, completing)
            celebration.value = if (completing) "Completaste “${task.title}”" else null
        }
    }

    fun toggleHabit(habit: Habit) {
        viewModelScope.launch {
            val after = habitRepository.toggleToday(habit.id) ?: return@launch
            celebration.value = when {
                !after.doneToday -> null
                after.currentStreak >= 2 -> "Racha de ${after.currentStreak} días con “${after.title}”"
                else -> "Hoy sí: “${after.title}”"
            }
        }
    }

    fun clearCelebration() {
        celebration.value = null
    }
}
