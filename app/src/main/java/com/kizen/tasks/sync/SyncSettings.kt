package com.kizen.tasks.sync

import android.content.Context
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncSettings @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val prefs = context.getSharedPreferences("kizen_sync", Context.MODE_PRIVATE)

    init {
        val stored = prefs.getString(KEY_USER, null)
        if (stored.isNullOrBlank() || stored.startsWith("user-")) {
            prefs.edit().putString(KEY_USER, DEFAULT_USER).apply()
        }
        val url = prefs.getString(KEY_URL, null)
        if (url.isNullOrBlank() || (!isEmulator() && isLoopback(url))) {
            val editor = prefs.edit().putString(KEY_URL, defaultUrl())
            if (!isEmulator()) editor.putBoolean(KEY_ENABLED, true)
            editor.apply()
        }
    }

    var isEnabled: Boolean
        get() = prefs.getBoolean(KEY_ENABLED, true)
        set(value) { prefs.edit().putBoolean(KEY_ENABLED, value).apply() }

    var baseUrl: String
        get() = prefs.getString(KEY_URL, defaultUrl()).orEmpty().ifBlank { defaultUrl() }
        set(value) { prefs.edit().putString(KEY_URL, value.trim()).apply() }

    var userId: String
        get() = prefs.getString(KEY_USER, DEFAULT_USER).orEmpty().ifBlank { DEFAULT_USER }
        set(value) {
            val clean = value.trim().ifBlank { DEFAULT_USER }
            prefs.edit().putString(KEY_USER, clean).apply()
        }

    fun normalizedBaseUrl(): String {
        val raw = baseUrl.trim().removeSuffix("/")
        return "$raw/"
    }

    companion object {
        const val CLOUD_URL = "https://kizen-api.onrender.com"
        const val EMULATOR_URL = "http://10.0.2.2:8787"
        const val DEFAULT_USER = "kizen-casa"
        private const val KEY_ENABLED = "enabled"
        private const val KEY_URL = "base_url"
        private const val KEY_USER = "user_id"

        fun defaultUrl(): String = if (isEmulator()) EMULATOR_URL else CLOUD_URL

        private fun isLoopback(url: String): Boolean {
            val value = url.lowercase()
            return value.contains("10.0.2.2") || value.contains("localhost") || value.contains("127.0.0.1")
        }

        private fun isEmulator(): Boolean {
            val fingerprint = Build.FINGERPRINT
            val model = Build.MODEL
            val hardware = Build.HARDWARE
            return fingerprint.startsWith("generic") ||
                fingerprint.contains("emulator") ||
                model.contains("Emulator") ||
                hardware.contains("ranchu") ||
                hardware.contains("goldfish")
        }
    }
}
