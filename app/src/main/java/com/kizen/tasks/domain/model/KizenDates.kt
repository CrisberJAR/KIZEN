package com.kizen.tasks.domain.model

import java.time.LocalDate
import java.time.ZoneId

object KizenDates {
    val ZONE: ZoneId = ZoneId.of("America/Lima")

    fun today(): LocalDate = LocalDate.now(ZONE)
    fun todayEpoch(): Long = today().toEpochDay()
    fun dayStartMillis(day: LocalDate = today()): Long =
        day.atStartOfDay(ZONE).toInstant().toEpochMilli()
    fun dayEndMillis(day: LocalDate = today()): Long =
        day.plusDays(1).atStartOfDay(ZONE).toInstant().toEpochMilli() - 1
}
