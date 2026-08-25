package com.kizen.tasks.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.kizen.tasks.MainActivity
import com.kizen.tasks.R
import com.kizen.tasks.domain.repository.HabitRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class ReminderReceiver : BroadcastReceiver() {

    @Inject lateinit var habitRepository: HabitRepository
    @Inject lateinit var reminderScheduler: ReminderScheduler

    override fun onReceive(context: Context, intent: Intent) {
        val title = intent.getStringExtra(AlarmReminderScheduler.EXTRA_TITLE) ?: return
        val list = intent.getStringExtra(AlarmReminderScheduler.EXTRA_LIST).orEmpty()
        val kind = intent.getStringExtra(AlarmReminderScheduler.EXTRA_KIND).orEmpty()
        val taskId = intent.getStringExtra(AlarmReminderScheduler.EXTRA_TASK_ID).orEmpty()
        val habitId = intent.getStringExtra(AlarmReminderScheduler.EXTRA_HABIT_ID).orEmpty()
        val notifyId = (if (kind == AlarmReminderScheduler.KIND_HABIT) "habit:$habitId" else "task:$taskId").hashCode()

        val manager = context.getSystemService(NotificationManager::class.java)
        val channelId = "kizen_reminders"
        manager.createNotificationChannel(
            NotificationChannel(
                channelId,
                context.getString(R.string.channel_reminders),
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = context.getString(R.string.channel_reminders_desc)
                enableVibration(true)
            },
        )

        val open = PendingIntent.getActivity(
            context,
            notifyId,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val body = if (kind == AlarmReminderScheduler.KIND_HABIT) {
            "Tu hábito de hoy te espera ✨"
        } else if (list.isBlank()) {
            "Kizen te recuerda tu tarea ✨"
        } else {
            "$list · Kizen te espera ✨"
        }

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Hora de: $title")
            .setContentText(body)
            .setAutoCancel(true)
            .setContentIntent(open)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        manager.notify(notifyId, notification)

        if (kind == AlarmReminderScheduler.KIND_HABIT && habitId.isNotBlank()) {
            val pending = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    habitRepository.getHabit(habitId)?.let { reminderScheduler.syncHabit(it) }
                } finally {
                    pending.finish()
                }
            }
        }
    }
}
