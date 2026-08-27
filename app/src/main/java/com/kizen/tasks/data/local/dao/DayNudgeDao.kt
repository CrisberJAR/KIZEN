package com.kizen.tasks.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.kizen.tasks.data.local.entity.DayNudgeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DayNudgeDao {
    @Query("SELECT * FROM day_nudges WHERE dayEpoch = :dayEpoch ORDER BY isDone ASC, startAt ASC")
    fun observeDay(dayEpoch: Long): Flow<List<DayNudgeEntity>>

    @Query("SELECT * FROM day_nudges WHERE dayEpoch = :dayEpoch ORDER BY isDone ASC, startAt ASC")
    suspend fun forDay(dayEpoch: Long): List<DayNudgeEntity>

    @Query("SELECT * FROM day_nudges WHERE id = :id")
    suspend fun get(id: String): DayNudgeEntity?

    @Query("SELECT * FROM day_nudges WHERE isDone = 0 AND dayEpoch = :dayEpoch")
    suspend fun pendingToday(dayEpoch: Long): List<DayNudgeEntity>

    @Query("SELECT * FROM day_nudges WHERE isDone = 0 AND dayEpoch < :dayEpoch")
    suspend fun pendingBefore(dayEpoch: Long): List<DayNudgeEntity>

    @Query("SELECT * FROM day_nudges")
    suspend fun all(): List<DayNudgeEntity>

    @Upsert
    suspend fun upsert(nudge: DayNudgeEntity)

    @Query("UPDATE day_nudges SET isDone = :done, updatedAt = :updatedAt WHERE id = :id")
    suspend fun setDone(id: String, done: Boolean, updatedAt: Long)

    @Query("DELETE FROM day_nudges WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM day_nudges WHERE dayEpoch < :beforeEpoch")
    suspend fun deleteOlderThan(beforeEpoch: Long)
}
