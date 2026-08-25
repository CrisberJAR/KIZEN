package com.kizen.tasks.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kizen.tasks.domain.model.Habit
import com.kizen.tasks.ui.theme.CreamCard
import com.kizen.tasks.ui.theme.CreamYellow
import com.kizen.tasks.ui.theme.KizenTypography
import com.kizen.tasks.ui.theme.TextMain
import com.kizen.tasks.ui.theme.TextMute
import com.kizen.tasks.ui.theme.parseHexColor

@Composable
fun HabitCard(
    habit: Habit,
    onToggle: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val titleColor by animateColorAsState(if (habit.doneToday) TextMute else TextMain, label = "habitTitle")
    val accent = parseHexColor(habit.colorHex)
    val streakLabel = when {
        habit.currentStreak >= 2 -> "🔥 ${habit.currentStreak} días"
        habit.doneToday -> "Día 1 ✨"
        else -> "Empieza la racha"
    }

    Row(
        modifier = modifier
            .shadow(6.dp, RoundedCornerShape(24.dp), ambientColor = Color(0x14000000), spotColor = Color(0x14000000))
            .clip(RoundedCornerShape(24.dp))
            .background(CreamCard)
            .clickable(onClick = onClick)
            .padding(14.dp)
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(habit.emoji, style = KizenTypography.headlineSmall)
        CuteCheckbox(
            checked = habit.doneToday,
            color = accent,
            onChecked = onToggle,
        )
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                habit.title,
                style = KizenTypography.titleMedium,
                color = titleColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textDecoration = if (habit.doneToday) TextDecoration.LineThrough else null,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                SoftChip(streakLabel, CreamYellow)
                if (habit.longestStreak > habit.currentStreak) {
                    SoftChip("mejor ${habit.longestStreak}", accent)
                }
            }
        }
    }
}
