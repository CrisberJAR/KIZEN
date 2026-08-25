package com.kizen.tasks.data.local

import com.kizen.tasks.data.local.dao.HabitWithToday
import com.kizen.tasks.data.local.dao.TaskListWithCounts
import com.kizen.tasks.data.local.dao.TaskWithList
import com.kizen.tasks.data.local.entity.HabitEntity
import com.kizen.tasks.data.local.entity.SubtaskEntity
import com.kizen.tasks.data.local.entity.TaskEntity
import com.kizen.tasks.data.local.entity.TaskListEntity
import com.kizen.tasks.domain.model.Habit
import com.kizen.tasks.domain.model.Priority
import com.kizen.tasks.domain.model.RepeatDays
import com.kizen.tasks.domain.model.Subtask
import com.kizen.tasks.domain.model.Task
import com.kizen.tasks.domain.model.TaskList

fun TaskListEntity.toDomain() = TaskList(
    id = id,
    name = name,
    colorHex = colorHex,
    emoji = emoji,
    createdAt = createdAt,
    updatedAt = updatedAt,
    remoteId = remoteId,
)

fun TaskListWithCounts.toDomain() = TaskList(
    id = id,
    name = name,
    colorHex = colorHex,
    emoji = emoji,
    createdAt = createdAt,
    updatedAt = updatedAt,
    remoteId = remoteId,
    taskCount = taskCount,
    doneCount = doneCount,
)

fun TaskList.toEntity() = TaskListEntity(
    id = id,
    name = name,
    colorHex = colorHex,
    emoji = emoji,
    createdAt = createdAt,
    updatedAt = updatedAt,
    remoteId = remoteId,
)

fun TaskWithList.toDomain() = Task(
    id = id,
    listId = listId,
    title = title,
    notes = notes,
    priority = Priority.from(priority),
    isDone = isDone,
    dueAt = dueAt,
    reminderAt = reminderAt,
    completedAt = completedAt,
    createdAt = createdAt,
    updatedAt = updatedAt,
    remoteId = remoteId,
    subtaskTotal = subtaskTotal,
    subtaskDone = subtaskDone,
    listName = listName,
    listEmoji = listEmoji,
    listColorHex = listColorHex,
)

fun Task.toEntity() = TaskEntity(
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
    remoteId = remoteId,
)

fun SubtaskEntity.toDomain() = Subtask(
    id = id,
    taskId = taskId,
    title = title,
    isDone = isDone,
    position = position,
    updatedAt = updatedAt,
    remoteId = remoteId,
)

fun Subtask.toEntity() = SubtaskEntity(
    id = id,
    taskId = taskId,
    title = title,
    isDone = isDone,
    position = position,
    updatedAt = updatedAt,
    remoteId = remoteId,
)

fun HabitEntity.toDomain(doneToday: Boolean = false) = Habit(
    id = id,
    title = title,
    notes = notes,
    emoji = emoji,
    colorHex = colorHex,
    repeatDays = RepeatDays.fromMask(repeatDaysMask),
    reminderMinutes = reminderMinutes,
    isActive = isActive,
    currentStreak = currentStreak,
    longestStreak = longestStreak,
    createdAt = createdAt,
    updatedAt = updatedAt,
    remoteId = remoteId,
    doneToday = doneToday,
)

fun HabitWithToday.toDomain() = habit.toDomain(doneToday)

fun Habit.toEntity() = HabitEntity(
    id = id,
    title = title,
    notes = notes,
    emoji = emoji,
    colorHex = colorHex,
    repeatDaysMask = RepeatDays.toMask(repeatDays),
    reminderMinutes = reminderMinutes,
    isActive = isActive,
    currentStreak = currentStreak,
    longestStreak = longestStreak,
    createdAt = createdAt,
    updatedAt = updatedAt,
    remoteId = remoteId,
)
