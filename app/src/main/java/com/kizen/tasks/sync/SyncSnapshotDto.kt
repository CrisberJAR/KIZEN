package com.kizen.tasks.sync

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SyncSnapshotDto(
    val lists: List<TaskListDto> = emptyList(),
    val tasks: List<TaskDto> = emptyList(),
    val habits: List<HabitDto> = emptyList(),
    @SerialName("habit_logs") val habitLogs: List<HabitLogDto> = emptyList(),
)

@Serializable
data class HabitLogDto(
    val id: String,
    @SerialName("habit_id") val habitId: String,
    @SerialName("day_epoch") val dayEpoch: Long,
    @SerialName("completed_at") val completedAt: Long,
)

@Serializable
data class AlexaSpeakDto(
    val speak: String,
    val intent: String? = null,
)
