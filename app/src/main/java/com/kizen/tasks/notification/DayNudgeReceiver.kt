package com.kizen.tasks.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.kizen.tasks.domain.model.nextNudgeMillis
import com.kizen.tasks.domain.repository.DayNudgeRepository
import com.kizen.tasks.sync.AlexaChimeClient
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class DayNudgeReceiver : BroadcastReceiver() {

    @Inject lateinit var nudgeRepository: DayNudgeRepository
    @Inject lateinit var reminderScheduler: ReminderScheduler
    @Inject lateinit var alexaChime: AlexaChimeClient

    override fun onReceive(context: Context, intent: Intent) {
        val nudgeId = intent.getStringExtra(AlarmReminderScheduler.EXTRA_NUDGE_ID).orEmpty()
        if (nudgeId.isBlank()) return
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            var keepAliveForSound = false
            try {
                val nudge = nudgeRepository.get(nudgeId) ?: return@launch
                if (nudge.isDone) {
                    reminderScheduler.cancelNudge(nudgeId)
                    KizenNotifier.cancel(context, notifyId(nudgeId))
                    alexaChime.enqueueNudge(nudgeId, nudge.title, cancel = true)
                    return@launch
                }
                val interval = nudge.intervalMinutes.coerceAtLeast(5)
                alexaChime.enqueueNudge(nudge.id, nudge.title)
                KizenNotifier.showNudge(
                    context = context,
                    nudgeId = nudge.id,
                    title = "Aún pendiente: ${nudge.title}",
                    body = "Te aviso cada $interval min hasta que lo marques como hecho.",
                    notifyId = notifyId(nudge.id),
                )
                val nextAt = nextNudgeMillis(nudge)
                if (nextAt == null) {
                    reminderScheduler.cancelNudge(nudgeId)
                } else {
                    reminderScheduler.scheduleNudgeAt(nudge, nextAt)
                }
                keepAliveForSound = true
                AlarmPlayer.play(context, durationMs = 10_000L, pending = pending)
            } finally {
                if (!keepAliveForSound) pending.finish()
            }
        }
    }

    private fun notifyId(id: String): Int = "nudge:$id".hashCode()
}
