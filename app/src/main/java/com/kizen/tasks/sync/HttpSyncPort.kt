package com.kizen.tasks.sync

import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HttpSyncPort @Inject constructor(
    private val settings: SyncSettings,
    private val store: SyncLocalStore,
    private val apiFactory: KizenApiFactory,
) : SyncPort {

    private val gate = Mutex()

    override val isEnabled: Boolean
        get() = settings.isEnabled

    override suspend fun pull(): Result<Unit> = runCatching {
        if (!settings.isEnabled) return@runCatching
        gate.withLock { store.merge(apiFactory.api().getSync()) }
    }

    override suspend fun push(): Result<Unit> = runCatching {
        if (!settings.isEnabled) return@runCatching
        gate.withLock { store.merge(apiFactory.api().putSync(store.export())) }
    }

    override suspend fun sync(): Result<Unit> = runCatching {
        if (!settings.isEnabled) return@runCatching
        gate.withLock {
            var lastError: Throwable? = null
            repeat(3) { attempt ->
                try {
                    store.merge(apiFactory.api().getSync())
                    store.merge(apiFactory.api().putSync(store.export()))
                    return@withLock
                } catch (error: Throwable) {
                    lastError = error
                    if (attempt < 2) delay(2_000L * (attempt + 1))
                }
            }
            throw lastError ?: IllegalStateException("No pude sincronizar")
        }
    }
}

@Singleton
class SmartInsightsPort @Inject constructor(
    private val settings: SyncSettings,
    private val local: LocalInsightsPort,
    private val apiFactory: KizenApiFactory,
) : InsightsPort {
    override suspend fun summary(): Result<InsightSummaryDto> {
        if (!settings.isEnabled) return local.summary()
        return runCatching { apiFactory.api().insights() }
            .recoverCatching { local.summary().getOrThrow() }
    }
}
