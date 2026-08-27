package com.kizen.tasks.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.Icon
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
import com.kizen.tasks.ui.theme.PinkDeep
import com.kizen.tasks.ui.theme.PinkSoft
import com.kizen.tasks.ui.theme.TextMain
import com.kizen.tasks.ui.theme.TextMute
import com.kizen.tasks.ui.theme.parseHexColor

@Composable
fun HabitCard(
    habit: Habit,
    onBump: (Int) -> Unit,
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
        if (habit.goal == 1) {
            CuteCheckbox(
                checked = habit.doneToday,
                color = accent,
                onChecked = { onBump(if (habit.doneToday) -1 else 1) },
            )
        } else {
            HabitStepper(
                count = habit.doneCount,
                goal = habit.goal,
                color = accent,
                onBump = onBump,
            )
        }
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
                if (habit.goal > 1) {
                    SoftChip("${habit.doneCount}/${habit.goal} hoy", accent)
                }
                SoftChip(streakLabel, CreamYellow)
                if (habit.longestStreak > habit.currentStreak) {
                    SoftChip("mejor ${habit.longestStreak}", accent)
                }
            }
        }
    }
}

@Composable
fun HabitStepper(
    count: Int,
    goal: Int,
    color: Color,
    onBump: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        StepperButton(enabled = count > 0, onClick = { onBump(-1) }) {
            Icon(Icons.Rounded.Remove, contentDescription = "Quitar una", tint = TextMain, modifier = Modifier.size(16.dp))
        }
        Text("$count", style = KizenTypography.titleMedium, color = PinkDeep)
        StepperButton(enabled = count < goal, onClick = { onBump(1) }, filled = true, color = color) {
            Icon(Icons.Rounded.Add, contentDescription = "Sumar una", tint = TextMain, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
fun StepperButton(
    enabled: Boolean,
    onClick: () -> Unit,
    filled: Boolean = false,
    color: Color = PinkSoft,
    content: @Composable () -> Unit,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(28.dp)
            .clip(CircleShape)
            .background(if (filled) color else color.copy(alpha = 0.35f))
            .clickable(enabled = enabled, onClick = onClick),
    ) {
        content()
    }
}
