package com.kizen.tasks.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.kizen.tasks.sync.AlexaChimeClient
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class AlexaChimeWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val alexaChime: AlexaChimeClient,
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val id = inputData.getString(KEY_ID).orEmpty()
        val title = inputData.getString(KEY_TITLE).orEmpty()
        val cancel = inputData.getBoolean(KEY_CANCEL, false)
        if (id.isBlank()) return Result.success()
        val outcome = runCatching {
            if (cancel) alexaChime.cancelNudge(id, title) else alexaChime.chimeNudge(id, title)
        }
        if (outcome.isFailure && runAttemptCount < 3) return Result.retry()
        return Result.success()
    }

    companion object {
        const val KEY_ID = "id"
        const val KEY_TITLE = "title"
        const val KEY_CANCEL = "cancel"
    }
}
