package com.kizen.tasks.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val PastelScheme = lightColorScheme(
    primary = PinkDeep,
    onPrimary = White,
    primaryContainer = PinkSoft,
    onPrimaryContainer = TextMain,
    secondary = SkyDeep,
    onSecondary = TextMain,
    secondaryContainer = Sky,
    tertiary = MintDeep,
    tertiaryContainer = Mint,
    background = CreamBg,
    onBackground = TextMain,
    surface = CreamCard,
    onSurface = TextMain,
    surfaceVariant = Lavender.copy(alpha = 0.45f),
    onSurfaceVariant = TextMute,
    outline = Lavender,
    error = Color(0xFFE57373),
)

@Composable
fun KizenTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = PastelScheme,
        typography = KizenTypography,
        shapes = KizenShapes,
        content = content,
    )
}
