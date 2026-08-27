package com.kizen.tasks.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import com.kizen.tasks.MainActivity
import com.kizen.tasks.R

object KizenNotifier {
    const val CHANNEL_ALARMS = "kizen_task_alarms"
    const val CHANNEL_NUDGES = "kizen_day_nudges_alarm"
    private const val CHANNEL_NUDGES_OLD = "kizen_day_nudges"

    fun ensureAlarmChannel(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        val channel = NotificationChannel(
            CHANNEL_ALARMS,
            context.getString(R.string.channel_alarms),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = context.getString(R.string.channel_alarms_desc)
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 500, 250, 500, 250, 800)
            setSound(alarmUri, attrs)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            setBypassDnd(true)
        }
        manager.createNotificationChannel(channel)
        ensureNudgeChannel(context)
    }

    fun ensureNudgeChannel(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.deleteNotificationChannel(CHANNEL_NUDGES_OLD)
        val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        val channel = NotificationChannel(
            CHANNEL_NUDGES,
            context.getString(R.string.channel_nudges),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = context.getString(R.string.channel_nudges_desc)
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 500, 250, 500, 250, 800)
            setSound(alarmUri, attrs)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            setBypassDnd(true)
        }
        manager.createNotificationChannel(channel)
    }

    fun showTaskAlarm(
        context: Context,
        title: String,
        body: String,
        notifyId: Int,
        highPriority: Boolean,
    ) {
        ensureAlarmChannel(context)
        val manager = context.getSystemService(NotificationManager::class.java)
        val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val open = openApp(context, notifyId)
        val builder = NotificationCompat.Builder(context, CHANNEL_ALARMS)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setContentIntent(open)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setSound(alarmUri)
            .setVibrate(longArrayOf(0, 500, 250, 500, 250, 800))
            .setOnlyAlertOnce(false)
        if (highPriority && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            builder.setFullScreenIntent(open, true)
        }
        manager.notify(notifyId, builder.build())
    }

    fun showNudge(
        context: Context,
        nudgeId: String,
        title: String,
        body: String,
        notifyId: Int,
    ) {
        ensureNudgeChannel(context)
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.cancel(notifyId)
        val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val doneIntent = Intent(context, NudgeDoneReceiver::class.java).apply {
            putExtra(AlarmReminderScheduler.EXTRA_NUDGE_ID, nudgeId)
        }
        val donePending = PendingIntent.getBroadcast(
            context,
            notifyId,
            doneIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val open = openApp(context, notifyId + 1)
        val builder = NotificationCompat.Builder(context, CHANNEL_NUDGES)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(false)
            .setContentIntent(open)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setSound(alarmUri)
            .setVibrate(longArrayOf(0, 500, 250, 500, 250, 800))
            .setOnlyAlertOnce(false)
            .addAction(0, context.getString(R.string.nudge_done_action), donePending)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            builder.setFullScreenIntent(open, true)
        }
        manager.notify(notifyId, builder.build())
    }

    fun cancel(context: Context, notifyId: Int) {
        context.getSystemService(NotificationManager::class.java).cancel(notifyId)
    }

    private fun openApp(context: Context, requestCode: Int): PendingIntent =
        PendingIntent.getActivity(
            context,
            requestCode,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
}
