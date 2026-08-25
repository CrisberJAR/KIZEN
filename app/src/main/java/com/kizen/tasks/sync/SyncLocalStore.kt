package com.kizen.tasks.sync

import com.kizen.tasks.data.local.dao.HabitDao
import com.kizen.tasks.data.local.dao.HabitLogDao
import com.kizen.tasks.data.local.dao.SubtaskDao
import com.kizen.tasks.data.local.dao.TaskDao
import com.kizen.tasks.data.local.dao.TaskListDao
import com.kizen.tasks.data.local.entity.HabitEntity
import com.kizen.tasks.data.local.entity.HabitLogEntity
import com.kizen.tasks.data.local.entity.SubtaskEntity
import com.kizen.tasks.data.local.entity.TaskEntity
import com.kizen.tasks.data.local.entity.TaskListEntity
import com.kizen.tasks.data.local.toDomain
import com.kizen.tasks.domain.model.Priority
import com.kizen.tasks.domain.model.RepeatDays
import com.kizen.tasks.domain.repository.HabitRepository
import com.kizen.tasks.notification.ReminderScheduler
import java.time.DayOfWeek
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncLocalStore @Inject constructor(
    private val listDao: TaskListDao,
    private val taskDao: TaskDao,
    private val subtaskDao: SubtaskDao,
    private val habitDao: HabitDao,
    private val habitLogDao: HabitLogDao,
    private val habitRepository: HabitRepository,
    private val reminderScheduler: ReminderScheduler,
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
        )
    }

    suspend fun merge(remote: SyncSnapshotDto) {
        remote.lists.forEach { dto ->
            val local = listDao.get(dto.id)
            if (local == null || dto.updatedAt >= local.updatedAt) {
                listDao.upsert(dto.toEntity(local?.createdAt))
            }
        }
        val fallbackListId = listDao.all().firstOrNull()?.id
        remote.tasks.forEach { dto ->
            val local = taskDao.getById(dto.id)
            if (local == null || dto.updatedAt >= local.updatedAt) {
                val listId = listDao.get(dto.listId)?.id ?: fallbackListId ?: return@forEach
                taskDao.upsert(dto.toEntity(listId))
                dto.subtasks.forEach { subtaskDao.upsert(it.toEntity(dto.id)) }
            }
        }
        remote.habits.forEach { dto ->
            val local = habitDao.get(dto.id)
            if (local == null || dto.updatedAt >= local.updatedAt) {
                habitDao.upsert(dto.toEntity())
            }
        }
        remote.habitLogs.forEach { habitLogDao.upsert(it.toEntity()) }
        habitRepository.recalculateStreaks()
        taskDao.pendingReminders(System.currentTimeMillis()).forEach {
            reminderScheduler.sync(it.toDomain())
        }
        habitRepository.pendingReminders().forEach { reminderScheduler.syncHabit(it) }
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
    completedAt = completedAt,
)

private fun HabitLogDto.toEntity() = HabitLogEntity(
    id = id,
    habitId = habitId,
    dayEpoch = dayEpoch,
    completedAt = completedAt,
)
