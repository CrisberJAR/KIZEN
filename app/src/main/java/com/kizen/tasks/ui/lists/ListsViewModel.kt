package com.kizen.tasks.ui.lists

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kizen.tasks.domain.model.TaskList
import com.kizen.tasks.domain.repository.ListRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class ListsViewModel @Inject constructor(
    private val listRepository: ListRepository,
) : ViewModel() {

    val lists = listRepository.observeLists()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun add(name: String, emoji: String, colorHex: String) {
        val title = name.trim()
        if (title.isEmpty()) return
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            listRepository.upsert(
                TaskList(
                    id = UUID.randomUUID().toString(),
                    name = title,
                    colorHex = colorHex,
                    emoji = emoji,
                    createdAt = now,
                    updatedAt = now,
                ),
            )
        }
    }

    fun delete(id: String) {
        viewModelScope.launch { listRepository.delete(id) }
    }
}
