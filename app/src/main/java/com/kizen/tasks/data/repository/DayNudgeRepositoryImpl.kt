package com.kizen.tasks.data.repository

import com.kizen.tasks.data.local.dao.DayNudgeDao
import com.kizen.tasks.data.local.dao.NudgeItemDao
import com.kizen.tasks.data.local.toDomain
import com.kizen.tasks.data.local.toEntity
import com.kizen.tasks.domain.model.DayNudge
import com.kizen.tasks.domain.model.NudgeItem
import com.kizen.tasks.domain.repository.DayNudgeRepository
import com.kizen.tasks.notification.KizenNotifier
import com.kizen.tasks.notification.ReminderScheduler
import com.kizen.tasks.sync.AlexaChimeClient
import com.kizen.tasks.sync.SyncPort
import com.kizen.tasks.sync.TombstoneStore
import com.kizen.tasks.widget.WidgetRefresher
import dagger.hilt.android.qualifiers.ApplicationContext
import android.content.Context
import com.kizen.tasks.domain.model.KizenDates
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DayNudgeRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val nudgeDao: DayNudgeDao,
    private val itemDao: NudgeItemDao,
    private val reminderScheduler: ReminderScheduler,
    private val widgetRefresher: WidgetRefresher,
    private val syncPort: SyncPort,
    private val alexaChime: AlexaChimeClient,
    private val tombstones: TombstoneStore,
) : DayNudgeRepository {

    override fun observeToday(): Flow<List<DayNudge>> {
        return combine(
            nudgeDao.observeAll(),
            itemDao.observeAll(),
        ) { nudges, items ->
            val today = KizenDates.todayEpoch()
            val grouped = items.groupBy { it.nudgeId }
            nudges.filter { it.dayEpoch == today }.map { entity ->
                entity.toDomain(grouped[entity.id].orEmpty().sortedBy { it.position }.map { it.toDomain() })
            }
        }
    }

    override fun observeItems(nudgeId: String): Flow<List<NudgeItem>> =
        itemDao.observeByNudge(nudgeId).map { rows -> rows.map { it.toDomain() } }

    override suspend fun todaySnapshot(): List<DayNudge> {
        val today = KizenDates.todayEpoch()
        return nudgeDao.forDay(today).map { entity ->
            entity.toDomain(itemDao.forNudge(entity.id).map { it.toDomain() })
        }
    }

    override suspend fun get(id: String): DayNudge? {
        val entity = nudgeDao.get(id) ?: return null
        return entity.toDomain(itemDao.forNudge(id).map { it.toDomain() })
    }

    override suspend fun upsert(nudge: DayNudge) {
        tombstones.clearNudge(nudge.id)
        nudgeDao.upsert(nudge.toEntity())
        reminderScheduler.syncNudge(nudge)
        widgetRefresher.refresh()
        pushCloud()
    }

    override suspend fun setDone(id: String, done: Boolean) {
        nudgeDao.setDone(id, done, System.currentTimeMillis())
        if (done) {
            reminderScheduler.cancelNudge(id)
            KizenNotifier.cancel(context, nudgeNotifyId(id))
            alexaChime.enqueueNudge(id, cancel = true)
        } else {
            get(id)?.let { reminderScheduler.syncNudge(it) }
        }
        widgetRefresher.refresh()
        pushCloud()
    }

    override suspend fun delete(id: String) {
        reminderScheduler.cancelNudge(id)
        KizenNotifier.cancel(context, nudgeNotifyId(id))
        alexaChime.enqueueNudge(id, cancel = true)
        tombstones.markNudge(id)
        nudgeDao.delete(id)
        widgetRefresher.refresh()
        pushCloud()
    }

    override suspend fun upsertItem(item: NudgeItem) {
        itemDao.upsert(item.toEntity())
        widgetRefresher.refresh()
        pushCloud()
    }

    override suspend fun setItemDone(id: String, done: Boolean) {
        itemDao.setDone(id, done, System.currentTimeMillis())
        widgetRefresher.refresh()
        pushCloud()
    }

    override suspend fun deleteItem(id: String) {
        itemDao.delete(id)
        widgetRefresher.refresh()
        pushCloud()
    }

    override suspend fun pendingToday(): List<DayNudge> {
        val today = KizenDates.todayEpoch()
        return nudgeDao.pendingToday(today).map { entity ->
            entity.toDomain(itemDao.forNudge(entity.id).map { it.toDomain() })
        }
    }

    override suspend fun pruneOlderThanYesterday() {
        val today = KizenDates.todayEpoch()
        nudgeDao.pendingBefore(today).forEach { reminderScheduler.cancelNudge(it.id) }
        nudgeDao.deleteOlderThan(today)
        widgetRefresher.refresh()
    }

    private fun nudgeNotifyId(id: String): Int = "nudge:$id".hashCode()

    private suspend fun pushCloud() {
        if (syncPort.isEnabled) runCatching { syncPort.push() }
    }
}
