package com.kizen.tasks.widget

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WidgetRefresher @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun refresh() {
        val app = context.applicationContext
        scope.launch {
            runCatching { KizenWidgets.pushNow(app) }
        }
    }
}
