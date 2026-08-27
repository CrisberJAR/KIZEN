package com.kizen.tasks.sync

import android.content.Context
import com.kizen.tasks.data.local.dao.DayNudgeDao
import com.kizen.tasks.data.local.dao.HabitDao
import com.kizen.tasks.data.local.dao.HabitLogDao
import com.kizen.tasks.data.local.dao.NudgeItemDao
import com.kizen.tasks.data.local.dao.SubtaskDao
import com.kizen.tasks.data.local.dao.TaskDao
import com.kizen.tasks.data.local.dao.TaskListDao
import com.kizen.tasks.data.local.entity.DayNudgeEntity
import com.kizen.tasks.data.local.entity.HabitEntity
import com.kizen.tasks.data.local.entity.HabitLogEntity
import com.kizen.tasks.data.local.entity.NudgeItemEntity
import com.kizen.tasks.data.local.entity.SubtaskEntity
import com.kizen.tasks.data.local.entity.TaskEntity
import com.kizen.tasks.data.local.entity.TaskListEntity
import com.kizen.tasks.data.local.toDomain
import com.kizen.tasks.domain.model.KizenDates
import com.kizen.tasks.domain.model.Priority
import com.kizen.tasks.domain.model.RepeatDays
import com.kizen.tasks.domain.repository.HabitRepository
import com.kizen.tasks.notification.KizenNotifier
import com.kizen.tasks.notification.ReminderScheduler
import com.kizen.tasks.widget.WidgetRefresher
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.DayOfWeek
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncLocalStore @Inject constructor(
    @ApplicationContext private val context: Context,
    private val listDao: TaskListDao,
    private val taskDao: TaskDao,
    private val subtaskDao: SubtaskDao,
    private val habitDao: HabitDao,
    private val habitLogDao: HabitLogDao,
    private val dayNudgeDao: DayNudgeDao,
    private val nudgeItemDao: NudgeItemDao,
    private val habitRepository: HabitRepository,
    private val reminderScheduler: ReminderScheduler,
    private val widgetRefresher: WidgetRefresher,
    private val tombstones: TombstoneStore,
) {
    suspend fun export(): SyncSnapshotDto {
        val subtasks = subtaskDao.all().groupBy { it.taskId }
        return SyncSnapshotDto(
            lists = listDao.all().map { it.toDto() },
            tasks = taskDao.allWithList().map { row ->
                row.toDomain().toDto(subtasks[row.id].orEmpty().map { it.toDto() })
            },
            habits = habitDao.all().map { it.toDto() },
            habitLogs = habitLogDao.all().map { it.toDto() },
            dayNudges = dayNudgeDao.all().map { entity ->
                entity.toDto(nudgeItemDao.forNudge(entity.id).map { it.toDto() })
            },
            deletedTasks = tombstones.exportTasks(),
            deletedHabits = tombstones.exportHabits(),
            deletedDayNudges = tombstones.exportNudges(),
            deletedLists = tombstones.exportLists(),
        )
    }

    suspend fun merge(remote: SyncSnapshotDto) {
        tombstones.absorb(remote.deletedTasks, remote.deletedHabits, remote.deletedDayNudges, remote.deletedLists)
        tombstones.listIds().forEach { listDao.delete(it) }
        tombstones.taskIds().forEach { id ->
            reminderScheduler.cancel(id)
            taskDao.delete(id)
        }
        tombstones.habitIds().forEach { id ->
            reminderScheduler.cancelHabit(id)
            habitDao.delete(id)
        }
        tombstones.nudgeIds().forEach { id ->
            reminderScheduler.cancelNudge(id)
            KizenNotifier.cancel(context, "nudge:$id".hashCode())
            dayNudgeDao.delete(id)
        }
        remote.lists.forEach { dto ->
            if (dto.id in tombstones.listIds()) return@forEach
            val local = listDao.get(dto.id)
            if (local == null || dto.updatedAt >= local.updatedAt) {
                listDao.upsert(dto.toEntity(local?.createdAt))
            }
        }
        val fallbackListId = listDao.all().firstOrNull()?.id
        remote.tasks.forEach { dto ->
            if (dto.id in tombstones.taskIds()) return@forEach
            val local = taskDao.getById(dto.id)
            if (local == null || dto.updatedAt >= local.updatedAt) {
                val listId = listDao.get(dto.listId)?.id ?: fallbackListId ?: return@forEach
                taskDao.upsert(dto.toEntity(listId))
                dto.subtasks.forEach { subtaskDao.upsert(it.toEntity(dto.id)) }
            }
        }
        tombstones.taskIds().forEach { id ->
            reminderScheduler.cancel(id)
            taskDao.delete(id)
        }
        remote.habits.forEach { dto ->
            if (dto.id in tombstones.habitIds()) return@forEach
            val local = habitDao.get(dto.id)
            if (local == null || dto.updatedAt >= local.updatedAt) {
                habitDao.upsert(dto.toEntity())
            }
        }
        remote.habitLogs.forEach { dto ->
            val local = habitLogDao.forDay(dto.habitId, dto.dayEpoch)
            if (local == null || dto.completedAt >= local.completedAt) {
                if (local != null && local.id != dto.id) {
                    habitLogDao.deleteDay(dto.habitId, dto.dayEpoch)
                }
                habitLogDao.upsert(dto.toEntity())
            }
        }
        val today = KizenDates.todayEpoch()
        remote.dayNudges.forEach { dto ->
            if (dto.id in tombstones.nudgeIds()) return@forEach
            val local = dayNudgeDao.get(dto.id)
            if (local == null || dto.updatedAt >= local.updatedAt) {
                dayNudgeDao.upsert(dto.toEntity(local?.createdAt, today))
                nudgeItemDao.deleteByNudge(dto.id)
                dto.items.forEach { nudgeItemDao.upsert(it.toEntity(dto.id)) }
            }
        }
        habitRepository.recalculateStreaks()
        habitRepository.pendingReminders().forEach { reminderScheduler.syncHabit(it) }
        taskDao.allWithList().forEach { reminderScheduler.sync(it.toDomain()) }
        dayNudgeDao.pendingBefore(today).forEach { reminderScheduler.cancelNudge(it.id) }
        dayNudgeDao.deleteOlderThan(today)
        dayNudgeDao.forDay(today).forEach { entity ->
            val domain = entity.toDomain(nudgeItemDao.forNudge(entity.id).map { it.toDomain() })
            reminderScheduler.syncNudge(domain)
            if (domain.isDone) {
                KizenNotifier.cancel(context, "nudge:${domain.id}".hashCode())
            }
        }
        tombstones.habitIds().forEach { id ->
            reminderScheduler.cancelHabit(id)
            habitDao.delete(id)
        }
        tombstones.nudgeIds().forEach { id ->
            reminderScheduler.cancelNudge(id)
            KizenNotifier.cancel(context, "nudge:$id".hashCode())
            dayNudgeDao.delete(id)
        }
        tombstones.taskIds().forEach { id ->
            reminderScheduler.cancel(id)
            taskDao.delete(id)
        }
        widgetRefresher.refresh()
    }
}

private fun TaskListEntity.toDto() = TaskListDto(
    id = id,
    name = name,
    colorHex = colorHex,
    emoji = emoji,
    updatedAt = updatedAt,
)

private fun TaskListDto.toEntity(createdAt: Long?) = TaskListEntity(
    id = id,
    name = name,
    colorHex = colorHex,
    emoji = emoji,
    createdAt = createdAt ?: updatedAt,
    updatedAt = updatedAt,
    remoteId = id,
)

private fun com.kizen.tasks.domain.model.Task.toDto(subtasks: List<SubtaskDto>) = TaskDto(
    id = id,
    listId = listId,
    title = title,
    notes = notes,
    priority = priority.name,
    isDone = isDone,
    dueAt = dueAt,
    reminderAt = reminderAt,
    completedAt = completedAt,
    createdAt = createdAt,
    updatedAt = updatedAt,
    source = TaskSource.ANDROID,
    subtasks = subtasks,
)

private fun TaskDto.toEntity(listId: String) = TaskEntity(
    id = id,
    listId = listId,
    title = title,
    notes = notes,
    priority = Priority.from(priority).name,
    isDone = isDone,
    dueAt = dueAt,
    reminderAt = reminderAt,
    completedAt = completedAt,
    createdAt = createdAt,
    updatedAt = updatedAt,
    remoteId = id,
)

private fun SubtaskEntity.toDto() = SubtaskDto(
    id = id,
    title = title,
    isDone = isDone,
    position = position,
)

private fun SubtaskDto.toEntity(taskId: String) = SubtaskEntity(
    id = id,
    taskId = taskId,
    title = title,
    isDone = isDone,
    position = position,
    updatedAt = System.currentTimeMillis(),
    remoteId = id,
)

private fun HabitEntity.toDto() = HabitDto(
    id = id,
    title = title,
    notes = notes,
    emoji = emoji,
    colorHex = colorHex,
    repeatDays = RepeatDays.fromMask(repeatDaysMask).map { it.name },
    reminderMinutes = reminderMinutes,
    timesPerDay = timesPerDay,
    isActive = isActive,
    currentStreak = currentStreak,
    longestStreak = longestStreak,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

private fun HabitDto.toEntity() = HabitEntity(
    id = id,
    title = title,
    notes = notes,
    emoji = emoji,
    colorHex = colorHex,
    repeatDaysMask = RepeatDays.toMask(
        repeatDays.mapNotNull { raw -> runCatching { DayOfWeek.valueOf(raw) }.getOrNull() }.toSet(),
    ),
    reminderMinutes = reminderMinutes,
    timesPerDay = timesPerDay.coerceAtLeast(1),
    isActive = isActive,
    currentStreak = currentStreak,
    longestStreak = longestStreak,
    createdAt = createdAt,
    updatedAt = updatedAt,
    remoteId = id,
)

private fun HabitLogEntity.toDto() = HabitLogDto(
    id = id,
    habitId = habitId,
    dayEpoch = dayEpoch,
    count = count,
    completedAt = completedAt,
)

private fun HabitLogDto.toEntity() = HabitLogEntity(
    id = id,
    habitId = habitId,
    dayEpoch = dayEpoch,
    count = count.coerceAtLeast(0),
    completedAt = completedAt,
)

private fun DayNudgeEntity.toDto(items: List<NudgeItemDto>) = DayNudgeDto(
    id = id,
    title = title,
    notes = notes,
    startAt = startAt,
    intervalMinutes = intervalMinutes,
    isDone = isDone,
    dayEpoch = dayEpoch,
    createdAt = createdAt,
    updatedAt = updatedAt,
    items = items,
)

private fun DayNudgeDto.toEntity(createdAt: Long?, todayEpoch: Long) = DayNudgeEntity(
    id = id,
    title = title,
    notes = notes,
    startAt = startAt,
    intervalMinutes = intervalMinutes.coerceAtLeast(5),
    isDone = isDone,
    dayEpoch = todayEpoch,
    createdAt = createdAt ?: this.createdAt,
    updatedAt = updatedAt,
)

private fun NudgeItemEntity.toDto() = NudgeItemDto(
    id = id,
    title = title,
    isDone = isDone,
    position = position,
)

private fun NudgeItemDto.toEntity(nudgeId: String) = NudgeItemEntity(
    id = id,
    nudgeId = nudgeId,
    title = title,
    isDone = isDone,
    position = position,
    updatedAt = System.currentTimeMillis(),
)
