package com.kizen.tasks.domain.model

fun nextNudgeMillis(
    nudge: DayNudge,
    nowMillis: Long = System.currentTimeMillis(),
    todayEpoch: Long = KizenDates.todayEpoch(),
): Long? {
    if (nudge.isDone) return null
    if (kotlin.math.abs(nudge.dayEpoch - todayEpoch) > 1L) return null
    val interval = nudge.intervalMinutes.coerceAtLeast(5) * 60_000L
    if (nowMillis < nudge.startAt) return nudge.startAt
    val stepsDone = (nowMillis - nudge.startAt) / interval
    var next = nudge.startAt + (stepsDone + 1) * interval
    if (next - nowMillis < 10_000L) next = nowMillis + 10_000L
    return next
}
