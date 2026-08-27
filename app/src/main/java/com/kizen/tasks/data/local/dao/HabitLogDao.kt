package com.kizen.tasks.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import com.kizen.tasks.data.local.entity.HabitLogEntity

@Dao
interface HabitLogDao {
    @Query("SELECT * FROM habit_logs WHERE habitId = :habitId AND dayEpoch = :dayEpoch LIMIT 1")
    suspend fun forDay(habitId: String, dayEpoch: Long): HabitLogEntity?

    @Query(
        """
        SELECT l.dayEpoch FROM habit_logs l
        INNER JOIN habits h ON h.id = l.habitId
        WHERE l.habitId = :habitId AND l.count >= h.timesPerDay
        """,
    )
    suspend fun completeDaysFor(habitId: String): List<Long>

    @Query("SELECT * FROM habit_logs")
    suspend fun all(): List<HabitLogEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(log: HabitLogEntity)

    @Upsert
    suspend fun upsert(log: HabitLogEntity)

    @Query("DELETE FROM habit_logs WHERE habitId = :habitId AND dayEpoch = :dayEpoch")
    suspend fun deleteDay(habitId: String, dayEpoch: Long)
}
