package com.kizen.tasks.domain.repository

import com.kizen.tasks.domain.model.Habit
import kotlinx.coroutines.flow.Flow

interface HabitRepository {
    fun observeToday(): Flow<List<Habit>>
    fun observeAll(): Flow<List<Habit>>
    suspend fun todaySnapshot(): List<Habit>
    suspend fun getHabit(id: String): Habit?
    suspend fun upsert(habit: Habit)
    suspend fun bumpToday(id: String, delta: Int): Habit?
    suspend fun delete(id: String)
    suspend fun pendingReminders(): List<Habit>
    suspend fun recalculateStreaks()
    suspend fun ensureSeeded()
}
