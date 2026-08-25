package com.kizen.tasks.notification

import com.kizen.tasks.domain.model.Habit
import com.kizen.tasks.domain.model.Task

interface ReminderScheduler {
    fun sync(task: Task)
    fun cancel(taskId: String)
    fun syncHabit(habit: Habit)
    fun cancelHabit(habitId: String)
}
