package com.kizen.tasks.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "tasks",
    foreignKeys = [
        ForeignKey(
            entity = TaskListEntity::class,
            parentColumns = ["id"],
            childColumns = ["listId"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [
        Index("listId"),
        Index("isDone"),
        Index("dueAt"),
        Index("reminderAt"),
        Index("updatedAt"),
    ],
)
data class TaskEntity(
    @PrimaryKey val id: String,
    val listId: String,
    val title: String,
    val notes: String,
    val priority: String,
    val isDone: Boolean,
    val dueAt: Long?,
    val reminderAt: Long?,
    val completedAt: Long?,
    val createdAt: Long,
    val updatedAt: Long,
    val remoteId: String?,
)
