package com.kizen.tasks.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "task_lists", indices = [Index("updatedAt")])
data class TaskListEntity(
    @PrimaryKey val id: String,
    val name: String,
    val colorHex: String,
    val emoji: String,
    val createdAt: Long,
    val updatedAt: Long,
    val remoteId: String?,
)
