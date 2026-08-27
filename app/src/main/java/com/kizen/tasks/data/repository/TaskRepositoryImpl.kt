package com.kizen.tasks.data.repository

import com.kizen.tasks.data.local.dao.SubtaskDao
import com.kizen.tasks.data.local.dao.TaskDao
import com.kizen.tasks.data.local.toDomain
import com.kizen.tasks.data.local.toEntity
import com.kizen.tasks.domain.model.Subtask
import com.kizen.tasks.domain.model.Task
import com.kizen.tasks.domain.model.KizenDates
import com.kizen.tasks.domain.repository.TaskRepository
import com.kizen.tasks.notification.ReminderScheduler
import com.kizen.tasks.sync.SyncPort
import com.kizen.tasks.sync.TombstoneStore
import com.kizen.tasks.widget.WidgetRefresher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TaskRepositoryImpl @Inject constructor(
    private val taskDao: TaskDao,
    private val subtaskDao: SubtaskDao,
    private val reminderScheduler: ReminderScheduler,
    private val widgetRefresher: WidgetRefresher,
    private val tombstones: TombstoneStore,
    private val syncPort: SyncPort,
) : TaskRepository {

    override fun observeToday(): Flow<List<Task>> {
        val start = KizenDates.dayStartMillis()
        val end = KizenDates.dayEndMillis()
        return taskDao.observeToday(start, end)
            .map { rows -> rows.map { it.toDomain() } }
            .distinctUntilChanged()
    }

    override fun observeByList(listId: String): Flow<List<Task>> =
        taskDao.observeByList(listId)
            .map { rows -> rows.map { it.toDomain() } }
            .distinctUntilChanged()

    override fun observeTask(id: String): Flow<Task?> =
        taskDao.observeById(id).map { it?.toDomain() }.distinctUntilChanged()

    override fun observeSubtasks(taskId: String): Flow<List<Subtask>> =
        subtaskDao.observeByTask(taskId).map { rows -> rows.map { it.toDomain() } }

    override fun observeAllSubtasks(): Flow<List<Subtask>> =
        subtaskDao.observeAll().map { rows -> rows.map { it.toDomain() } }

    override suspend fun getTask(id: String): Task? = taskDao.getById(id)?.toDomain()

    override suspend fun upsert(task: Task) {
        tombstones.clearTask(task.id)
        taskDao.upsert(task.toEntity())
        reminderScheduler.sync(task)
        widgetRefresher.refresh()
        pushCloud()
    }

    override suspend fun setDone(id: String, done: Boolean) {
        val now = System.currentTimeMillis()
        taskDao.setDone(id, done, if (done) now else null, now)
        val task = taskDao.getById(id)?.toDomain()
        if (task != null) reminderScheduler.sync(task)
        widgetRefresher.refresh()
        pushCloud()
    }

    override suspend fun delete(id: String) {
        reminderScheduler.cancel(id)
        tombstones.markTask(id)
        taskDao.delete(id)
        widgetRefresher.refresh()
        if (syncPort.isEnabled) runCatching { syncPort.sync() }
    }

    override suspend fun upsertSubtask(subtask: Subtask) {
        subtaskDao.upsert(subtask.toEntity())
        taskDao.touch(subtask.taskId, System.currentTimeMillis())
        widgetRefresher.refresh()
        pushCloud()
    }

    override suspend fun setSubtaskDone(id: String, done: Boolean) {
        val now = System.currentTimeMillis()
        val item = subtaskDao.get(id)
        subtaskDao.setDone(id, done, now)
        if (item != null) taskDao.touch(item.taskId, now)
        widgetRefresher.refresh()
        pushCloud()
    }

    override suspend fun deleteSubtask(id: String) {
        val item = subtaskDao.get(id)
        subtaskDao.delete(id)
        if (item != null) taskDao.touch(item.taskId, System.currentTimeMillis())
        widgetRefresher.refresh()
        pushCloud()
    }

    override suspend fun pendingReminders(): List<Task> =
        taskDao.pendingReminders(System.currentTimeMillis()).map { it.toDomain() }

    override suspend fun countOpen(): Int = taskDao.countOpen()

    override suspend fun pruneCompletedBefore(before: Long) {
        taskDao.deleteCompletedBefore(before)
    }

    private suspend fun pushCloud() {
        if (syncPort.isEnabled) runCatching { syncPort.push() }
    }
}
