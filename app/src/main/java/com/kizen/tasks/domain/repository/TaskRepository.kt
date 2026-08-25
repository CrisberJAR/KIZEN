package com.kizen.tasks.domain.repository

import com.kizen.tasks.domain.model.Subtask
import com.kizen.tasks.domain.model.Task
import kotlinx.coroutines.flow.Flow

interface TaskRepository {
    fun observeToday(): Flow<List<Task>>
    fun observeByList(listId: String): Flow<List<Task>>
    fun observeTask(id: String): Flow<Task?>
    fun observeSubtasks(taskId: String): Flow<List<Subtask>>
    suspend fun getTask(id: String): Task?
    suspend fun upsert(task: Task)
    suspend fun setDone(id: String, done: Boolean)
    suspend fun delete(id: String)
    suspend fun upsertSubtask(subtask: Subtask)
    suspend fun setSubtaskDone(id: String, done: Boolean)
    suspend fun deleteSubtask(id: String)
    suspend fun pendingReminders(): List<Task>
    suspend fun countOpen(): Int
    suspend fun pruneCompletedBefore(before: Long)
}
