package com.kizen.tasks.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class KizenTodayWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        paint(context)
    }
}

internal fun AppWidgetProvider.paint(context: Context) {
    val pending = goAsync()
    CoroutineScope(Dispatchers.IO).launch {
        try {
            KizenWidgets.pushNow(context)
        } finally {
            pending.finish()
        }
    }
}
