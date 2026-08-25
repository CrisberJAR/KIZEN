package com.kizen.tasks.sync

import com.kizen.tasks.domain.repository.HabitRepository
import com.kizen.tasks.domain.repository.TaskRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocalInsightsPort @Inject constructor(
    private val habitRepository: HabitRepository,
    private val taskRepository: TaskRepository,
) : InsightsPort {
    override suspend fun summary(): Result<InsightSummaryDto> {
        val habits = habitRepository.todaySnapshot()
        val openTasks = taskRepository.countOpen()
        val done = habits.count { it.doneToday }
        val best = habits.maxOfOrNull { maxOf(it.currentStreak, it.longestStreak) } ?: 0
        return Result.success(
            InsightSummaryDto(
                text = InsightCopy.spanish(done, habits.size, best, openTasks),
                habitsDoneToday = done,
                habitsTotalToday = habits.size,
                bestStreak = best,
                openTasks = openTasks,
            ),
        )
    }
}
