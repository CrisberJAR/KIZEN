package com.kizen.tasks.widget

import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class WidgetPinnedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val widgetId = intent.getIntExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID,
        )
        if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            KizenWidgets.refreshAll(context)
            return
        }
        val entityId = intent.getStringExtra(WidgetPin.EXTRA_ENTITY_ID).orEmpty()
        when (intent.getStringExtra(WidgetPin.EXTRA_KIND)) {
            WidgetPin.KIND_HABIT -> if (entityId.isNotBlank()) WidgetBindings.bindHabit(context, widgetId, entityId)
            WidgetPin.KIND_TASK -> if (entityId.isNotBlank()) WidgetBindings.bindTask(context, widgetId, entityId)
        }
        KizenWidgets.refreshAll(context)
    }
}
