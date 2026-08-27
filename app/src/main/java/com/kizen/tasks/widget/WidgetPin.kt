package com.kizen.tasks.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.RemoteViews
import com.kizen.tasks.R

object WidgetPin {
    const val EXTRA_KIND = "pin_kind"
    const val EXTRA_ENTITY_ID = "pin_entity_id"
    const val KIND_HABIT = "habit"
    const val KIND_TASK = "task"

    fun isSupported(context: Context): Boolean =
        AppWidgetManager.getInstance(context).isRequestPinAppWidgetSupported

    fun pinHabitsList(context: Context): String = pin(context, KizenHabitsWidget::class.java)
    fun pinTasksList(context: Context): String = pin(context, KizenTasksWidget::class.java)
    fun pinNudgesList(context: Context): String = pin(context, KizenNudgesWidget::class.java)
    fun pinToday(context: Context): String = pin(context, KizenTodayWidget::class.java)

    fun pinHabit(context: Context, habitId: String): String {
        WidgetBindings.setPendingHabit(context, habitId)
        return pin(context, KizenOneHabitWidget::class.java, KIND_HABIT, habitId)
    }

    fun pinTask(context: Context, taskId: String): String {
        WidgetBindings.setPendingTask(context, taskId)
        return pin(context, KizenOneTaskWidget::class.java, KIND_TASK, taskId)
    }

    private fun pin(
        context: Context,
        provider: Class<*>,
        kind: String? = null,
        entityId: String? = null,
    ): String {
        val manager = AppWidgetManager.getInstance(context)
        if (!manager.isRequestPinAppWidgetSupported) {
            return "Este teléfono no deja ponerlo desde la app. Mantén pulsado el escritorio → Widgets → Kizen."
        }
        val extras = Bundle().apply {
            putParcelable(AppWidgetManager.EXTRA_APPWIDGET_PREVIEW, preview(context, provider))
        }
        val callback = PendingIntent.getBroadcast(
            context,
            (provider.name + (entityId ?: "")).hashCode(),
            Intent(context, WidgetPinnedReceiver::class.java).apply {
                putExtra(EXTRA_KIND, kind)
                putExtra(EXTRA_ENTITY_ID, entityId)
            },
            pendingFlags(),
        )
        val ok = manager.requestPinAppWidget(ComponentName(context, provider), extras, callback)
        return if (ok) {
            "Confirma en la ventana para dejarlo en el escritorio."
        } else {
            "No se pudo pedir el widget. Prueba desde el escritorio: Widgets → Kizen."
        }
    }

    private fun preview(context: Context, provider: Class<*>): RemoteViews {
        val layout = when (provider) {
            KizenTodayWidget::class.java -> R.layout.widget_today
            KizenOneHabitWidget::class.java, KizenOneTaskWidget::class.java -> R.layout.widget_one
            else -> R.layout.widget_list
        }
        return RemoteViews(context.packageName, layout)
    }

    private fun pendingFlags(): Int {
        var flags = PendingIntent.FLAG_UPDATE_CURRENT
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            flags = flags or PendingIntent.FLAG_MUTABLE
        }
        return flags
    }
}
