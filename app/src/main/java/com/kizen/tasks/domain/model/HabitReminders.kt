package com.kizen.tasks.domain.model

import java.time.ZonedDateTime

fun nextHabitReminderMillis(
    habit: Habit,
    now: ZonedDateTime = ZonedDateTime.now(KizenDates.ZONE),
): Long? {
    val minutes = habit.reminderMinutes ?: return null
    if (!habit.isActive || habit.repeatDays.isEmpty()) return null
    var day = now.toLocalDate()
    repeat(8) {
        val scheduled = day.dayOfWeek in habit.repeatDays
        val skipToday = day == now.toLocalDate() && habit.doneToday
        if (scheduled && !skipToday) {
            val at = day.atStartOfDay(now.zone).plusMinutes(minutes.toLong())
            if (at.isAfter(now)) return at.toInstant().toEpochMilli()
        }
        day = day.plusDays(1)
    }
    return null
}
