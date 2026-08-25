package com.kizen.tasks.work

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MaintenanceScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun ensureScheduled() {
        enqueue(ExistingWorkPolicy.KEEP)
    }

    fun enqueueNext() {
        enqueue(ExistingWorkPolicy.REPLACE)
    }

    private fun enqueue(policy: ExistingWorkPolicy) {
        val delay = millisUntilNextRun()
        val request = OneTimeWorkRequestBuilder<DailyMaintenanceWorker>()
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .addTag(TAG)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(UNIQUE, policy, request)
    }

    private fun millisUntilNextRun(): Long {
        val zone = ZoneId.systemDefault()
        val now = ZonedDateTime.now(zone)
        var next = LocalDate.now(zone).atTime(LocalTime.of(0, 5)).atZone(zone)
        if (!next.isAfter(now)) next = next.plusDays(1)
        return Duration.between(now, next).toMillis().coerceAtLeast(60_000L)
    }

    companion object {
        const val UNIQUE = "kizen-daily-maintenance"
        const val TAG = "kizen-daily"
    }
}
