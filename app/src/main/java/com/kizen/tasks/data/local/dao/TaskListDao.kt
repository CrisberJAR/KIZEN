package com.kizen.tasks.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.kizen.tasks.data.local.entity.TaskListEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskListDao {
    @Query(
        """
        SELECT lists.*,
            (SELECT COUNT(*) FROM tasks WHERE listId = lists.id) AS taskCount,
            (SELECT COUNT(*) FROM tasks WHERE listId = lists.id AND isDone = 1) AS doneCount
        FROM task_lists AS lists
        ORDER BY createdAt ASC
        """
    )
    fun observeWithCounts(): Flow<List<TaskListWithCounts>>

    @Query("SELECT * FROM task_lists WHERE id = :id")
    suspend fun get(id: String): TaskListEntity?

    @Query("SELECT COUNT(*) FROM task_lists")
    suspend fun count(): Int

    @Query("SELECT * FROM task_lists ORDER BY createdAt ASC")
    suspend fun all(): List<TaskListEntity>

    @Upsert
    suspend fun upsert(list: TaskListEntity)

    @Upsert
    suspend fun upsertAll(lists: List<TaskListEntity>)

    @Query("DELETE FROM task_lists WHERE id = :id")
    suspend fun delete(id: String)
}

data class TaskListWithCounts(
    val id: String,
    val name: String,
    val colorHex: String,
    val emoji: String,
    val createdAt: Long,
    val updatedAt: Long,
    val remoteId: String?,
    val taskCount: Int,
    val doneCount: Int,
)
