package com.kizen.tasks.domain.model

data class Subtask(
    val id: String,
    val taskId: String,
    val title: String,
    val isDone: Boolean,
    val position: Int,
    val updatedAt: Long,
    val remoteId: String? = null,
)
