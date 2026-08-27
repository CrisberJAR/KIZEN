package com.kizen.tasks.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.kizen.tasks.domain.model.nudgeFollowupMillis
import com.kizen.tasks.domain.repository.DayNudgeRepository
import com.kizen.tasks.sync.AlexaChimeClient
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
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
                    alexaChime.cancelNudge(nudgeId, nudge.title)
                    return@launch
                }
                coroutineScope {
                    val chime = async { alexaChime.chimeNudge(nudge.id, nudge.title) }
                    val interval = nudge.intervalMinutes.coerceAtLeast(5)
                    KizenNotifier.showNudge(
                        context = context,
                        nudgeId = nudge.id,
                        title = "Aún pendiente: ${nudge.title}",
                        body = "Te aviso cada $interval min hasta que lo marques como hecho.",
                        notifyId = notifyId(nudge.id),
                    )
                    reminderScheduler.scheduleNudgeAt(nudge, nudgeFollowupMillis(nudge))
                    keepAliveForSound = true
                    AlarmPlayer.play(context, durationMs = 10_000L, pending = pending)
                    runCatching { chime.await() }
                }
            } finally {
                if (!keepAliveForSound) pending.finish()
            }
        }
    }

    private fun notifyId(id: String): Int = "nudge:$id".hashCode()
}
