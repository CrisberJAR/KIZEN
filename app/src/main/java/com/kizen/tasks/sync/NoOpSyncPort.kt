package com.kizen.tasks.sync

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NoOpSyncPort @Inject constructor() : SyncPort {
    override val isEnabled: Boolean = false
    override suspend fun pull(): Result<Unit> = Result.success(Unit)
    override suspend fun push(): Result<Unit> = Result.success(Unit)
}
