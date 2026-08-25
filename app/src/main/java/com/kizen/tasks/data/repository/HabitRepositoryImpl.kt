package com.kizen.tasks.data.repository

import com.kizen.tasks.data.local.DefaultHabits
import com.kizen.tasks.data.local.dao.HabitDao
import com.kizen.tasks.data.local.dao.HabitLogDao
import com.kizen.tasks.data.local.entity.HabitLogEntity
import com.kizen.tasks.data.local.toDomain
import com.kizen.tasks.data.local.toEntity
import com.kizen.tasks.domain.model.Habit
import com.kizen.tasks.domain.model.RepeatDays
import com.kizen.tasks.domain.model.StreakCalculator
import com.kizen.tasks.domain.repository.HabitRepository
import com.kizen.tasks.notification.ReminderScheduler
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HabitRepositoryImpl @Inject constructor(
    private val habitDao: HabitDao,
    private val logDao: HabitLogDao,
    private val reminderScheduler: ReminderScheduler,
) : HabitRepository {

    override fun observeToday(): Flow<List<Habit>> {
        val today = LocalDate.now()
        return habitDao.observeToday(today.toEpochDay(), RepeatDays.bit(today.dayOfWeek))
            .map { rows -> rows.map { it.toDomain() } }
    }

    override fun observeAll(): Flow<List<Habit>> =
        habitDao.observeAll(LocalDate.now().toEpochDay())
            .map { rows -> rows.map { it.toDomain() } }

    override suspend fun todaySnapshot(): List<Habit> {
        val today = LocalDate.now()
        return habitDao.today(today.toEpochDay(), RepeatDays.bit(today.dayOfWeek)).map { it.toDomain() }
    }

    override suspend fun getHabit(id: String): Habit? {
        val entity = habitDao.get(id) ?: return null
        val doneToday = logDao.forDay(id, LocalDate.now().toEpochDay()) != null
        return entity.toDomain(doneToday)
    }

    override suspend fun upsert(habit: Habit) {
        habitDao.upsert(habit.toEntity())
        reminderScheduler.syncHabit(habit)
    }

    override suspend fun toggleToday(id: String): Habit? {
        val today = LocalDate.now().toEpochDay()
        val existing = logDao.forDay(id, today)
        if (existing != null) {
            logDao.deleteDay(id, today)
        } else {
            logDao.insert(
                HabitLogEntity(
                    id = UUID.randomUUID().toString(),
                    habitId = id,
                    dayEpoch = today,
                    completedAt = System.currentTimeMillis(),
                ),
            )
        }
        refreshStreak(id)
        val habit = getHabit(id)
        if (habit != null) reminderScheduler.syncHabit(habit)
        return habit
    }

    override suspend fun delete(id: String) {
        reminderScheduler.cancelHabit(id)
        habitDao.delete(id)
    }

    override suspend fun pendingReminders(): List<Habit> {
        val today = LocalDate.now().toEpochDay()
        return habitDao.active().map { entity ->
            val done = logDao.forDay(entity.id, today) != null
            entity.toDomain(done)
        }.filter { it.reminderMinutes != null }
    }

    override suspend fun recalculateStreaks() {
        habitDao.all().forEach { refreshStreak(it.id) }
    }

    override suspend fun ensureSeeded() {
        if (habitDao.count() == 0) {
            habitDao.upsertAll(DefaultHabits.seed())
        }
    }

    private suspend fun refreshStreak(id: String) {
        val entity = habitDao.get(id) ?: return
        val days = logDao.daysFor(id).toSet()
        val createdOn = Instant.ofEpochMilli(entity.createdAt).atZone(ZoneId.systemDefault()).toLocalDate()
        val repeat = RepeatDays.fromMask(entity.repeatDaysMask)
        val current = StreakCalculator.current(days, repeat, LocalDate.now(), createdOn)
        val longest = maxOf(entity.longestStreak, StreakCalculator.longest(days, repeat), current)
        habitDao.updateStreaks(id, current, longest, System.currentTimeMillis())
    }
}
