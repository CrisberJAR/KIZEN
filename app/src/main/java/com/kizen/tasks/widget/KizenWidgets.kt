package com.kizen.tasks.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import com.kizen.tasks.MainActivity
import com.kizen.tasks.R
import com.kizen.tasks.domain.repository.DayNudgeRepository
import com.kizen.tasks.domain.repository.HabitRepository
import com.kizen.tasks.domain.repository.TaskRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

object KizenWidgets {
    const val MAX_LINES = 6
    val lineIds = intArrayOf(
        R.id.widget_line_1,
        R.id.widget_line_2,
        R.id.widget_line_3,
        R.id.widget_line_4,
        R.id.widget_line_5,
        R.id.widget_line_6,
    )

    fun entry(context: Context): WidgetEntryPoint =
        EntryPointAccessors.fromApplication(context.applicationContext, WidgetEntryPoint::class.java)

    fun openApp(context: Context, requestCode: Int = 0): PendingIntent =
        PendingIntent.getActivity(
            context,
            requestCode,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    fun fillLines(
        views: RemoteViews,
        lines: List<WidgetLine>,
        context: Context,
        clickBuilder: ((WidgetLine, Int) -> PendingIntent?)? = null,
    ) {
        lineIds.forEachIndexed { index, viewId ->
            val line = lines.getOrNull(index)
            if (line == null) {
                views.setViewVisibility(viewId, View.GONE)
                views.setOnClickPendingIntent(viewId, openApp(context, viewId))
            } else {
                views.setViewVisibility(viewId, View.VISIBLE)
                views.setTextViewText(viewId, line.text)
                views.setOnClickPendingIntent(
                    viewId,
                    clickBuilder?.invoke(line, index) ?: openApp(context, viewId),
                )
            }
        }
    }

    suspend fun pushNow(context: Context) {
        val app = context.applicationContext
        val manager = AppWidgetManager.getInstance(app)
        updateList(app, manager, KizenHabitsWidget::class.java) { habitsViews(app) }
        updateList(app, manager, KizenTasksWidget::class.java) { tasksViews(app) }
        updateList(app, manager, KizenNudgesWidget::class.java) { nudgesViews(app) }
        updateList(app, manager, KizenTodayWidget::class.java) { todayViews(app) }
        manager.getAppWidgetIds(ComponentName(app, KizenOneHabitWidget::class.java)).forEach { id ->
            manager.updateAppWidget(id, oneHabitViews(app, id))
        }
        manager.getAppWidgetIds(ComponentName(app, KizenOneTaskWidget::class.java)).forEach { id ->
            manager.updateAppWidget(id, oneTaskViews(app, id))
        }
    }

    fun refreshAll(context: Context) {
        val app = context.applicationContext
        CoroutineScope(Dispatchers.IO).launch {
            runCatching { pushNow(app) }
        }
    }

    private suspend fun updateList(
        context: Context,
        manager: AppWidgetManager,
        clazz: Class<*>,
        views: suspend () -> RemoteViews,
    ) {
        val ids = manager.getAppWidgetIds(ComponentName(context, clazz))
        if (ids.isEmpty()) return
        manager.updateAppWidget(ids, views())
    }

    private suspend fun todayViews(context: Context): RemoteViews {
        val entry = entry(context)
        val habits = entry.habitRepository().todaySnapshot()
        val tasks = entry.taskRepository().observeToday().first()
        val nudges = entry.dayNudgeRepository().todaySnapshot()
        val views = RemoteViews(context.packageName, R.layout.widget_today)
        views.setTextViewText(R.id.widget_title, context.getString(R.string.widget_today))
        views.setTextViewText(R.id.widget_habits, "Hábitos ${habits.count { it.doneToday }}/${habits.size}")
        views.setTextViewText(
            R.id.widget_tasks,
            if (tasks.isEmpty()) "Sin tareas de hoy" else "Tareas ${tasks.count { it.isDone }}/${tasks.size}",
        )
        val nudgeOpen = nudges.firstOrNull { !it.isDone }?.title
        views.setTextViewText(
            R.id.widget_nudge,
            when {
                nudgeOpen != null -> "Aviso: $nudgeOpen"
                nudges.isEmpty() -> "Sin avisos de hoy"
                else -> "Avisos listos"
            },
        )
        views.setOnClickPendingIntent(R.id.widget_root, openApp(context, 10))
        return views
    }

    private suspend fun habitsViews(context: Context): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_list)
        views.setTextViewText(R.id.widget_title, context.getString(R.string.widget_habits))
        views.setOnClickPendingIntent(R.id.widget_root, openApp(context, 11))
        fillLines(views, habitLines(context), context) { line, index ->
            val habitId = line.id ?: return@fillLines openApp(context, 20 + index)
            PendingIntent.getBroadcast(
                context,
                40 + index,
                Intent(context, KizenHabitsWidget::class.java).apply {
                    action = KizenHabitsWidget.ACTION_BUMP
                    putExtra(KizenHabitsWidget.EXTRA_HABIT_ID, habitId)
                },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        }
        return views
    }

    private suspend fun tasksViews(context: Context): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_list)
        views.setTextViewText(R.id.widget_title, context.getString(R.string.widget_tasks))
        views.setOnClickPendingIntent(R.id.widget_root, openApp(context, 12))
        fillLines(views, taskLines(context), context)
        return views
    }

    private suspend fun nudgesViews(context: Context): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_list)
        views.setTextViewText(R.id.widget_title, context.getString(R.string.widget_nudges))
        views.setOnClickPendingIntent(R.id.widget_root, openApp(context, 13))
        fillLines(views, nudgeLines(context), context)
        return views
    }

    private suspend fun oneHabitViews(context: Context, widgetId: Int): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_one)
        val habitId = WidgetBindings.resolveHabit(context, widgetId)
        val habit = habitId?.let { entry(context).habitRepository().getHabit(it) }
        if (habit == null) {
            views.setTextViewText(R.id.widget_one_title, "Hábito")
            views.setTextViewText(R.id.widget_one_body, "Ábrelo otra vez desde Kizen")
            views.setOnClickPendingIntent(R.id.widget_root, openApp(context, widgetId))
            return views
        }
        val mark = if (habit.doneToday) "hecho hoy ✓" else "${habit.doneCount} de ${habit.goal} · toca para sumar"
        views.setTextViewText(R.id.widget_one_title, "${habit.emoji} ${habit.title}")
        views.setTextViewText(R.id.widget_one_body, mark)
        views.setOnClickPendingIntent(
            R.id.widget_root,
            PendingIntent.getBroadcast(
                context,
                widgetId,
                Intent(context, KizenOneHabitWidget::class.java).apply {
                    action = KizenOneHabitWidget.ACTION_BUMP
                    putExtra(KizenOneHabitWidget.EXTRA_HABIT_ID, habit.id)
                },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            ),
        )
        return views
    }

    private suspend fun oneTaskViews(context: Context, widgetId: Int): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_one)
        val taskId = WidgetBindings.resolveTask(context, widgetId)
        val task = taskId?.let { entry(context).taskRepository().getTask(it) }
        if (task == null) {
            views.setTextViewText(R.id.widget_one_title, "Tarea")
            views.setTextViewText(R.id.widget_one_body, "Ábrela otra vez desde Kizen")
            views.setOnClickPendingIntent(R.id.widget_root, openApp(context, widgetId))
            return views
        }
        views.setTextViewText(R.id.widget_one_title, task.title)
        views.setTextViewText(
            R.id.widget_one_body,
            if (task.isDone) "Hecha ✓ · toca para abrir" else "Toca para marcar hecha",
        )
        val click = if (task.isDone) {
            openApp(context, widgetId)
        } else {
            PendingIntent.getBroadcast(
                context,
                widgetId,
                Intent(context, KizenOneTaskWidget::class.java).apply {
                    action = KizenOneTaskWidget.ACTION_DONE
                    putExtra(KizenOneTaskWidget.EXTRA_TASK_ID, task.id)
                },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        }
        views.setOnClickPendingIntent(R.id.widget_root, click)
        return views
    }

    suspend fun habitLines(context: Context): List<WidgetLine> {
        val habits = entry(context).habitRepository().todaySnapshot()
        if (habits.isEmpty()) return listOf(WidgetLine(text = "Hoy no hay hábitos"))
        return habits.take(MAX_LINES).map { habit ->
            val mark = if (habit.doneToday) "✓" else "${habit.doneCount}/${habit.goal}"
            WidgetLine(text = "${habit.emoji} ${habit.title}  $mark", id = habit.id)
        }
    }

    suspend fun taskLines(context: Context): List<WidgetLine> {
        val tasks = entry(context).taskRepository().observeToday().first().filter { !it.isDone }
        if (tasks.isEmpty()) return listOf(WidgetLine(text = "Sin tareas pendientes de hoy"))
        return tasks.take(MAX_LINES).map { task ->
            val extra = if (task.subtaskTotal > 0) "  lista ${task.subtaskDone}/${task.subtaskTotal}" else ""
            WidgetLine(text = "• ${task.title}$extra", id = task.id)
        }
    }

    suspend fun nudgeLines(context: Context): List<WidgetLine> {
        val nudges = entry(context).dayNudgeRepository().todaySnapshot().filter { !it.isDone }
        if (nudges.isEmpty()) return listOf(WidgetLine(text = "Sin avisos pendientes"))
        return nudges.take(MAX_LINES).map { nudge ->
            WidgetLine(text = "• ${nudge.title}", id = nudge.id)
        }
    }
}

data class WidgetLine(
    val text: String,
    val id: String? = null,
)

@EntryPoint
@InstallIn(SingletonComponent::class)
interface WidgetEntryPoint {
    fun habitRepository(): HabitRepository
    fun taskRepository(): TaskRepository
    fun dayNudgeRepository(): DayNudgeRepository
}
