package com.kizen.tasks.ui.theme

import androidx.compose.ui.graphics.Color

val CreamBg = Color(0xFFFFF8F4)
val CreamCard = Color(0xFFFFFDFB)
val PinkSoft = Color(0xFFF8C8DC)
val PinkDeep = Color(0xFFE891B0)
val Sky = Color(0xFFA8D8EA)
val SkyDeep = Color(0xFF7EC4DC)
val Mint = Color(0xFFB5EAD7)
val MintDeep = Color(0xFF7DCFB6)
val Lavender = Color(0xFFC7CEEA)
val LavenderDeep = Color(0xFF9AA3D0)
val CreamYellow = Color(0xFFFFF2CC)
val TextMain = Color(0xFF4A4458)
val TextMute = Color(0xFF8A8198)
val White = Color(0xFFFFFFFF)
val Overlay = Color(0x66F4E4DA)

fun parseHexColor(hex: String): Color {
    val cleaned = hex.trim().removePrefix("#")
    val value = when (cleaned.length) {
        6 -> "FF$cleaned"
        8 -> cleaned
        else -> "FFC7CEEA"
    }
    return Color(value.toLong(16))
}
