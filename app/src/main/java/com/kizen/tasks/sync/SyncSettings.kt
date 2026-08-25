package com.kizen.tasks.sync

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncSettings @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val prefs = context.getSharedPreferences("kizen_sync", Context.MODE_PRIVATE)

    var isEnabled: Boolean
        get() = prefs.getBoolean(KEY_ENABLED, false)
        set(value) { prefs.edit().putBoolean(KEY_ENABLED, value).apply() }

    var baseUrl: String
        get() = prefs.getString(KEY_URL, DEFAULT_URL).orEmpty().ifBlank { DEFAULT_URL }
        set(value) { prefs.edit().putString(KEY_URL, value.trim()).apply() }

    var userId: String
        get() {
            val stored = prefs.getString(KEY_USER, null)
            if (!stored.isNullOrBlank() && !stored.startsWith("user-")) return stored
            prefs.edit().putString(KEY_USER, DEFAULT_USER).apply()
            return DEFAULT_USER
        }
        set(value) {
            val clean = value.trim().ifBlank { DEFAULT_USER }
            prefs.edit().putString(KEY_USER, clean).apply()
        }

    fun normalizedBaseUrl(): String {
        val raw = baseUrl.trim().removeSuffix("/")
        return "$raw/"
    }

    companion object {
        const val DEFAULT_URL = "http://10.0.2.2:8787"
        const val DEFAULT_USER = "kizen-casa"
        private const val KEY_ENABLED = "enabled"
        private const val KEY_URL = "base_url"
        private const val KEY_USER = "user_id"
    }
}
