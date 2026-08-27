package com.kizen.tasks.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.kizen.tasks.data.local.entity.SubtaskEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SubtaskDao {
    @Query("SELECT * FROM subtasks WHERE taskId = :taskId ORDER BY position ASC, updatedAt ASC")
    fun observeByTask(taskId: String): Flow<List<SubtaskEntity>>

    @Query("SELECT * FROM subtasks")
    fun observeAll(): Flow<List<SubtaskEntity>>

    @Query("SELECT * FROM subtasks")
    suspend fun all(): List<SubtaskEntity>

    @Upsert
    suspend fun upsertAll(subtasks: List<SubtaskEntity>)

    @Upsert
    suspend fun upsert(subtask: SubtaskEntity)

    @Query("UPDATE subtasks SET isDone = :done, updatedAt = :updatedAt WHERE id = :id")
    suspend fun setDone(id: String, done: Boolean, updatedAt: Long)

    @Query("DELETE FROM subtasks WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM subtasks WHERE taskId = :taskId")
    suspend fun deleteByTask(taskId: String)

    @Query("SELECT * FROM subtasks WHERE id = :id")
    suspend fun get(id: String): SubtaskEntity?
}
