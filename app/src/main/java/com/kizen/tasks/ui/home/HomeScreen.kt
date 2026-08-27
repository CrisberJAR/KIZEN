package com.kizen.tasks.ui.home

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CloudQueue
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kizen.tasks.domain.model.TaskList
import com.kizen.tasks.notification.AlarmPermissions
import com.kizen.tasks.ui.components.CelebrationOverlay
import com.kizen.tasks.ui.components.EmptyState
import com.kizen.tasks.ui.components.HabitCard
import com.kizen.tasks.ui.components.Nubi
import com.kizen.tasks.ui.components.NubiMood
import com.kizen.tasks.ui.components.NudgeCard
import com.kizen.tasks.ui.components.SoftCard
import com.kizen.tasks.ui.components.TaskCard
import com.kizen.tasks.ui.components.WidgetPinRow
import com.kizen.tasks.ui.theme.CreamBg
import com.kizen.tasks.ui.theme.CreamYellow
import com.kizen.tasks.ui.theme.KizenTypography
import com.kizen.tasks.ui.theme.Lavender
import com.kizen.tasks.ui.theme.PinkDeep
import com.kizen.tasks.ui.theme.PinkSoft
import com.kizen.tasks.ui.theme.Sky
import com.kizen.tasks.ui.theme.TextMain
import com.kizen.tasks.ui.theme.TextMute
import com.kizen.tasks.ui.theme.parseHexColor
import com.kizen.tasks.ui.util.greeting

@Composable
fun HomeScreen(
    onOpenList: (String) -> Unit,
    onOpenTask: (String) -> Unit,
    onCreateTask: () -> Unit,
    onManageLists: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenHabit: (String) -> Unit,
    onCreateHabit: () -> Unit,
    onOpenNudge: (String) -> Unit,
    onCreateNudge: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.pullCloud()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(Unit) {
        AlarmPermissions.ensureAlarmChannel(context)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !AlarmPermissions.notificationsAllowed(context)
        ) {
            permission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        AlarmPermissions.requestExactAlarmsOnce(context)
    }

    val empty = state.active.isEmpty() && state.done.isEmpty() && state.habits.isEmpty() && state.nudges.isEmpty()

    Box(Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = CreamBg,
            floatingActionButton = {
                FloatingActionButton(
                    onClick = onCreateTask,
                    containerColor = PinkSoft,
                    contentColor = TextMain,
                    shape = CircleShape,
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = "Nueva tarea")
                }
            },
        ) { padding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(greeting(), style = KizenTypography.bodyMedium, color = TextMute)
                            Text("Hola, vamos despacio", style = KizenTypography.headlineMedium)
                        }
                        Nubi(mood = NubiMood.Happy, size = 72.dp)
                        IconButton(onClick = onOpenSettings) {
                            Icon(Icons.Rounded.CloudQueue, contentDescription = "Nube", tint = TextMain)
                        }
                        IconButton(onClick = onManageLists) {
                            Icon(Icons.Rounded.GridView, contentDescription = "Listas", tint = TextMain)
                        }
                    }
                }
                item {
                    TodayHero(progress = state.progress, done = state.doneCount, total = state.total)
                }
                item {
                    SoftCard(modifier = Modifier.fillMaxWidth()) {
                        Text("En tu escritorio", style = KizenTypography.titleMedium)
                        Text(
                            "Toca y confirma. Así ves hábitos o tareas al desbloquear, sin buscar widgets.",
                            style = KizenTypography.bodyMedium,
                            color = TextMute,
                            modifier = Modifier.padding(top = 4.dp, bottom = 4.dp),
                        )
                        WidgetPinRow()
                    }
                }
                if (state.insight.isNotBlank()) {
                    item {
                        SoftCard(color = Lavender.copy(alpha = 0.45f), modifier = Modifier.fillMaxWidth()) {
                            Text("Nubi mira el día", style = KizenTypography.titleMedium)
                            Text(
                                state.insight,
                                style = KizenTypography.bodyMedium,
                                color = TextMute,
                                modifier = Modifier.padding(top = 6.dp),
                            )
                        }
                    }
                }
                item {
                    Text("Tus rincones", style = KizenTypography.titleMedium)
                    Spacer(Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        state.lists.forEach { list ->
                            ListBubble(list = list, onClick = { onOpenList(list.id) })
                        }
                    }
                }
                item {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Text("Hábitos de hoy", style = KizenTypography.titleMedium, modifier = Modifier.weight(1f))
                        IconButton(onClick = onCreateHabit) {
                            Icon(Icons.Rounded.Add, contentDescription = "Nuevo hábito", tint = PinkDeep)
                        }
                    }
                }
                if (state.habits.isEmpty()) {
                    item {
                        SoftCard(color = Sky.copy(alpha = 0.35f), modifier = Modifier.fillMaxWidth()) {
                            Text("Hoy no hay hábitos en la lista", style = KizenTypography.titleMedium)
                            Text(
                                "Crea uno pequeño. La racha nace del primer día.",
                                style = KizenTypography.bodyMedium,
                                color = TextMute,
                                modifier = Modifier.padding(top = 4.dp),
                            )
                        }
                    }
                } else {
                    items(state.habits, key = { it.id }) { habit ->
                        HabitCard(
                            habit = habit,
                            onBump = { delta ->
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                viewModel.bumpHabit(habit, delta)
                            },
                            onClick = { onOpenHabit(habit.id) },
                        )
                    }
                }
                item {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Text("Avisos de hoy", style = KizenTypography.titleMedium, modifier = Modifier.weight(1f))
                        IconButton(onClick = onCreateNudge) {
                            Icon(Icons.Rounded.Add, contentDescription = "Nuevo aviso", tint = PinkDeep)
                        }
                    }
                }
                if (state.nudges.isEmpty()) {
                    item {
                        SoftCard(color = Lavender.copy(alpha = 0.35f), modifier = Modifier.fillMaxWidth()) {
                            Text("Hoy no hay avisos", style = KizenTypography.titleMedium)
                            Text(
                                "Sirven para algo de este día: te avisan desde una hora, una y otra vez, hasta que lo marques.",
                                style = KizenTypography.bodyMedium,
                                color = TextMute,
                                modifier = Modifier.padding(top = 4.dp),
                            )
                        }
                    }
                } else {
                    items(state.nudges, key = { "nudge-${it.id}" }) { nudge ->
                        NudgeCard(
                            nudge = nudge,
                            onToggle = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                viewModel.toggleNudge(nudge)
                            },
                            onClick = { onOpenNudge(nudge.id) },
                        )
                    }
                }
                if (empty && state.ready) {
                    item {
                        EmptyState(
                            mood = NubiMood.Idle,
                            title = "El día está en blanco",
                            subtitle = "Una tarea o un hábito. Nubi te acompaña sin prisa.",
                        )
                    }
                } else {
                    if (state.active.isNotEmpty()) {
                        item { Text("Para ahora", style = KizenTypography.titleMedium) }
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
                    }
                    if (state.done.isNotEmpty()) {
                        item {
                            Text(
                                "Hecho con cariño ✨",
                                style = KizenTypography.titleMedium,
                                modifier = Modifier.padding(top = 8.dp),
                            )
                        }
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

@Composable
private fun TodayHero(progress: Float, done: Int, total: Int) {
    val animated by animateFloatAsState(targetValue = progress, label = "progress")
    SoftCard(color = Sky.copy(alpha = 0.45f), modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(84.dp)) {
                Canvas(Modifier.size(84.dp)) {
                    val stroke = 10.dp.toPx()
                    drawArc(
                        color = CreamYellow,
                        startAngle = -90f,
                        sweepAngle = 360f,
                        useCenter = false,
                        topLeft = Offset(stroke / 2, stroke / 2),
                        size = Size(this.size.width - stroke, this.size.height - stroke),
                        style = Stroke(stroke, cap = StrokeCap.Round),
                    )
                    drawArc(
                        color = PinkDeep,
                        startAngle = -90f,
                        sweepAngle = 360f * animated,
                        useCenter = false,
                        topLeft = Offset(stroke / 2, stroke / 2),
                        size = Size(this.size.width - stroke, this.size.height - stroke),
                        style = Stroke(stroke, cap = StrokeCap.Round),
                    )
                }
                Text("${(animated * 100).toInt()}%", style = KizenTypography.titleMedium)
            }
            Spacer(Modifier.width(16.dp))
            Column {
                Text("Hoy se siente ligero", style = KizenTypography.titleLarge)
                Text(
                    if (total == 0) "Sin prisa. Un hábito, un aviso o una tarea, cuando quieras."
                    else "$done de $total listos entre hábitos, avisos y tareas",
                    style = KizenTypography.bodyMedium,
                    color = TextMute,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}

@Composable
private fun ListBubble(list: TaskList, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(92.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(parseHexColor(list.colorHex).copy(alpha = 0.7f))
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp, horizontal = 8.dp),
    ) {
        Text(list.emoji, style = KizenTypography.headlineSmall)
        Text(list.name, style = KizenTypography.labelLarge, maxLines = 1)
        Text("${list.doneCount}/${list.taskCount}", style = KizenTypography.bodyMedium, color = TextMute)
    }
}
