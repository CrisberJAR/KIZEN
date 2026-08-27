package com.kizen.tasks.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.kizen.tasks.data.local.entity.NudgeItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NudgeItemDao {
    @Query("SELECT * FROM nudge_items WHERE nudgeId = :nudgeId ORDER BY position ASC, updatedAt ASC")
    fun observeByNudge(nudgeId: String): Flow<List<NudgeItemEntity>>

    @Query("SELECT * FROM nudge_items WHERE nudgeId = :nudgeId ORDER BY position ASC")
    suspend fun forNudge(nudgeId: String): List<NudgeItemEntity>

    @Query("SELECT * FROM nudge_items")
    fun observeAll(): Flow<List<NudgeItemEntity>>

    @Query("SELECT * FROM nudge_items")
    suspend fun all(): List<NudgeItemEntity>

    @Upsert
    suspend fun upsert(item: NudgeItemEntity)

    @Query("UPDATE nudge_items SET isDone = :done, updatedAt = :updatedAt WHERE id = :id")
    suspend fun setDone(id: String, done: Boolean, updatedAt: Long)

    @Query("DELETE FROM nudge_items WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM nudge_items WHERE nudgeId = :nudgeId")
    suspend fun deleteByNudge(nudgeId: String)
}
