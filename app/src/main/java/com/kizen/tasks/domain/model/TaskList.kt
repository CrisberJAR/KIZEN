package com.kizen.tasks.domain.model

data class TaskList(
    val id: String,
    val name: String,
    val colorHex: String,
    val emoji: String,
    val createdAt: Long,
    val updatedAt: Long,
    val remoteId: String? = null,
    val taskCount: Int = 0,
    val doneCount: Int = 0,
)
