package com.kizen.tasks.domain.model

fun nextNudgeMillis(
    nudge: DayNudge,
    nowMillis: Long = System.currentTimeMillis(),
    todayEpoch: Long = java.time.LocalDate.now().toEpochDay(),
): Long? {
    if (nudge.isDone) return null
    if (nudge.dayEpoch != todayEpoch) return null
    return if (nowMillis < nudge.startAt) nudge.startAt else nowMillis + 1_500L
}

fun nudgeFollowupMillis(
    nudge: DayNudge,
    nowMillis: Long = System.currentTimeMillis(),
): Long {
    val interval = nudge.intervalMinutes.coerceAtLeast(5) * 60_000L
    return nowMillis + interval
}
