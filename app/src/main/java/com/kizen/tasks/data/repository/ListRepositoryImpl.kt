package com.kizen.tasks.data.repository

import com.kizen.tasks.data.local.DefaultLists
import com.kizen.tasks.data.local.dao.TaskListDao
import com.kizen.tasks.data.local.toDomain
import com.kizen.tasks.data.local.toEntity
import com.kizen.tasks.domain.model.TaskList
import com.kizen.tasks.domain.repository.ListRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ListRepositoryImpl @Inject constructor(
    private val listDao: TaskListDao,
) : ListRepository {

    override fun observeLists(): Flow<List<TaskList>> =
        listDao.observeWithCounts().map { rows -> rows.map { it.toDomain() } }

    override suspend fun getList(id: String): TaskList? =
        listDao.get(id)?.let {
            TaskList(it.id, it.name, it.colorHex, it.emoji, it.createdAt, it.updatedAt, it.remoteId)
        }

    override suspend fun upsert(list: TaskList) {
        listDao.upsert(list.toEntity())
    }

    override suspend fun delete(id: String) {
        listDao.delete(id)
    }

    override suspend fun ensureSeeded() {
        if (listDao.count() == 0) {
            listDao.upsertAll(DefaultLists.seed())
        }
    }
}
