package com.kizen.tasks.ui.habit

import android.app.TimePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kizen.tasks.domain.model.RepeatDays
import com.kizen.tasks.ui.components.SoftCard
import com.kizen.tasks.ui.theme.CreamBg
import com.kizen.tasks.ui.theme.CreamCard
import com.kizen.tasks.ui.theme.KizenTypography
import com.kizen.tasks.ui.theme.PinkDeep
import com.kizen.tasks.ui.theme.PinkSoft
import com.kizen.tasks.ui.theme.TextMain
import com.kizen.tasks.ui.theme.TextMute
import com.kizen.tasks.ui.theme.parseHexColor
import com.kizen.tasks.ui.util.formatMinutesOfDay
import java.time.DayOfWeek

private val emojis = listOf("💧", "☁️", "🌱", "🍓", "💖", "✨", "📚", "🧘", "🌙", "☀️")
private val colors = listOf("#A8D8EA", "#C7CEEA", "#B5EAD7", "#F8C8DC", "#FFF2CC")

@Composable
fun HabitEditorScreen(
    onBack: () -> Unit,
    viewModel: HabitEditorViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(state.saved, state.deleted) {
        if (state.saved || state.deleted) onBack()
    }

    Scaffold(
        containerColor = CreamBg,
        topBar = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 36.dp, start = 4.dp, end = 8.dp, bottom = 8.dp),
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Volver")
                }
                Text(
                    if (state.isNew) "Nuevo hábito" else "Editar hábito",
                    style = KizenTypography.headlineSmall,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = viewModel::save) {
                    Text("Guardar", color = PinkDeep, style = KizenTypography.titleMedium)
                }
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            OutlinedTextField(
                value = state.title,
                onValueChange = viewModel::onTitle,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Un gesto pequeño cada día") },
                textStyle = KizenTypography.headlineSmall,
                singleLine = true,
                shape = RoundedCornerShape(24.dp),
                colors = fieldColors(),
            )

            SoftCard(modifier = Modifier.fillMaxWidth()) {
                Text("Emoji", style = KizenTypography.titleMedium)
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    emojis.forEach { emoji ->
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(if (state.emoji == emoji) PinkSoft else CreamCard)
                                .clickable { viewModel.onEmoji(emoji) },
                        ) {
                            Text(emoji, style = KizenTypography.titleLarge)
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text("Color", style = KizenTypography.titleMedium)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    colors.forEach { hex ->
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(parseHexColor(hex))
                                .clickable { viewModel.onColor(hex) },
                        )
                    }
                }
            }

            SoftCard(modifier = Modifier.fillMaxWidth()) {
                Text("Se repite", style = KizenTypography.titleMedium)
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    DayOfWeek.entries.forEach { day ->
                        FilterChip(
                            selected = day in state.repeatDays,
                            onClick = { viewModel.toggleDay(day) },
                            label = { Text(RepeatDays.label(day)) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PinkSoft,
                                containerColor = CreamCard,
                            ),
                            border = null,
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.weight(1f)) {
                        Text("Recordatorio local", style = KizenTypography.titleMedium)
                        Text(
                            state.reminderMinutes?.let { formatMinutesOfDay(it) } ?: "Sin hora fija",
                            style = KizenTypography.bodyMedium,
                            color = TextMute,
                        )
                    }
                    Switch(
                        checked = state.reminderMinutes != null,
                        onCheckedChange = { on ->
                            viewModel.onReminder(if (on) 9 * 60 else null)
                        },
                        colors = SwitchDefaults.colors(checkedTrackColor = PinkSoft, checkedThumbColor = PinkDeep),
                    )
                }
                if (state.reminderMinutes != null) {
                    Text(
                        "Toca para elegir hora",
                        style = KizenTypography.bodyMedium,
                        color = PinkDeep,
                        modifier = Modifier
                            .padding(top = 8.dp)
                            .clickable {
                                val minutes = state.reminderMinutes ?: 9 * 60
                                TimePickerDialog(
                                    context,
                                    { _, hour, minute -> viewModel.onReminder(hour * 60 + minute) },
                                    minutes / 60,
                                    minutes % 60,
                                    true,
                                ).show()
                            },
                    )
                }
            }

            SoftCard(modifier = Modifier.fillMaxWidth()) {
                Text("Notas", style = KizenTypography.titleMedium)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = state.notes,
                    onValueChange = viewModel::onNotes,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Para ti, no para exigirte") },
                    minLines = 3,
                    shape = RoundedCornerShape(20.dp),
                    colors = fieldColors(),
                )
            }

            if (!state.isNew) {
                Button(
                    onClick = viewModel::delete,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = PinkSoft, contentColor = TextMain),
                    shape = RoundedCornerShape(22.dp),
                ) {
                    Text("Eliminar hábito")
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = CreamCard,
    unfocusedContainerColor = CreamCard,
    focusedBorderColor = PinkSoft,
    unfocusedBorderColor = PinkSoft.copy(alpha = 0.35f),
)
