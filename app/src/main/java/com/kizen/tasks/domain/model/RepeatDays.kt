package com.kizen.tasks.domain.model

import java.time.DayOfWeek

object RepeatDays {
    const val ALL = 0b1111111
    const val WEEKDAYS = 0b0011111

    fun toMask(days: Set<DayOfWeek>): Int =
        days.fold(0) { acc, day -> acc or bit(day) }

    fun fromMask(mask: Int): Set<DayOfWeek> =
        DayOfWeek.entries.filter { mask and bit(it) != 0 }.toSet()

    fun bit(day: DayOfWeek): Int = 1 shl (day.value - 1)

    fun label(day: DayOfWeek): String = when (day) {
        DayOfWeek.MONDAY -> "L"
        DayOfWeek.TUESDAY -> "M"
        DayOfWeek.WEDNESDAY -> "X"
        DayOfWeek.THURSDAY -> "J"
        DayOfWeek.FRIDAY -> "V"
        DayOfWeek.SATURDAY -> "S"
        DayOfWeek.SUNDAY -> "D"
    }
}
