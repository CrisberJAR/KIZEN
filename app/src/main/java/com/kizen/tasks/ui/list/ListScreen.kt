package com.kizen.tasks.ui.list

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kizen.tasks.ui.components.CelebrationOverlay
import com.kizen.tasks.ui.components.EmptyState
import com.kizen.tasks.ui.components.NubiMood
import com.kizen.tasks.ui.components.TaskCard
import com.kizen.tasks.ui.theme.CreamBg
import com.kizen.tasks.ui.theme.CreamCard
import com.kizen.tasks.ui.theme.KizenTypography
import com.kizen.tasks.ui.theme.PinkSoft
import com.kizen.tasks.ui.theme.TextMain
import com.kizen.tasks.ui.theme.TextMute
import com.kizen.tasks.ui.theme.parseHexColor

@Composable
fun ListScreen(
    onBack: () -> Unit,
    onOpenTask: (String) -> Unit,
    onCreateTask: (String) -> Unit,
    viewModel: ListViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val list = state.list
    val haptic = LocalHapticFeedback.current
    val tint = parseHexColor(list?.colorHex ?: "#C7CEEA").copy(alpha = 0.35f)

    Box(Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = CreamBg,
            topBar = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(tint)
                        .padding(top = 40.dp, bottom = 16.dp, start = 4.dp, end = 16.dp),
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Volver")
                    }
                    Column(Modifier.weight(1f)) {
                        Text("${list?.emoji.orEmpty()} ${list?.name.orEmpty()}", style = KizenTypography.headlineSmall)
                        Text(
                            "${state.done.size} de ${state.active.size + state.done.size} completadas",
                            style = KizenTypography.bodyMedium,
                            color = TextMute,
                        )
                    }
                }
            },
            bottomBar = {
                Row(
                    modifier = Modifier
                        .imePadding()
                        .background(CreamBg)
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedTextField(
                        value = state.draft,
                        onValueChange = viewModel::onDraftChange,
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Añade algo suave…") },
                        singleLine = true,
                        shape = RoundedCornerShape(28.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = CreamCard,
                            unfocusedContainerColor = CreamCard,
                            focusedBorderColor = PinkSoft,
                            unfocusedBorderColor = PinkSoft.copy(alpha = 0.4f),
                        ),
                    )
                    IconButton(
                        onClick = {
                            if (state.draft.isBlank()) onCreateTask(list?.id.orEmpty())
                            else viewModel.quickAdd()
                        },
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(PinkSoft),
                    ) {
                        Icon(Icons.Rounded.ArrowUpward, contentDescription = "Agregar", tint = TextMain)
                    }
                }
            },
        ) { padding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (state.active.isEmpty() && state.done.isEmpty()) {
                    item {
                        EmptyState(
                            mood = NubiMood.Think,
                            title = "Este rincón está quieto",
                            subtitle = "Escribe abajo una idea. Puede ser minúscula. Cuenta igual.",
                        )
                    }
                } else {
                    items(state.active, key = { it.id }) { task ->
                        TaskCard(
                            task = task,
                            onToggle = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                viewModel.toggleDone(task)
                            },
                            onClick = { onOpenTask(task.id) },
                            subtasks = state.subtasksByTask[task.id].orEmpty(),
                            onToggleSubtask = viewModel::toggleSubtask,
                        )
                    }
                    if (state.done.isNotEmpty()) {
                        item { Text("Ya respiran ☁️", style = KizenTypography.titleMedium) }
                        items(state.done, key = { it.id }) { task ->
                            TaskCard(
                                task = task,
                                onToggle = { viewModel.toggleDone(task) },
                                onClick = { onOpenTask(task.id) },
                                subtasks = state.subtasksByTask[task.id].orEmpty(),
                                onToggleSubtask = viewModel::toggleSubtask,
                            )
                        }
                    }
                }
            }
        }
        CelebrationOverlay(
            visible = state.celebration != null,
            message = state.celebration.orEmpty(),
            onFinished = viewModel::clearCelebration,
        )
    }
}
