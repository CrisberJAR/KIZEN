package com.kizen.tasks.ui.task

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
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
import com.kizen.tasks.domain.model.Priority
import com.kizen.tasks.ui.components.CuteCheckbox
import com.kizen.tasks.ui.components.PinWidgetButton
import com.kizen.tasks.ui.components.SoftCard
import com.kizen.tasks.widget.WidgetPin
import com.kizen.tasks.ui.theme.CreamBg
import com.kizen.tasks.ui.theme.CreamCard
import com.kizen.tasks.ui.theme.KizenTypography
import com.kizen.tasks.ui.theme.Mint
import com.kizen.tasks.ui.theme.PinkDeep
import com.kizen.tasks.ui.theme.PinkSoft
import com.kizen.tasks.ui.theme.Sky
import com.kizen.tasks.ui.theme.TextMain
import com.kizen.tasks.ui.theme.TextMute
import com.kizen.tasks.ui.theme.parseHexColor
import com.kizen.tasks.ui.util.formatDateTime
import java.util.Calendar

@Composable
fun TaskEditorScreen(
    onBack: () -> Unit,
    viewModel: TaskEditorViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(state.saved, state.deleted) {
        if (state.saved) {
            if (state.alarmArmed) {
                Toast.makeText(context, "Alarma programada", Toast.LENGTH_SHORT).show()
            }
            onBack()
        } else if (state.deleted) {
            onBack()
        }
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
                    if (state.isNew) "Nueva tarea" else "Editar tarea",
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
                placeholder = { Text("¿Qué hay que hacer?") },
                textStyle = KizenTypography.headlineSmall,
                singleLine = true,
                shape = RoundedCornerShape(24.dp),
                colors = fieldColors(),
            )

            SoftCard(modifier = Modifier.fillMaxWidth()) {
                Text("Rincón", style = KizenTypography.titleMedium)
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    state.lists.forEach { list ->
                        FilterChip(
                            selected = state.listId == list.id,
                            onClick = { viewModel.onList(list.id) },
                            label = { Text("${list.emoji} ${list.name}") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = parseHexColor(list.colorHex),
                                containerColor = parseHexColor(list.colorHex).copy(alpha = 0.35f),
                            ),
                            border = null,
                        )
                    }
                }
            }

            SoftCard(modifier = Modifier.fillMaxWidth()) {
                Text("Prioridad", style = KizenTypography.titleMedium)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Priority.entries.forEach { priority ->
                        FilterChip(
                            selected = state.priority == priority,
                            onClick = { viewModel.onPriority(priority) },
                            label = { Text(priority.label) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = parseHexColor(priority.colorHex),
                                containerColor = parseHexColor(priority.colorHex).copy(alpha = 0.35f),
                            ),
                            border = null,
                        )
                    }
                }
            }

            SoftCard(modifier = Modifier.fillMaxWidth()) {
                Text("Cuándo", style = KizenTypography.titleMedium)
                Spacer(Modifier.height(8.dp))
                Text(
                    text = state.dueAt?.let { formatDateTime(it) } ?: "Sin fecha · tócalo para elegir",
                    style = KizenTypography.bodyLarge,
                    color = if (state.dueAt == null) TextMute else TextMain,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Sky.copy(alpha = 0.35f))
                        .clickable {
                            val cal = Calendar.getInstance().apply {
                                timeInMillis = state.dueAt ?: System.currentTimeMillis()
                            }
                            DatePickerDialog(
                                context,
                                { _, y, m, d ->
                                    TimePickerDialog(
                                        context,
                                        { _, h, min ->
                                            val picked = Calendar.getInstance().apply {
                                                set(y, m, d, h, min, 0)
                                                set(Calendar.MILLISECOND, 0)
                                            }.timeInMillis
                                            viewModel.onDue(picked)
                                        },
                                        cal.get(Calendar.HOUR_OF_DAY),
                                        cal.get(Calendar.MINUTE),
                                        true,
                                    ).show()
                                },
                                cal.get(Calendar.YEAR),
                                cal.get(Calendar.MONTH),
                                cal.get(Calendar.DAY_OF_MONTH),
                            ).show()
                        }
                        .padding(14.dp),
                )
                if (state.dueAt != null) {
                    TextButton(onClick = { viewModel.onDue(null); viewModel.onReminder(null) }) {
                        Text("Quitar fecha", color = TextMute)
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.weight(1f)) {
                        Text("Recordatorio local", style = KizenTypography.titleMedium)
                        Text("Kizen te avisa en el teléfono", style = KizenTypography.bodyMedium, color = TextMute)
                    }
                    Switch(
                        checked = state.reminderAt != null,
                        onCheckedChange = { on ->
                            viewModel.onReminder(
                                if (on) (state.dueAt ?: System.currentTimeMillis() + 60 * 60 * 1000L) else null,
                            )
                        },
                        colors = SwitchDefaults.colors(checkedTrackColor = PinkSoft, checkedThumbColor = PinkDeep),
                    )
                }
            }

            SoftCard(modifier = Modifier.fillMaxWidth()) {
                Text("Descripción o nota", style = KizenTypography.titleMedium)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = state.notes,
                    onValueChange = viewModel::onNotes,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Detalles, enlace, qué llevar a la tienda…") },
                    minLines = 3,
                    shape = RoundedCornerShape(20.dp),
                    colors = fieldColors(),
                )
            }

            SoftCard(modifier = Modifier.fillMaxWidth()) {
                Text("Lista dentro", style = KizenTypography.titleMedium)
                Text(
                    "Compras, cosas que adjuntar o pasos de esta tarea.",
                    style = KizenTypography.bodyMedium,
                    color = TextMute,
                    modifier = Modifier.padding(top = 4.dp, bottom = 8.dp),
                )
                state.subtasks.forEach { subtask ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                    ) {
                        CuteCheckbox(
                            checked = subtask.isDone,
                            color = Mint,
                            onChecked = { viewModel.toggleSubtask(subtask) },
                        )
                        Text(
                            subtask.title,
                            style = KizenTypography.bodyLarge,
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 10.dp),
                        )
                        IconButton(onClick = { viewModel.deleteSubtask(subtask.id) }) {
                            Icon(Icons.Rounded.Delete, contentDescription = "Quitar", tint = TextMute)
                        }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = state.draftSubtask,
                        onValueChange = viewModel::onDraftSubtask,
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Pan, leche, factura del producto…") },
                        singleLine = true,
                        shape = RoundedCornerShape(22.dp),
                        colors = fieldColors(),
                    )
                    IconButton(
                        onClick = viewModel::addSubtask,
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .clip(CircleShape)
                            .background(Mint),
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = "Agregar paso", tint = TextMain)
                    }
                }
            }

            if (!state.isNew) {
                PinWidgetButton(
                    label = "Esta tarea en el escritorio",
                    onPin = { WidgetPin.pinTask(context, state.id) },
                    filled = true,
                )
                Button(
                    onClick = viewModel::delete,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = PinkSoft, contentColor = TextMain),
                    shape = RoundedCornerShape(22.dp),
                ) {
                    Text("Eliminar tarea")
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
