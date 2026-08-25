package com.kizen.tasks.data.local

import com.kizen.tasks.data.local.entity.HabitEntity
import com.kizen.tasks.domain.model.RepeatDays

object DefaultHabits {
    fun seed(now: Long = System.currentTimeMillis()): List<HabitEntity> = listOf(
        HabitEntity(
            id = "habit-agua",
            title = "Un vaso de agua",
            notes = "Sin prisa. Un sorbo consciente cuenta.",
            emoji = "💧",
            colorHex = "#A8D8EA",
            repeatDaysMask = RepeatDays.ALL,
            reminderMinutes = 9 * 60,
            isActive = true,
            currentStreak = 0,
            longestStreak = 0,
            createdAt = now,
            updatedAt = now,
            remoteId = null,
        ),
        HabitEntity(
            id = "habit-calma",
            title = "5 minutos de calma",
            notes = "Respira. Nubi se queda contigo.",
            emoji = "☁️",
            colorHex = "#C7CEEA",
            repeatDaysMask = RepeatDays.ALL,
            reminderMinutes = 21 * 60,
            isActive = true,
            currentStreak = 0,
            longestStreak = 0,
            createdAt = now,
            updatedAt = now,
            remoteId = null,
        ),
        HabitEntity(
            id = "habit-mover",
            title = "Mover el cuerpo",
            notes = "Una caminata corta o estirarte un poco.",
            emoji = "🌱",
            colorHex = "#B5EAD7",
            repeatDaysMask = RepeatDays.WEEKDAYS,
            reminderMinutes = 18 * 60,
            isActive = true,
            currentStreak = 0,
            longestStreak = 0,
            createdAt = now,
            updatedAt = now,
            remoteId = null,
        ),
    )
}
