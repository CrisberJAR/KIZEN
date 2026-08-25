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
    val isActive: Boolean,
    val currentStreak: Int,
    val longestStreak: Int,
    val createdAt: Long,
    val updatedAt: Long,
    val remoteId: String? = null,
    val doneToday: Boolean = false,
)
