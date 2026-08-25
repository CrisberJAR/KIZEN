package com.kizen.tasks.domain.model

import java.time.DayOfWeek
import java.time.LocalDate

object StreakCalculator {
    fun current(
        doneDays: Set<Long>,
        repeatDays: Set<DayOfWeek>,
        today: LocalDate,
        createdOn: LocalDate,
    ): Int {
        if (repeatDays.isEmpty()) return 0
        var cursor = today
        if (cursor.dayOfWeek in repeatDays && cursor.toEpochDay() !in doneDays) {
            cursor = cursor.minusDays(1)
        }
        var streak = 0
        var steps = 0
        while (steps++ < 4000 && !cursor.isBefore(createdOn)) {
            if (cursor.dayOfWeek in repeatDays) {
                if (cursor.toEpochDay() in doneDays) {
                    streak++
                    cursor = cursor.minusDays(1)
                } else {
                    break
                }
            } else {
                cursor = cursor.minusDays(1)
            }
        }
        return streak
    }

    fun longest(doneDays: Set<Long>, repeatDays: Set<DayOfWeek>): Int {
        if (doneDays.isEmpty() || repeatDays.isEmpty()) return 0
        val start = LocalDate.ofEpochDay(doneDays.min())
        val end = LocalDate.ofEpochDay(doneDays.max())
        var best = 0
        var run = 0
        var day = start
        while (!day.isAfter(end)) {
            if (day.dayOfWeek in repeatDays) {
                if (day.toEpochDay() in doneDays) {
                    run++
                    best = maxOf(best, run)
                } else {
                    run = 0
                }
            }
            day = day.plusDays(1)
        }
        return best
    }
}
