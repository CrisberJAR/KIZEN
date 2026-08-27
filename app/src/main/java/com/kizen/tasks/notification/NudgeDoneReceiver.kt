package com.kizen.tasks.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.kizen.tasks.domain.repository.DayNudgeRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class NudgeDoneReceiver : BroadcastReceiver() {

    @Inject lateinit var nudgeRepository: DayNudgeRepository

    override fun onReceive(context: Context, intent: Intent) {
        val nudgeId = intent.getStringExtra(AlarmReminderScheduler.EXTRA_NUDGE_ID).orEmpty()
        if (nudgeId.isBlank()) return
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                nudgeRepository.setDone(nudgeId, true)
            } finally {
                pending.finish()
            }
        }
    }
}
