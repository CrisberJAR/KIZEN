package com.kizen.tasks.sync

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Contrato para insights / Alexa.
 *
 * Backend previsto (Fase 3):
 * - GET /api/v3/tasks/insights
 * - POST /api/v3/ai/summary
 *
 * La Skill de Alexa (Lambda) llama a esos endpoints.
 * Las API keys de Gemini u otro modelo viven en variables de entorno de AWS,
 * nunca en el APK.
 */
@Serializable
data class InsightSummaryDto(
    val text: String,
    @SerialName("habits_done_today") val habitsDoneToday: Int,
    @SerialName("habits_total_today") val habitsTotalToday: Int,
    @SerialName("best_streak") val bestStreak: Int,
    @SerialName("open_tasks") val openTasks: Int,
)

@Serializable
data class HabitDto(
    val id: String,
    val title: String,
    val notes: String = "",
    val emoji: String,
    @SerialName("color_hex") val colorHex: String,
    @SerialName("repeat_days") val repeatDays: List<String>,
    @SerialName("reminder_minutes") val reminderMinutes: Int? = null,
    @SerialName("is_active") val isActive: Boolean = true,
    @SerialName("current_streak") val currentStreak: Int = 0,
    @SerialName("longest_streak") val longestStreak: Int = 0,
    @SerialName("created_at") val createdAt: Long,
    @SerialName("updated_at") val updatedAt: Long,
)

interface InsightsPort {
    suspend fun summary(): Result<InsightSummaryDto>
}

object InsightCopy {
    fun spanish(
        habitsDone: Int,
        habitsTotal: Int,
        bestStreak: Int,
        openTasks: Int,
    ): String = when {
        habitsTotal == 0 && openTasks == 0 ->
            "Hoy el día está en blanco. Un hábito pequeño basta para empezar."
        habitsDone == habitsTotal && habitsTotal > 0 && openTasks == 0 ->
            "Todo lo de hoy está listo. Tu mejor racha: $bestStreak días. Respira."
        bestStreak >= 3 ->
            "Racha de $bestStreak días. $habitsDone de $habitsTotal hábitos y $openTasks tareas suaves."
        else ->
            "$habitsDone de $habitsTotal hábitos de hoy. Sin prisa: $openTasks tareas te esperan."
    }
}
