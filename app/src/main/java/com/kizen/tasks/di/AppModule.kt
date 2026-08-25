package com.kizen.tasks.di

import android.content.Context
import androidx.room.Room
import com.kizen.tasks.data.local.KizenDatabase
import com.kizen.tasks.data.local.dao.HabitDao
import com.kizen.tasks.data.local.dao.HabitLogDao
import com.kizen.tasks.data.local.dao.SubtaskDao
import com.kizen.tasks.data.local.dao.TaskDao
import com.kizen.tasks.data.local.dao.TaskListDao
import com.kizen.tasks.data.repository.HabitRepositoryImpl
import com.kizen.tasks.data.repository.ListRepositoryImpl
import com.kizen.tasks.data.repository.TaskRepositoryImpl
import com.kizen.tasks.domain.repository.HabitRepository
import com.kizen.tasks.domain.repository.ListRepository
import com.kizen.tasks.domain.repository.TaskRepository
import com.kizen.tasks.notification.AlarmReminderScheduler
import com.kizen.tasks.notification.ReminderScheduler
import com.kizen.tasks.sync.HttpSyncPort
import com.kizen.tasks.sync.InsightsPort
import com.kizen.tasks.sync.SmartInsightsPort
import com.kizen.tasks.sync.SyncPort
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun database(@ApplicationContext context: Context): KizenDatabase =
        Room.databaseBuilder(context, KizenDatabase::class.java, "kizen.db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides fun listDao(db: KizenDatabase): TaskListDao = db.listDao()
    @Provides fun taskDao(db: KizenDatabase): TaskDao = db.taskDao()
    @Provides fun subtaskDao(db: KizenDatabase): SubtaskDao = db.subtaskDao()
    @Provides fun habitDao(db: KizenDatabase): HabitDao = db.habitDao()
    @Provides fun habitLogDao(db: KizenDatabase): HabitLogDao = db.habitLogDao()
}

@Module
@InstallIn(SingletonComponent::class)
abstract class AppBindsModule {
    @Binds @Singleton abstract fun lists(impl: ListRepositoryImpl): ListRepository
    @Binds @Singleton abstract fun tasks(impl: TaskRepositoryImpl): TaskRepository
    @Binds @Singleton abstract fun habits(impl: HabitRepositoryImpl): HabitRepository
    @Binds @Singleton abstract fun reminders(impl: AlarmReminderScheduler): ReminderScheduler
    @Binds @Singleton abstract fun sync(impl: HttpSyncPort): SyncPort
    @Binds @Singleton abstract fun insights(impl: SmartInsightsPort): InsightsPort
}
