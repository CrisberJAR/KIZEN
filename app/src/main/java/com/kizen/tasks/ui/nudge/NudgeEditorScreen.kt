package com.kizen.tasks.ui.nudge

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
import com.kizen.tasks.ui.components.CuteCheckbox
import com.kizen.tasks.ui.components.SoftCard
import com.kizen.tasks.ui.theme.CreamBg
import com.kizen.tasks.ui.theme.CreamCard
import com.kizen.tasks.ui.theme.KizenTypography
import com.kizen.tasks.ui.theme.Lavender
import com.kizen.tasks.ui.theme.Mint
import com.kizen.tasks.ui.theme.PinkDeep
import com.kizen.tasks.ui.theme.PinkSoft
import com.kizen.tasks.ui.theme.Sky
import com.kizen.tasks.ui.theme.TextMain
import com.kizen.tasks.ui.theme.TextMute
import com.kizen.tasks.ui.util.formatTime
import java.util.Calendar

private val intervals = listOf(5, 10, 15, 20, 30, 45, 60)

@Composable
fun NudgeEditorScreen(
    onBack: () -> Unit,
    viewModel: NudgeEditorViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(state.saved, state.deleted) {
        if (state.saved) {
            Toast.makeText(context, "Te avisaré hasta que lo marques", Toast.LENGTH_SHORT).show()
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
                    if (state.isNew) "Aviso de hoy" else "Editar aviso",
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
                placeholder = { Text("Recoger el encargo, llamar…") },
                textStyle = KizenTypography.headlineSmall,
                singleLine = true,
                shape = RoundedCornerShape(24.dp),
                colors = fieldColors(),
            )

            SoftCard(modifier = Modifier.fillMaxWidth()) {
                Text("A partir de qué hora", style = KizenTypography.titleMedium)
                Text(
                    "Solo existe hoy. Te avisa hasta que lo marques hecho, aunque se pase la hora.",
                    style = KizenTypography.bodyMedium,
                    color = TextMute,
                    modifier = Modifier.padding(top = 4.dp, bottom = 8.dp),
                )
                Text(
                    formatTime(state.startAt),
                    style = KizenTypography.bodyLarge,
                    color = TextMain,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Sky.copy(alpha = 0.35f))
                        .clickable {
                            val cal = Calendar.getInstance().apply { timeInMillis = state.startAt }
                            TimePickerDialog(
                                context,
                                { _, hour, minute ->
                                    val picked = Calendar.getInstance().apply {
                                        timeInMillis = System.currentTimeMillis()
                                        set(Calendar.HOUR_OF_DAY, hour)
                                        set(Calendar.MINUTE, minute)
                                        set(Calendar.SECOND, 0)
                                        set(Calendar.MILLISECOND, 0)
                                    }.timeInMillis
                                    viewModel.onStartAt(picked)
                                },
                                cal.get(Calendar.HOUR_OF_DAY),
                                cal.get(Calendar.MINUTE),
                                true,
                            ).show()
                        }
                        .padding(14.dp),
                )
                Spacer(Modifier.height(12.dp))
                Text("Repetir cada", style = KizenTypography.titleMedium)
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    intervals.forEach { minutes ->
                        FilterChip(
                            selected = state.intervalMinutes == minutes,
                            onClick = { viewModel.onInterval(minutes) },
                            label = { Text("$minutes min") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Lavender,
                                containerColor = CreamCard,
                            ),
                            border = null,
                        )
                    }
                }
            }

            SoftCard(modifier = Modifier.fillMaxWidth()) {
                Text("Descripción o nota", style = KizenTypography.titleMedium)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = state.notes,
                    onValueChange = viewModel::onNotes,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Qué llevar, a quién preguntar, un detalle…") },
                    minLines = 3,
                    shape = RoundedCornerShape(20.dp),
                    colors = fieldColors(),
                )
            }

            SoftCard(modifier = Modifier.fillMaxWidth()) {
                Text("Lista dentro", style = KizenTypography.titleMedium)
                Text(
                    "Compras, cosas que adjuntar, pasos de este aviso.",
                    style = KizenTypography.bodyMedium,
                    color = TextMute,
                    modifier = Modifier.padding(top = 4.dp, bottom = 8.dp),
                )
                state.items.forEach { item ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                    ) {
                        CuteCheckbox(
                            checked = item.isDone,
                            color = Mint,
                            onChecked = { viewModel.toggleItem(item) },
                        )
                        Text(
                            item.title,
                            style = KizenTypography.bodyLarge,
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 10.dp),
                        )
                        IconButton(onClick = { viewModel.deleteItem(item.id) }) {
                            Icon(Icons.Rounded.Delete, contentDescription = "Quitar", tint = TextMute)
                        }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = state.draftItem,
                        onValueChange = viewModel::onDraftItem,
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Pan, leche, factura…") },
                        singleLine = true,
                        shape = RoundedCornerShape(22.dp),
                        colors = fieldColors(),
                    )
                    IconButton(
                        onClick = viewModel::addItem,
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .clip(CircleShape)
                            .background(Mint),
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = "Agregar", tint = TextMain)
                    }
                }
            }

            if (!state.isNew) {
                Button(
                    onClick = viewModel::delete,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = PinkSoft, contentColor = TextMain),
                    shape = RoundedCornerShape(22.dp),
                ) {
                    Text("Eliminar aviso")
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
