package com.kizen.tasks.notification

import com.kizen.tasks.domain.model.DayNudge
import com.kizen.tasks.domain.model.Habit
import com.kizen.tasks.domain.model.Task

interface ReminderScheduler {
    fun sync(task: Task)
    fun cancel(taskId: String)
    fun syncHabit(habit: Habit)
    fun cancelHabit(habitId: String)
    fun syncNudge(nudge: DayNudge)
    fun scheduleNudgeAt(nudge: DayNudge, atMillis: Long)
    fun cancelNudge(nudgeId: String)
}
