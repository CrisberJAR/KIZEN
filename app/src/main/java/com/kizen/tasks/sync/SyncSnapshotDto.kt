package com.kizen.tasks.sync

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SyncSnapshotDto(
    val lists: List<TaskListDto> = emptyList(),
    val tasks: List<TaskDto> = emptyList(),
    val habits: List<HabitDto> = emptyList(),
    @SerialName("habit_logs") val habitLogs: List<HabitLogDto> = emptyList(),
    @SerialName("day_nudges") val dayNudges: List<DayNudgeDto> = emptyList(),
    @SerialName("deleted_tasks") val deletedTasks: List<DeletedRefDto> = emptyList(),
    @SerialName("deleted_habits") val deletedHabits: List<DeletedRefDto> = emptyList(),
    @SerialName("deleted_day_nudges") val deletedDayNudges: List<DeletedRefDto> = emptyList(),
    @SerialName("deleted_lists") val deletedLists: List<DeletedRefDto> = emptyList(),
)

@Serializable
data class DeletedRefDto(
    val id: String,
    @SerialName("deleted_at") val deletedAt: Long,
)

@Serializable
data class DayNudgeDto(
    val id: String,
    val title: String,
    val notes: String = "",
    @SerialName("start_at") val startAt: Long,
    @SerialName("interval_minutes") val intervalMinutes: Int = 20,
    @SerialName("is_done") val isDone: Boolean = false,
    @SerialName("day_epoch") val dayEpoch: Long,
    @SerialName("created_at") val createdAt: Long,
    @SerialName("updated_at") val updatedAt: Long,
    val items: List<NudgeItemDto> = emptyList(),
)

@Serializable
data class NudgeItemDto(
    val id: String,
    val title: String,
    @SerialName("is_done") val isDone: Boolean = false,
    val position: Int = 0,
    @SerialName("updated_at") val updatedAt: Long = 0,
)

@Serializable
data class HabitLogDto(
    val id: String,
    @SerialName("habit_id") val habitId: String,
    @SerialName("day_epoch") val dayEpoch: Long,
    val count: Int = 1,
    @SerialName("completed_at") val completedAt: Long,
)

@Serializable
data class AlexaSpeakDto(
    val speak: String,
    val intent: String? = null,
)
