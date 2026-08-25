package com.kizen.tasks.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kizen.tasks.ui.theme.KizenTypography
import com.kizen.tasks.ui.theme.Overlay
import com.kizen.tasks.ui.theme.TextMain
import kotlinx.coroutines.delay

@Composable
fun CelebrationOverlay(
    visible: Boolean,
    message: String,
    onFinished: () -> Unit,
) {
    LaunchedEffect(visible) {
        if (visible) {
            delay(1800)
            onFinished()
        }
    }
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + scaleIn(initialScale = 0.8f),
        exit = fadeOut() + scaleOut(targetScale = 1.1f),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Overlay),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Nubi(NubiMood.Party, size = 180.dp)
                Text(
                    "¡Genial!",
                    style = KizenTypography.headlineMedium,
                    color = TextMain,
                )
                Text(
                    message,
                    style = KizenTypography.bodyLarge,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 32.dp, vertical = 8.dp),
                )
            }
        }
    }
}
