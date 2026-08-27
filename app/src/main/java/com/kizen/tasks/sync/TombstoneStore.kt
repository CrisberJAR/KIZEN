package com.kizen.tasks.sync

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TombstoneStore @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val prefs = context.getSharedPreferences("kizen_tombstones", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    @Serializable
    data class Bag(
        val tasks: Map<String, Long> = emptyMap(),
        val habits: Map<String, Long> = emptyMap(),
        val dayNudges: Map<String, Long> = emptyMap(),
        val lists: Map<String, Long> = emptyMap(),
    )

    private val lock = Any()
    private var bag: Bag = prune(readPrefs())

    fun markTask(id: String) = mark { it.copy(tasks = it.tasks + (id to now())) }
    fun markHabit(id: String) = mark { it.copy(habits = it.habits + (id to now())) }
    fun markNudge(id: String) = mark { it.copy(dayNudges = it.dayNudges + (id to now())) }
    fun markList(id: String) = mark { it.copy(lists = it.lists + (id to now())) }

    fun clearTask(id: String) = mark { it.copy(tasks = it.tasks - id) }
    fun clearHabit(id: String) = mark { it.copy(habits = it.habits - id) }
    fun clearNudge(id: String) = mark { it.copy(dayNudges = it.dayNudges - id) }
    fun clearList(id: String) = mark { it.copy(lists = it.lists - id) }

    fun taskIds(): Set<String> = snapshot().tasks.keys
    fun habitIds(): Set<String> = snapshot().habits.keys
    fun nudgeIds(): Set<String> = snapshot().dayNudges.keys
    fun listIds(): Set<String> = snapshot().lists.keys

    fun exportTasks(): List<DeletedRefDto> = snapshot().tasks.map { DeletedRefDto(it.key, it.value) }
    fun exportHabits(): List<DeletedRefDto> = snapshot().habits.map { DeletedRefDto(it.key, it.value) }
    fun exportNudges(): List<DeletedRefDto> = snapshot().dayNudges.map { DeletedRefDto(it.key, it.value) }
    fun exportLists(): List<DeletedRefDto> = snapshot().lists.map { DeletedRefDto(it.key, it.value) }

    fun absorb(tasks: List<DeletedRefDto>, habits: List<DeletedRefDto>, nudges: List<DeletedRefDto>, lists: List<DeletedRefDto>) {
        mark { current ->
            current.copy(
                tasks = mergeMaps(current.tasks, tasks),
                habits = mergeMaps(current.habits, habits),
                dayNudges = mergeMaps(current.dayNudges, nudges),
                lists = mergeMaps(current.lists, lists),
            )
        }
    }

    private fun snapshot(): Bag = synchronized(lock) { prune(bag) }

    private fun mark(block: (Bag) -> Bag) {
        synchronized(lock) {
            bag = prune(block(bag))
            writePrefs(bag)
        }
    }

    private fun readPrefs(): Bag = runCatching {
        json.decodeFromString(Bag.serializer(), prefs.getString(KEY, "{}").orEmpty().ifBlank { "{}" })
    }.getOrDefault(Bag())

    private fun writePrefs(value: Bag) {
        prefs.edit().putString(KEY, json.encodeToString(Bag.serializer(), value)).commit()
    }

    private fun prune(value: Bag): Bag {
        val cutoff = now() - THIRTY_DAYS
        fun Map<String, Long>.fresh() = filterValues { it >= cutoff }
        return value.copy(
            tasks = value.tasks.fresh(),
            habits = value.habits.fresh(),
            dayNudges = value.dayNudges.fresh(),
            lists = value.lists.fresh(),
        )
    }

    private fun mergeMaps(local: Map<String, Long>, remote: List<DeletedRefDto>): Map<String, Long> {
        val out = local.toMutableMap()
        remote.forEach { item ->
            if (item.id.isNotBlank()) {
                out[item.id] = maxOf(out[item.id] ?: 0L, item.deletedAt)
            }
        }
        return out
    }

    private fun now() = System.currentTimeMillis()

    companion object {
        private const val KEY = "bag"
        private const val THIRTY_DAYS = 30L * 24 * 60 * 60 * 1000
    }
}
