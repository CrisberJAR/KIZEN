package com.kizen.tasks.data.local.dao

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Query
import androidx.room.Upsert
import com.kizen.tasks.data.local.entity.HabitEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HabitDao {
    @Query(
        """
        SELECT h.*,
            EXISTS(
                SELECT 1 FROM habit_logs l
                WHERE l.habitId = h.id AND l.dayEpoch = :todayEpoch
            ) AS doneToday
        FROM habits h
        WHERE h.isActive = 1 AND (h.repeatDaysMask & :todayBit) != 0
        ORDER BY doneToday ASC, h.createdAt ASC
        """,
    )
    fun observeToday(todayEpoch: Long, todayBit: Int): Flow<List<HabitWithToday>>

    @Query(
        """
        SELECT h.*,
            EXISTS(
                SELECT 1 FROM habit_logs l
                WHERE l.habitId = h.id AND l.dayEpoch = :todayEpoch
            ) AS doneToday
        FROM habits h
        WHERE h.isActive = 1 AND (h.repeatDaysMask & :todayBit) != 0
        ORDER BY doneToday ASC, h.createdAt ASC
        """,
    )
    suspend fun today(todayEpoch: Long, todayBit: Int): List<HabitWithToday>

    @Query(
        """
        SELECT h.*,
            EXISTS(
                SELECT 1 FROM habit_logs l
                WHERE l.habitId = h.id AND l.dayEpoch = :todayEpoch
            ) AS doneToday
        FROM habits h
        ORDER BY h.createdAt ASC
        """,
    )
    fun observeAll(todayEpoch: Long): Flow<List<HabitWithToday>>

    @Query("SELECT * FROM habits WHERE id = :id")
    suspend fun get(id: String): HabitEntity?

    @Query("SELECT * FROM habits")
    suspend fun all(): List<HabitEntity>

    @Query("SELECT * FROM habits WHERE isActive = 1")
    suspend fun active(): List<HabitEntity>

    @Query("SELECT COUNT(*) FROM habits")
    suspend fun count(): Int

    @Upsert
    suspend fun upsert(habit: HabitEntity)

    @Upsert
    suspend fun upsertAll(habits: List<HabitEntity>)

    @Query(
        """
        UPDATE habits
        SET currentStreak = :currentStreak, longestStreak = :longestStreak, updatedAt = :updatedAt
        WHERE id = :id
        """,
    )
    suspend fun updateStreaks(id: String, currentStreak: Int, longestStreak: Int, updatedAt: Long)

    @Query("DELETE FROM habits WHERE id = :id")
    suspend fun delete(id: String)
}

data class HabitWithToday(
    @Embedded val habit: HabitEntity,
    val doneToday: Boolean,
)
