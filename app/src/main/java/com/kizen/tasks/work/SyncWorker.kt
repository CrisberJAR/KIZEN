package com.kizen.tasks.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.kizen.tasks.sync.SyncPort
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val syncPort: SyncPort,
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        if (!syncPort.isEnabled) return Result.success()
        val outcome = syncPort.sync()
        return if (outcome.isSuccess) Result.success() else Result.retry()
    }
}
