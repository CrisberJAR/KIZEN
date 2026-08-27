package com.kizen.tasks.data.repository

import com.kizen.tasks.data.local.DefaultLists
import com.kizen.tasks.data.local.dao.TaskListDao
import com.kizen.tasks.data.local.toDomain
import com.kizen.tasks.data.local.toEntity
import com.kizen.tasks.domain.model.TaskList
import com.kizen.tasks.domain.repository.ListRepository
import com.kizen.tasks.sync.SyncPort
import com.kizen.tasks.sync.TombstoneStore
import dagger.Lazy
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ListRepositoryImpl @Inject constructor(
    private val listDao: TaskListDao,
    private val tombstones: TombstoneStore,
    private val syncPort: Lazy<SyncPort>,
) : ListRepository {

    override fun observeLists(): Flow<List<TaskList>> =
        listDao.observeWithCounts().map { rows -> rows.map { it.toDomain() } }

    override suspend fun getList(id: String): TaskList? =
        listDao.get(id)?.let {
            TaskList(it.id, it.name, it.colorHex, it.emoji, it.createdAt, it.updatedAt, it.remoteId)
        }

    override suspend fun upsert(list: TaskList) {
        tombstones.clearList(list.id)
        listDao.upsert(list.toEntity())
        pushCloud()
    }

    override suspend fun delete(id: String) {
        tombstones.markList(id)
        listDao.delete(id)
        val port = syncPort.get()
        if (port.isEnabled) runCatching { port.sync() }
    }

    override suspend fun ensureSeeded() {
        if (listDao.count() == 0) {
            listDao.upsertAll(DefaultLists.seed())
        }
    }

    private suspend fun pushCloud() {
        val port = syncPort.get()
        if (port.isEnabled) runCatching { port.push() }
    }
}
