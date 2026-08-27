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
import com.kizen.tasks.domain.model.DayNudge
import com.kizen.tasks.ui.theme.CreamCard
import com.kizen.tasks.ui.theme.CreamYellow
import com.kizen.tasks.ui.theme.KizenTypography
import com.kizen.tasks.ui.theme.Lavender
import com.kizen.tasks.ui.theme.Mint
import com.kizen.tasks.ui.theme.TextMain
import com.kizen.tasks.ui.theme.TextMute
import com.kizen.tasks.ui.util.formatTime

@Composable
fun NudgeCard(
    nudge: DayNudge,
    onToggle: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val titleColor by animateColorAsState(if (nudge.isDone) TextMute else TextMain, label = "nudgeTitle")
    val doneItems = nudge.items.count { it.isDone }

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
        CuteCheckbox(
            checked = nudge.isDone,
            color = Lavender,
            onChecked = onToggle,
        )
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                nudge.title,
                style = KizenTypography.titleMedium,
                color = titleColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textDecoration = if (nudge.isDone) TextDecoration.LineThrough else null,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                SoftChip("desde ${formatTime(nudge.startAt)}", CreamYellow)
                SoftChip("cada ${nudge.intervalMinutes} min", Lavender)
                if (nudge.items.isNotEmpty()) {
                    SoftChip("$doneItems/${nudge.items.size}", Mint)
                }
            }
        }
    }
}
