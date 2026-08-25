package com.kizen.tasks.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.kizen.tasks.domain.repository.HabitRepository
import com.kizen.tasks.domain.repository.TaskRepository
import com.kizen.tasks.notification.ReminderScheduler
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

@HiltWorker
class DailyMaintenanceWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val habitRepository: HabitRepository,
    private val taskRepository: TaskRepository,
    private val reminderScheduler: ReminderScheduler,
    private val maintenanceScheduler: MaintenanceScheduler,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        habitRepository.recalculateStreaks()
        val monthAgo = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(30)
        taskRepository.pruneCompletedBefore(monthAgo)
        habitRepository.pendingReminders().forEach { reminderScheduler.syncHabit(it) }
        maintenanceScheduler.enqueueNext()
        return Result.success()
    }
}
