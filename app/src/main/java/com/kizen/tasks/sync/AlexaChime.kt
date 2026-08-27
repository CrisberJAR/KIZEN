package com.kizen.tasks.sync

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.kizen.tasks.work.AlexaChimeWorker
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.serialization.Serializable
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class AlexaChimeDto(
    val id: String,
    val title: String = "",
    val kind: String = "nudge",
    val cancel: Boolean = false,
)

@Serializable
data class AlexaChimeResultDto(
    val ok: Boolean = false,
    val reason: String? = null,
)

@Singleton
class AlexaChimeClient @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settings: SyncSettings,
    private val apiFactory: KizenApiFactory,
) {
    fun enqueueNudge(id: String, title: String = "", cancel: Boolean = false) {
        if (!settings.isEnabled || id.isBlank()) return
        val request = OneTimeWorkRequestBuilder<AlexaChimeWorker>()
            .setInputData(
                workDataOf(
                    AlexaChimeWorker.KEY_ID to id,
                    AlexaChimeWorker.KEY_TITLE to title,
                    AlexaChimeWorker.KEY_CANCEL to cancel,
                ),
            )
            .setConstraints(
                Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build(),
            )
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 20, TimeUnit.SECONDS)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            "alexa-chime-$id",
            ExistingWorkPolicy.REPLACE,
            request,
        )
    }

    suspend fun chimeNudge(id: String, title: String) {
        if (!settings.isEnabled || id.isBlank()) return
        runCatching { apiFactory.api().alexaChime(AlexaChimeDto(id = id, title = title)) }
    }

    suspend fun cancelNudge(id: String, title: String = "") {
        if (!settings.isEnabled || id.isBlank()) return
        runCatching {
            apiFactory.api().alexaChime(AlexaChimeDto(id = id, title = title, cancel = true))
        }
    }
}
