package com.kizen.tasks.data.local

import com.kizen.tasks.data.local.entity.TaskListEntity

object DefaultLists {
    fun seed(now: Long = System.currentTimeMillis()): List<TaskListEntity> = listOf(
        TaskListEntity("list-personal", "Personal", "#C7CEEA", "🌸", now, 1L, null),
        TaskListEntity("list-trabajo", "Trabajo", "#A8D8EA", "☁️", now, 1L, null),
        TaskListEntity("list-compras", "Compras", "#B5EAD7", "🍓", now, 1L, null),
        TaskListEntity("list-cuidado", "Cuidado", "#F8C8DC", "💖", now, 1L, null),
        TaskListEntity("list-ideas", "Ideas", "#FFF2CC", "✨", now, 1L, null),
    )
}
