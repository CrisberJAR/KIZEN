package com.kizen.tasks.domain.model

data class Task(
    val id: String,
    val listId: String,
    val title: String,
    val notes: String,
    val priority: Priority,
    val isDone: Boolean,
    val dueAt: Long?,
    val reminderAt: Long?,
    val completedAt: Long?,
    val createdAt: Long,
    val updatedAt: Long,
    val remoteId: String? = null,
    val subtaskTotal: Int = 0,
    val subtaskDone: Int = 0,
    val listName: String = "",
    val listEmoji: String = "",
    val listColorHex: String = "#C7CEEA",
)
