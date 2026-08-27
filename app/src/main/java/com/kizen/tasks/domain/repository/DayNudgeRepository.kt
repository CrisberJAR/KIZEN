package com.kizen.tasks.domain.repository

import com.kizen.tasks.domain.model.DayNudge
import com.kizen.tasks.domain.model.NudgeItem
import kotlinx.coroutines.flow.Flow

interface DayNudgeRepository {
    fun observeToday(): Flow<List<DayNudge>>
    fun observeItems(nudgeId: String): Flow<List<NudgeItem>>
    suspend fun todaySnapshot(): List<DayNudge>
    suspend fun get(id: String): DayNudge?
    suspend fun upsert(nudge: DayNudge)
    suspend fun setDone(id: String, done: Boolean)
    suspend fun delete(id: String)
    suspend fun upsertItem(item: NudgeItem)
    suspend fun setItemDone(id: String, done: Boolean)
    suspend fun deleteItem(id: String)
    suspend fun pendingToday(): List<DayNudge>
    suspend fun pruneOlderThanYesterday()
}
