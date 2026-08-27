package com.kizen.tasks.widget

import android.content.Context

object WidgetBindings {
    private const val PREFS = "kizen_widget_bindings"
    private const val KIND_HABIT = "habit"
    private const val KIND_TASK = "task"
    private const val PENDING_HABIT = "pending_habit"
    private const val PENDING_TASK = "pending_task"

    fun setPendingHabit(context: Context, habitId: String) {
        prefs(context).edit().putString(PENDING_HABIT, habitId).apply()
    }

    fun setPendingTask(context: Context, taskId: String) {
        prefs(context).edit().putString(PENDING_TASK, taskId).apply()
    }

    fun resolveHabit(context: Context, widgetId: Int): String? {
        habitId(context, widgetId)?.let { return it }
        val pending = prefs(context).getString(PENDING_HABIT, null) ?: return null
        bindHabit(context, widgetId, pending)
        prefs(context).edit().remove(PENDING_HABIT).apply()
        return pending
    }

    fun resolveTask(context: Context, widgetId: Int): String? {
        taskId(context, widgetId)?.let { return it }
        val pending = prefs(context).getString(PENDING_TASK, null) ?: return null
        bindTask(context, widgetId, pending)
        prefs(context).edit().remove(PENDING_TASK).apply()
        return pending
    }

    fun bindHabit(context: Context, widgetId: Int, habitId: String) {
        prefs(context).edit().putString(key(KIND_HABIT, widgetId), habitId).apply()
    }

    fun bindTask(context: Context, widgetId: Int, taskId: String) {
        prefs(context).edit().putString(key(KIND_TASK, widgetId), taskId).apply()
    }

    fun habitId(context: Context, widgetId: Int): String? =
        prefs(context).getString(key(KIND_HABIT, widgetId), null)

    fun taskId(context: Context, widgetId: Int): String? =
        prefs(context).getString(key(KIND_TASK, widgetId), null)

    fun unbind(context: Context, widgetIds: IntArray) {
        val editor = prefs(context).edit()
        widgetIds.forEach { id ->
            editor.remove(key(KIND_HABIT, id))
            editor.remove(key(KIND_TASK, id))
        }
        editor.apply()
    }

    private fun key(kind: String, widgetId: Int) = "${kind}_$widgetId"
    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
