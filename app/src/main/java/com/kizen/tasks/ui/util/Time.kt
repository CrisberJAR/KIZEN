package com.kizen.tasks.ui.util

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val zone: ZoneId get() = ZoneId.systemDefault()
private val timeFmt = DateTimeFormatter.ofPattern("HH:mm", Locale("es", "MX"))
private val dateFmt = DateTimeFormatter.ofPattern("EEE d MMM", Locale("es", "MX"))
private val dateTimeFmt = DateTimeFormatter.ofPattern("EEE d MMM · HH:mm", Locale("es", "MX"))

fun greeting(): String {
    val hour = java.time.LocalTime.now().hour
    return when {
        hour < 12 -> "Buenos días"
        hour < 19 -> "Buenas tardes"
        else -> "Buenas noches"
    }
}

fun formatTime(epochMs: Long): String =
    Instant.ofEpochMilli(epochMs).atZone(zone).toLocalTime().format(timeFmt)

fun formatDate(epochMs: Long): String =
    Instant.ofEpochMilli(epochMs).atZone(zone).toLocalDate().format(dateFmt)

fun formatDateTime(epochMs: Long): String =
    Instant.ofEpochMilli(epochMs).atZone(zone).format(dateTimeFmt)

fun dayStartMillis(date: LocalDate = LocalDate.now()): Long =
    date.atStartOfDay(zone).toInstant().toEpochMilli()

fun formatMinutesOfDay(minutes: Int): String {
    val hours = (minutes / 60).coerceIn(0, 23)
    val mins = (minutes % 60).coerceIn(0, 59)
    return "%02d:%02d".format(hours, mins)
}
