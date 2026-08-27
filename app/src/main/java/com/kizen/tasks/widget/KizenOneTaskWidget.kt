package com.kizen.tasks.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class KizenOneTaskWidget : AppWidgetProvider() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION_DONE) {
            val taskId = intent.getStringExtra(EXTRA_TASK_ID).orEmpty()
            if (taskId.isBlank()) return
            val pending = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    KizenWidgets.entry(context).taskRepository().setDone(taskId, true)
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
        const val ACTION_DONE = "com.kizen.tasks.widget.DONE_ONE_TASK"
        const val EXTRA_TASK_ID = "task_id"
    }
}
