package com.kizen.tasks.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.kizen.tasks.domain.model.Habit
import com.kizen.tasks.domain.model.Task
import com.kizen.tasks.domain.model.nextHabitReminderMillis
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AlarmReminderScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
) : ReminderScheduler {

    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    override fun sync(task: Task) {
        val at = task.reminderAt
        if (task.isDone || at == null || at <= System.currentTimeMillis()) {
            cancel(task.id)
            return
        }
        schedule(
            requestKey = "task:${task.id}",
            at = at,
            extras = mapOf(
                EXTRA_KIND to KIND_TASK,
                EXTRA_TASK_ID to task.id,
                EXTRA_TITLE to task.title,
                EXTRA_LIST to "${task.listEmoji} ${task.listName}".trim(),
            ),
        )
    }

    override fun cancel(taskId: String) {
        cancelKey("task:$taskId")
    }

    override fun syncHabit(habit: Habit) {
        val at = nextHabitReminderMillis(habit)
        if (at == null) {
            cancelHabit(habit.id)
            return
        }
        schedule(
            requestKey = "habit:${habit.id}",
            at = at,
            extras = mapOf(
                EXTRA_KIND to KIND_HABIT,
                EXTRA_HABIT_ID to habit.id,
                EXTRA_TITLE to habit.title,
                EXTRA_LIST to "${habit.emoji} hábito",
            ),
        )
    }

    override fun cancelHabit(habitId: String) {
        cancelKey("habit:$habitId")
    }

    private fun schedule(requestKey: String, at: Long, extras: Map<String, String>) {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            extras.forEach { (key, value) -> putExtra(key, value) }
        }
        val pending = pendingIntent(requestKey, intent, create = true) ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pending)
        } else {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pending)
        }
    }

    private fun cancelKey(requestKey: String) {
        val pending = pendingIntent(requestKey, Intent(context, ReminderReceiver::class.java), create = false)
        if (pending != null) {
            alarmManager.cancel(pending)
            pending.cancel()
        }
    }

    private fun pendingIntent(requestKey: String, intent: Intent, create: Boolean): PendingIntent? {
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        val requestCode = requestKey.hashCode()
        return if (create) {
            PendingIntent.getBroadcast(context, requestCode, intent, flags)
        } else {
            PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
            )
        }
    }

    companion object {
        const val EXTRA_KIND = "kind"
        const val EXTRA_TASK_ID = "task_id"
        const val EXTRA_HABIT_ID = "habit_id"
        const val EXTRA_TITLE = "title"
        const val EXTRA_LIST = "list"
        const val KIND_TASK = "task"
        const val KIND_HABIT = "habit"
    }
}
