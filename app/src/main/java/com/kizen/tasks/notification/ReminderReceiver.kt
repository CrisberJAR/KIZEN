package com.kizen.tasks.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
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
        val high = intent.getStringExtra(AlarmReminderScheduler.EXTRA_PRIORITY) == "HIGH"
        val notifyId = (if (kind == AlarmReminderScheduler.KIND_HABIT) "habit:$habitId" else "task:$taskId").hashCode()
        val body = if (kind == AlarmReminderScheduler.KIND_HABIT) {
            "Tu hábito de hoy te espera."
        } else if (list.isBlank()) {
            "Kizen te recuerda tu tarea."
        } else {
            "$list · Kizen te espera."
        }
        KizenNotifier.showTaskAlarm(
            context = context,
            title = "Hora de: $title",
            body = body,
            notifyId = notifyId,
            highPriority = high,
        )

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
