package com.kizen.tasks.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kizen.tasks.ui.theme.CreamCard
import com.kizen.tasks.ui.theme.KizenTypography
import com.kizen.tasks.ui.theme.TextMute

@Composable
fun SoftCard(
    modifier: Modifier = Modifier,
    color: Color = CreamCard,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .shadow(8.dp, RoundedCornerShape(28.dp), ambientColor = Color(0x22000000), spotColor = Color(0x1A000000))
            .clip(RoundedCornerShape(28.dp))
            .background(color)
            .padding(18.dp),
        content = content,
    )
}

@Composable
fun EmptyState(
    mood: NubiMood,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Nubi(mood = mood, size = 168.dp)
        Text(title, style = KizenTypography.headlineSmall, textAlign = TextAlign.Center)
        Text(
            subtitle,
            style = KizenTypography.bodyMedium,
            color = TextMute,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp, start = 24.dp, end = 24.dp),
        )
    }
}

@Composable
fun SoftChip(
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.55f))
            .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        Text(text, style = KizenTypography.labelLarge)
    }
}
