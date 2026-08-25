package com.kizen.tasks.sync

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Contrato estable para la Fase 2 (API + Alexa).
 * Alexa Skills Kit → Lambda → este DTO → Room.
 */
@Serializable
data class TaskDto(
    val id: String,
    @SerialName("list_id") val listId: String,
    val title: String,
    val notes: String = "",
    val priority: String = "MEDIUM",
    @SerialName("is_done") val isDone: Boolean = false,
    @SerialName("due_at") val dueAt: Long? = null,
    @SerialName("reminder_at") val reminderAt: Long? = null,
    @SerialName("completed_at") val completedAt: Long? = null,
    @SerialName("created_at") val createdAt: Long,
    @SerialName("updated_at") val updatedAt: Long,
    val source: TaskSource = TaskSource.ANDROID,
    val subtasks: List<SubtaskDto> = emptyList(),
)

@Serializable
data class SubtaskDto(
    val id: String,
    val title: String,
    @SerialName("is_done") val isDone: Boolean = false,
    val position: Int = 0,
)

@Serializable
data class TaskListDto(
    val id: String,
    val name: String,
    @SerialName("color_hex") val colorHex: String,
    val emoji: String,
    @SerialName("updated_at") val updatedAt: Long,
)

@Serializable
enum class TaskSource {
    ANDROID,
    ALEXA,
    WEB,
}

@Serializable
data class AlexaTaskEvent(
    @SerialName("user_id") val userId: String,
    val intent: AlexaIntent,
    val utterance: String? = null,
    val task: TaskDto? = null,
    @SerialName("task_id") val taskId: String? = null,
    @SerialName("occurred_at") val occurredAt: Long,
)

@Serializable
enum class AlexaIntent {
    ADD_TASK,
    COMPLETE_TASK,
    LIST_TASKS,
    ADD_TO_LIST,
    ADD_HABIT,
    COMPLETE_HABIT,
    LIST_HABITS,
    STREAK,
    INSIGHTS,
}
