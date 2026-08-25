package com.kizen.tasks.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kizen.tasks.domain.model.Priority
import com.kizen.tasks.domain.model.Task
import com.kizen.tasks.ui.theme.CreamCard
import com.kizen.tasks.ui.theme.KizenTypography
import com.kizen.tasks.ui.theme.Mint
import com.kizen.tasks.ui.theme.TextMain
import com.kizen.tasks.ui.theme.TextMute
import com.kizen.tasks.ui.theme.parseHexColor
import com.kizen.tasks.ui.util.formatTime

@Composable
fun TaskCard(
    task: Task,
    onToggle: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scale by animateFloatAsState(if (task.isDone) 0.98f else 1f, label = "scale")
    val titleColor by animateColorAsState(if (task.isDone) TextMute else TextMain, label = "title")
    val bar = parseHexColor(task.priority.colorHex)

    Row(
        modifier = modifier
            .scale(scale)
            .shadow(6.dp, RoundedCornerShape(24.dp), ambientColor = Color(0x14000000), spotColor = Color(0x14000000))
            .clip(RoundedCornerShape(24.dp))
            .background(CreamCard)
            .clickable(onClick = onClick)
            .height(88.dp)
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .width(8.dp)
                .fillMaxHeight()
                .background(bar),
        )
        CuteCheckbox(
            checked = task.isDone,
            color = bar,
            onChecked = onToggle,
            modifier = Modifier.padding(start = 12.dp),
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 10.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                task.title,
                style = KizenTypography.titleMedium,
                color = titleColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textDecoration = if (task.isDone) TextDecoration.LineThrough else null,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                SoftChip("${task.listEmoji} ${task.listName}", parseHexColor(task.listColorHex))
                if (task.dueAt != null) {
                    SoftChip(formatTime(task.dueAt), parseHexColor(Priority.MEDIUM.colorHex))
                }
                if (task.subtaskTotal > 0) {
                    SoftChip("${task.subtaskDone}/${task.subtaskTotal}", Mint)
                }
            }
        }
    }
}

@Composable
fun CuteCheckbox(
    checked: Boolean,
    color: Color,
    onChecked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val bg by animateColorAsState(if (checked) color else Color.White, label = "cb")
    Box(
        modifier = modifier
            .size(28.dp)
            .clip(CircleShape)
            .background(bg)
            .clickable(onClick = onChecked)
            .then(Modifier),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(if (checked) Color.Transparent else color.copy(alpha = 0.35f)),
            contentAlignment = Alignment.Center,
        ) {
            if (checked) {
                Icon(Icons.Rounded.Check, contentDescription = "Hecha", tint = TextMain, modifier = Modifier.size(16.dp))
            }
        }
    }
}
