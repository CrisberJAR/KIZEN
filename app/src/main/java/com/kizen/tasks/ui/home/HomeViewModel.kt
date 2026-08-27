package com.kizen.tasks.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kizen.tasks.domain.model.DayNudge
import com.kizen.tasks.domain.model.Habit
import com.kizen.tasks.domain.model.Subtask
import com.kizen.tasks.domain.model.Task
import com.kizen.tasks.domain.model.TaskList
import com.kizen.tasks.domain.repository.DayNudgeRepository
import com.kizen.tasks.domain.repository.HabitRepository
import com.kizen.tasks.domain.repository.ListRepository
import com.kizen.tasks.domain.repository.TaskRepository
import com.kizen.tasks.sync.InsightCopy
import com.kizen.tasks.sync.SyncPort
import com.kizen.tasks.widget.WidgetRefresher
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class HomeUiState(
    val lists: List<TaskList> = emptyList(),
    val active: List<Task> = emptyList(),
    val done: List<Task> = emptyList(),
    val habits: List<Habit> = emptyList(),
    val nudges: List<DayNudge> = emptyList(),
    val subtasksByTask: Map<String, List<Subtask>> = emptyMap(),
    val insight: String = "",
    val celebration: String? = null,
    val ready: Boolean = false,
) {
    val doneCount: Int get() =
        done.size + habits.count { it.doneToday } + nudges.count { it.isDone }
    val total: Int get() = active.size + done.size + habits.size + nudges.size
    val progress: Float get() = if (total == 0) 0f else doneCount / total.toFloat()
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val listRepository: ListRepository,
    private val taskRepository: TaskRepository,
    private val habitRepository: HabitRepository,
    private val nudgeRepository: DayNudgeRepository,
    private val syncPort: SyncPort,
    private val widgetRefresher: WidgetRefresher,
) : ViewModel() {

    private val celebration = MutableStateFlow<String?>(null)

    val uiState: StateFlow<HomeUiState> = combine(
        combine(
            listRepository.observeLists(),
            taskRepository.observeToday(),
            habitRepository.observeToday(),
            nudgeRepository.observeToday(),
            celebration,
        ) { lists, tasks, habits, nudges, cheer ->
            HomePartial(lists, tasks, habits, nudges, cheer)
        },
        taskRepository.observeAllSubtasks(),
    ) { partial, subtasks ->
        val openTasks = partial.tasks.count { !it.isDone }
        val habitsDone = partial.habits.count { it.doneToday }
        val bestStreak = partial.habits.maxOfOrNull { maxOf(it.currentStreak, it.longestStreak) } ?: 0
        HomeUiState(
            lists = partial.lists,
            active = partial.tasks.filter { !it.isDone },
            done = partial.tasks.filter { it.isDone },
            habits = partial.habits,
            nudges = partial.nudges,
            subtasksByTask = subtasks.groupBy { it.taskId },
            insight = InsightCopy.spanish(habitsDone, partial.habits.size, bestStreak, openTasks),
            celebration = partial.cheer,
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
            if (syncPort.isEnabled) withContext(Dispatchers.IO) {
                syncPort.sync()
            }
            widgetRefresher.refresh()
        }
    }

    fun toggleDone(task: Task) {
        viewModelScope.launch {
            val completing = !task.isDone
            taskRepository.setDone(task.id, completing)
            celebration.value = if (completing) "Completaste “${task.title}”" else null
        }
    }

    fun bumpHabit(habit: Habit, delta: Int) {
        viewModelScope.launch {
            val after = habitRepository.bumpToday(habit.id, delta) ?: return@launch
            celebration.value = when {
                delta < 0 -> null
                after.doneToday && after.currentStreak >= 2 ->
                    "Racha de ${after.currentStreak} días con “${after.title}”"
                after.doneToday -> "Hoy sí: “${after.title}”"
                after.goal > 1 -> "${after.doneCount} de ${after.goal} en “${after.title}”"
                else -> null
            }
        }
    }

    fun toggleNudge(nudge: DayNudge) {
        viewModelScope.launch {
            val completing = !nudge.isDone
            nudgeRepository.setDone(nudge.id, completing)
            celebration.value = if (completing) "Listo: “${nudge.title}”" else null
        }
    }

    fun toggleSubtask(subtask: Subtask) {
        viewModelScope.launch {
            taskRepository.setSubtaskDone(subtask.id, !subtask.isDone)
        }
    }

    fun clearCelebration() {
        celebration.value = null
    }
}

private data class HomePartial(
    val lists: List<TaskList>,
    val tasks: List<Task>,
    val habits: List<Habit>,
    val nudges: List<DayNudge>,
    val cheer: String?,
)
