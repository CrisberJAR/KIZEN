package com.kizen.tasks.domain.model

data class DayNudge(
    val id: String,
    val title: String,
    val notes: String,
    val startAt: Long,
    val intervalMinutes: Int,
    val isDone: Boolean,
    val dayEpoch: Long,
    val createdAt: Long,
    val updatedAt: Long,
    val items: List<NudgeItem> = emptyList(),
)

data class NudgeItem(
    val id: String,
    val nudgeId: String,
    val title: String,
    val isDone: Boolean,
    val position: Int,
    val updatedAt: Long,
)
