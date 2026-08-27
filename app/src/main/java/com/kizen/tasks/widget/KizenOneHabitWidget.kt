package com.kizen.tasks.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class KizenOneHabitWidget : AppWidgetProvider() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION_BUMP) {
            val habitId = intent.getStringExtra(EXTRA_HABIT_ID).orEmpty()
            if (habitId.isBlank()) return
            val pending = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    KizenWidgets.entry(context).habitRepository().bumpToday(habitId, 1)
                } finally {
                    pending.finish()
                }
            }
            return
        }
        super.onReceive(context, intent)
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        paint(context)
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        WidgetBindings.unbind(context, appWidgetIds)
    }

    companion object {
        const val ACTION_BUMP = "com.kizen.tasks.widget.BUMP_ONE_HABIT"
        const val EXTRA_HABIT_ID = "habit_id"
    }
}
