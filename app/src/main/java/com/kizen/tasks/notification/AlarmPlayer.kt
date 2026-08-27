package com.kizen.tasks.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Handler
import android.os.Looper
import android.os.PowerManager

object AlarmPlayer {
    fun play(
        context: Context,
        durationMs: Long = 12_000L,
        pending: BroadcastReceiver.PendingResult? = null,
    ) {
        val power = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val wake = power.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "kizen:alarm")
        wake.acquire(durationMs + 2_000L)
        val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val ringtone = RingtoneManager.getRingtone(context, uri)
        ringtone.audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        ringtone.play()
        Handler(Looper.getMainLooper()).postDelayed({
            runCatching { if (ringtone.isPlaying) ringtone.stop() }
            runCatching { if (wake.isHeld) wake.release() }
            runCatching { pending?.finish() }
        }, durationMs)
    }
}
