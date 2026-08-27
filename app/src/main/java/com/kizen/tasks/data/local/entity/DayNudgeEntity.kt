package com.kizen.tasks.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "day_nudges",
    indices = [Index("dayEpoch"), Index("isDone")],
)
data class DayNudgeEntity(
    @PrimaryKey val id: String,
    val title: String,
    val notes: String,
    val startAt: Long,
    val intervalMinutes: Int,
    val isDone: Boolean,
    val dayEpoch: Long,
    val createdAt: Long,
    val updatedAt: Long,
)
