package com.kizen.tasks.domain.model

import java.time.LocalDate
import java.time.ZoneId

object KizenDates {
    val ZONE: ZoneId = ZoneId.of("America/Lima")

    fun todayEpoch(): Long = LocalDate.now(ZONE).toEpochDay()
}
