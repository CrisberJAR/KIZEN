package com.kizen.tasks.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.kizen.tasks.data.local.dao.HabitDao
import com.kizen.tasks.data.local.dao.HabitLogDao
import com.kizen.tasks.data.local.dao.SubtaskDao
import com.kizen.tasks.data.local.dao.TaskDao
import com.kizen.tasks.data.local.dao.TaskListDao
import com.kizen.tasks.data.local.entity.HabitEntity
import com.kizen.tasks.data.local.entity.HabitLogEntity
import com.kizen.tasks.data.local.entity.SubtaskEntity
import com.kizen.tasks.data.local.entity.TaskEntity
import com.kizen.tasks.data.local.entity.TaskListEntity

@Database(
    entities = [
        TaskListEntity::class,
        TaskEntity::class,
        SubtaskEntity::class,
        HabitEntity::class,
        HabitLogEntity::class,
    ],
    version = 2,
    exportSchema = false,
)
abstract class KizenDatabase : RoomDatabase() {
    abstract fun listDao(): TaskListDao
    abstract fun taskDao(): TaskDao
    abstract fun subtaskDao(): SubtaskDao
    abstract fun habitDao(): HabitDao
    abstract fun habitLogDao(): HabitLogDao
}
