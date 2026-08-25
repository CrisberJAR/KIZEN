package com.kizen.tasks.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.kizen.tasks.domain.repository.HabitRepository
import com.kizen.tasks.domain.repository.TaskRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject lateinit var taskRepository: TaskRepository
    @Inject lateinit var habitRepository: HabitRepository
    @Inject lateinit var reminderScheduler: ReminderScheduler

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                taskRepository.pendingReminders().forEach { reminderScheduler.sync(it) }
                habitRepository.pendingReminders().forEach { reminderScheduler.syncHabit(it) }
            } finally {
                pending.finish()
            }
        }
    }
}
