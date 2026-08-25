package com.kizen.tasks.domain.repository

import com.kizen.tasks.domain.model.TaskList
import kotlinx.coroutines.flow.Flow

interface ListRepository {
    fun observeLists(): Flow<List<TaskList>>
    suspend fun getList(id: String): TaskList?
    suspend fun upsert(list: TaskList)
    suspend fun delete(id: String)
    suspend fun ensureSeeded()
}
