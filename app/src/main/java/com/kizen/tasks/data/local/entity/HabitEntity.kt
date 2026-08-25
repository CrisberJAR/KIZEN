package com.kizen.tasks.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "habits",
    indices = [Index("isActive"), Index("updatedAt")],
)
data class HabitEntity(
    @PrimaryKey val id: String,
    val title: String,
    val notes: String,
    val emoji: String,
    val colorHex: String,
    val repeatDaysMask: Int,
    val reminderMinutes: Int?,
    val isActive: Boolean,
    val currentStreak: Int,
    val longestStreak: Int,
    val createdAt: Long,
    val updatedAt: Long,
    val remoteId: String?,
)
