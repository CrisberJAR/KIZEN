package com.kizen.tasks.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class TaskAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val title = intent.getStringExtra(AlarmReminderScheduler.EXTRA_TITLE) ?: return
        val list = intent.getStringExtra(AlarmReminderScheduler.EXTRA_LIST).orEmpty()
        val taskId = intent.getStringExtra(AlarmReminderScheduler.EXTRA_TASK_ID).orEmpty()
        val high = intent.getStringExtra(AlarmReminderScheduler.EXTRA_PRIORITY) == "HIGH"
        val notifyId = "task:$taskId".hashCode()
        val body = when {
            high && list.isNotBlank() -> "$list · Prioridad alta. Kizen te espera."
            high -> "Prioridad alta. Es hora de esta tarea."
            list.isNotBlank() -> "$list · Kizen te recuerda tu tarea."
            else -> "Kizen te recuerda tu tarea."
        }
        KizenNotifier.showTaskAlarm(
            context = context,
            title = "Hora de: $title",
            body = body,
            notifyId = notifyId,
            highPriority = true,
        )
        AlarmPlayer.play(context, pending = goAsync())
    }
}
