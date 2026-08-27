package com.kizen.tasks.domain.model

import java.time.DayOfWeek

data class Habit(
    val id: String,
    val title: String,
    val notes: String,
    val emoji: String,
    val colorHex: String,
    val repeatDays: Set<DayOfWeek>,
    val reminderMinutes: Int?,
    val timesPerDay: Int = 1,
    val isActive: Boolean,
    val currentStreak: Int,
    val longestStreak: Int,
    val createdAt: Long,
    val updatedAt: Long,
    val remoteId: String? = null,
    val doneCount: Int = 0,
) {
    val goal: Int get() = timesPerDay.coerceAtLeast(1)
    val remainingToday: Int get() = (goal - doneCount).coerceAtLeast(0)
    val doneToday: Boolean get() = doneCount >= goal
}
