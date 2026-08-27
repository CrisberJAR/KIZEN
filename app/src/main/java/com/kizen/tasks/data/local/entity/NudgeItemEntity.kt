package com.kizen.tasks.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "nudge_items",
    foreignKeys = [
        ForeignKey(
            entity = DayNudgeEntity::class,
            parentColumns = ["id"],
            childColumns = ["nudgeId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("nudgeId")],
)
data class NudgeItemEntity(
    @PrimaryKey val id: String,
    val nudgeId: String,
    val title: String,
    val isDone: Boolean,
    val position: Int,
    val updatedAt: Long,
)
