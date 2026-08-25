package com.kizen.tasks.sync

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HttpSyncPort @Inject constructor(
    private val settings: SyncSettings,
    private val store: SyncLocalStore,
    private val apiFactory: KizenApiFactory,
) : SyncPort {

    override val isEnabled: Boolean
        get() = settings.isEnabled

    override suspend fun pull(): Result<Unit> = runCatching {
        if (!settings.isEnabled) return@runCatching
        store.merge(apiFactory.api().getSync())
    }

    override suspend fun push(): Result<Unit> = runCatching {
        if (!settings.isEnabled) return@runCatching
        apiFactory.api().putSync(store.export())
        Unit
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
