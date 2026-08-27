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
import com.kizen.tasks.sync.SyncPort
import com.kizen.tasks.sync.TombstoneStore
import com.kizen.tasks.widget.WidgetRefresher
import dagger.Lazy
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
    private val widgetRefresher: WidgetRefresher,
    private val tombstones: TombstoneStore,
    private val syncPort: Lazy<SyncPort>,
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
        val count = logDao.forDay(id, LocalDate.now().toEpochDay())?.count ?: 0
        return entity.toDomain(count)
    }

    override suspend fun upsert(habit: Habit) {
        tombstones.clearHabit(habit.id)
        habitDao.upsert(habit.toEntity())
        reminderScheduler.syncHabit(habit)
        widgetRefresher.refresh()
    }

    override suspend fun bumpToday(id: String, delta: Int): Habit? {
        if (delta == 0) return getHabit(id)
        val entity = habitDao.get(id) ?: return null
        val goal = entity.timesPerDay.coerceAtLeast(1)
        val today = LocalDate.now().toEpochDay()
        val existing = logDao.forDay(id, today)
        val next = ((existing?.count ?: 0) + delta).coerceIn(0, goal)
        val now = System.currentTimeMillis()
        if (existing == null) {
            if (next > 0) {
                logDao.insert(
                    HabitLogEntity(
                        id = UUID.randomUUID().toString(),
                        habitId = id,
                        dayEpoch = today,
                        count = next,
                        completedAt = now,
                    ),
                )
            }
        } else {
            logDao.upsert(existing.copy(count = next, completedAt = now))
        }
        refreshStreak(id)
        val habit = getHabit(id)
        if (habit != null) reminderScheduler.syncHabit(habit)
        widgetRefresher.refresh()
        val port = syncPort.get()
        if (port.isEnabled) runCatching { port.push() }
        return habit
    }

    override suspend fun delete(id: String) {
        reminderScheduler.cancelHabit(id)
        tombstones.markHabit(id)
        habitDao.delete(id)
        widgetRefresher.refresh()
    }

    override suspend fun pendingReminders(): List<Habit> {
        val today = LocalDate.now().toEpochDay()
        return habitDao.active().map { entity ->
            val count = logDao.forDay(entity.id, today)?.count ?: 0
            entity.toDomain(count)
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
        val days = logDao.completeDaysFor(id).toSet()
        val createdOn = Instant.ofEpochMilli(entity.createdAt).atZone(ZoneId.systemDefault()).toLocalDate()
        val repeat = RepeatDays.fromMask(entity.repeatDaysMask)
        val current = StreakCalculator.current(days, repeat, LocalDate.now(), createdOn)
        val longest = maxOf(entity.longestStreak, StreakCalculator.longest(days, repeat), current)
        habitDao.updateStreaks(id, current, longest, System.currentTimeMillis())
    }
}
