package com.kizen.tasks.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.kizen.tasks.MainActivity
import com.kizen.tasks.domain.model.DayNudge
import com.kizen.tasks.domain.model.Habit
import com.kizen.tasks.domain.model.Priority
import com.kizen.tasks.domain.model.Task
import com.kizen.tasks.domain.model.nextHabitReminderMillis
import com.kizen.tasks.domain.model.nextNudgeMillis
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AlarmReminderScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
) : ReminderScheduler {

    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    override fun sync(task: Task) {
        val at = task.alarmAt()
        if (at == null) {
            cancel(task.id)
            return
        }
        schedule(
            receiver = TaskAlarmReceiver::class.java,
            requestKey = "task:${task.id}",
            at = at,
            extras = mapOf(
                EXTRA_KIND to KIND_TASK,
                EXTRA_TASK_ID to task.id,
                EXTRA_TITLE to task.title,
                EXTRA_LIST to "${task.listEmoji} ${task.listName}".trim(),
                EXTRA_PRIORITY to task.priority.name,
            ),
        )
    }

    override fun cancel(taskId: String) {
        cancelKey("task:$taskId", TaskAlarmReceiver::class.java)
        cancelKey("task:$taskId", ReminderReceiver::class.java)
    }

    override fun syncHabit(habit: Habit) {
        val at = nextHabitReminderMillis(habit)
        if (at == null) {
            cancelHabit(habit.id)
            return
        }
        schedule(
            receiver = ReminderReceiver::class.java,
            requestKey = "habit:${habit.id}",
            at = at,
            extras = mapOf(
                EXTRA_KIND to KIND_HABIT,
                EXTRA_HABIT_ID to habit.id,
                EXTRA_TITLE to habit.title,
                EXTRA_LIST to "${habit.emoji} hábito",
                EXTRA_PRIORITY to Priority.MEDIUM.name,
            ),
        )
    }

    override fun cancelHabit(habitId: String) {
        cancelKey("habit:$habitId", ReminderReceiver::class.java)
    }

    override fun syncNudge(nudge: DayNudge) {
        val at = nextNudgeMillis(nudge)
        if (at == null) {
            cancelNudge(nudge.id)
            return
        }
        scheduleNudgeAt(nudge, at)
    }

    override fun scheduleNudgeAt(nudge: DayNudge, atMillis: Long) {
        if (nudge.isDone) {
            cancelNudge(nudge.id)
            return
        }
        schedule(
            receiver = DayNudgeReceiver::class.java,
            requestKey = "nudge:${nudge.id}",
            at = atMillis,
            extras = mapOf(
                EXTRA_KIND to KIND_NUDGE,
                EXTRA_NUDGE_ID to nudge.id,
                EXTRA_TITLE to nudge.title,
                EXTRA_LIST to "Aviso de hoy",
                EXTRA_INTERVAL to nudge.intervalMinutes.toString(),
            ),
        )
    }

    override fun cancelNudge(nudgeId: String) {
        cancelKey("nudge:$nudgeId", DayNudgeReceiver::class.java)
    }

    private fun schedule(
        receiver: Class<*>,
        requestKey: String,
        at: Long,
        extras: Map<String, String>,
    ) {
        val intent = Intent(context, receiver).apply {
            extras.forEach { (key, value) -> putExtra(key, value) }
        }
        val pending = pendingIntent(requestKey, intent, create = true) ?: return
        val show = PendingIntent.getActivity(
            context,
            requestKey.hashCode(),
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val exactAllowed = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()
        if (exactAllowed) {
            alarmManager.setAlarmClock(AlarmManager.AlarmClockInfo(at, show), pending)
        } else {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pending)
        }
    }

    private fun cancelKey(requestKey: String, receiver: Class<*>) {
        val pending = pendingIntent(requestKey, Intent(context, receiver), create = false)
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
        const val EXTRA_NUDGE_ID = "nudge_id"
        const val EXTRA_TITLE = "title"
        const val EXTRA_LIST = "list"
        const val EXTRA_PRIORITY = "priority"
        const val EXTRA_INTERVAL = "interval"
        const val KIND_TASK = "task"
        const val KIND_HABIT = "habit"
        const val KIND_NUDGE = "nudge"
    }
}
