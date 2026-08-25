package com.kizen.tasks.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.kizen.tasks.data.local.entity.TaskEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Query(
        """
        SELECT t.*, l.name AS listName, l.emoji AS listEmoji, l.colorHex AS listColorHex,
            (SELECT COUNT(*) FROM subtasks s WHERE s.taskId = t.id) AS subtaskTotal,
            (SELECT COUNT(*) FROM subtasks s WHERE s.taskId = t.id AND s.isDone = 1) AS subtaskDone
        FROM tasks t
        INNER JOIN task_lists l ON l.id = t.listId
        WHERE t.isDone = 0
           OR (t.completedAt IS NOT NULL AND t.completedAt BETWEEN :start AND :end)
        ORDER BY t.isDone ASC, t.priority DESC, t.dueAt ASC, t.createdAt DESC
        """
    )
    fun observeToday(start: Long, end: Long): Flow<List<TaskWithList>>

    @Query(
        """
        SELECT t.*, l.name AS listName, l.emoji AS listEmoji, l.colorHex AS listColorHex,
            (SELECT COUNT(*) FROM subtasks s WHERE s.taskId = t.id) AS subtaskTotal,
            (SELECT COUNT(*) FROM subtasks s WHERE s.taskId = t.id AND s.isDone = 1) AS subtaskDone
        FROM tasks t
        INNER JOIN task_lists l ON l.id = t.listId
        WHERE t.listId = :listId
        ORDER BY t.isDone ASC, t.priority DESC, t.dueAt ASC, t.createdAt DESC
        """
    )
    fun observeByList(listId: String): Flow<List<TaskWithList>>

    @Query(
        """
        SELECT t.*, l.name AS listName, l.emoji AS listEmoji, l.colorHex AS listColorHex,
            (SELECT COUNT(*) FROM subtasks s WHERE s.taskId = t.id) AS subtaskTotal,
            (SELECT COUNT(*) FROM subtasks s WHERE s.taskId = t.id AND s.isDone = 1) AS subtaskDone
        FROM tasks t
        INNER JOIN task_lists l ON l.id = t.listId
        WHERE t.id = :id
        """
    )
    fun observeById(id: String): Flow<TaskWithList?>

    @Query(
        """
        SELECT t.*, l.name AS listName, l.emoji AS listEmoji, l.colorHex AS listColorHex,
            (SELECT COUNT(*) FROM subtasks s WHERE s.taskId = t.id) AS subtaskTotal,
            (SELECT COUNT(*) FROM subtasks s WHERE s.taskId = t.id AND s.isDone = 1) AS subtaskDone
        FROM tasks t
        INNER JOIN task_lists l ON l.id = t.listId
        WHERE t.id = :id
        """
    )
    suspend fun getById(id: String): TaskWithList?

    @Upsert
    suspend fun upsert(task: TaskEntity)

    @Query("UPDATE tasks SET isDone = :done, completedAt = :completedAt, updatedAt = :updatedAt WHERE id = :id")
    suspend fun setDone(id: String, done: Boolean, completedAt: Long?, updatedAt: Long)

    @Query("DELETE FROM tasks WHERE id = :id")
    suspend fun delete(id: String)

    @Query(
        """
        SELECT t.*, l.name AS listName, l.emoji AS listEmoji, l.colorHex AS listColorHex,
            0 AS subtaskTotal, 0 AS subtaskDone
        FROM tasks t
        INNER JOIN task_lists l ON l.id = t.listId
        WHERE t.reminderAt IS NOT NULL AND t.reminderAt > :now AND t.isDone = 0
        """
    )
    suspend fun pendingReminders(now: Long): List<TaskWithList>

    @Query("SELECT COUNT(*) FROM tasks WHERE isDone = 0")
    suspend fun countOpen(): Int

    @Query("DELETE FROM tasks WHERE isDone = 1 AND completedAt IS NOT NULL AND completedAt < :before")
    suspend fun deleteCompletedBefore(before: Long)

    @Query(
        """
        SELECT t.*, l.name AS listName, l.emoji AS listEmoji, l.colorHex AS listColorHex,
            (SELECT COUNT(*) FROM subtasks s WHERE s.taskId = t.id) AS subtaskTotal,
            (SELECT COUNT(*) FROM subtasks s WHERE s.taskId = t.id AND s.isDone = 1) AS subtaskDone
        FROM tasks t
        INNER JOIN task_lists l ON l.id = t.listId
        ORDER BY t.updatedAt DESC
        """,
    )
    suspend fun allWithList(): List<TaskWithList>

    @Upsert
    suspend fun upsertAll(tasks: List<TaskEntity>)
}

data class TaskWithList(
    val id: String,
    val listId: String,
    val title: String,
    val notes: String,
    val priority: String,
    val isDone: Boolean,
    val dueAt: Long?,
    val reminderAt: Long?,
    val completedAt: Long?,
    val createdAt: Long,
    val updatedAt: Long,
    val remoteId: String?,
    val listName: String,
    val listEmoji: String,
    val listColorHex: String,
    val subtaskTotal: Int,
    val subtaskDone: Int,
)
