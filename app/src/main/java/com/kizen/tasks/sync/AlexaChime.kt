package com.kizen.tasks.sync

import kotlinx.serialization.Serializable
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
    private val settings: SyncSettings,
    private val apiFactory: KizenApiFactory,
) {
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
