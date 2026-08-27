package com.kizen.tasks.notification

import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat

object AlarmPermissions {
    private const val PREFS = "kizen_permissions"
    private const val KEY_ASKED_EXACT = "asked_exact_alarm"

    fun notificationsAllowed(context: Context): Boolean =
        NotificationManagerCompat.from(context).areNotificationsEnabled()

    fun exactAlarmsAllowed(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        val manager = context.getSystemService(AlarmManager::class.java)
        return manager.canScheduleExactAlarms()
    }

    fun openExactAlarmSettings(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
        val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
            data = Uri.parse("package:${context.packageName}")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }

    fun ensureAlarmChannel(context: Context) {
        KizenNotifier.ensureAlarmChannel(context)
    }

    fun requestExactAlarmsOnce(context: Context) {
        if (exactAlarmsAllowed(context)) return
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (prefs.getBoolean(KEY_ASKED_EXACT, false)) return
        prefs.edit().putBoolean(KEY_ASKED_EXACT, true).apply()
        openExactAlarmSettings(context)
    }
}
