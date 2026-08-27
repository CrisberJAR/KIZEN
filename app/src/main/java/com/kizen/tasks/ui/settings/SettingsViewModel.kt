package com.kizen.tasks.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kizen.tasks.domain.repository.DayNudgeRepository
import com.kizen.tasks.sync.SyncPort
import com.kizen.tasks.sync.SyncSettings
import com.kizen.tasks.work.SyncScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class SettingsUiState(
    val enabled: Boolean = true,
    val baseUrl: String = SyncSettings.CLOUD_URL,
    val userId: String = "",
    val busy: Boolean = false,
    val message: String = "",
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settings: SyncSettings,
    private val syncPort: SyncPort,
    private val syncScheduler: SyncScheduler,
    private val nudgeRepository: DayNudgeRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        SettingsUiState(
            enabled = settings.isEnabled,
            baseUrl = settings.baseUrl,
            userId = settings.userId,
        ),
    )
    val uiState: StateFlow<SettingsUiState> = _uiState

    fun onEnabled(value: Boolean) {
        settings.isEnabled = value
        _uiState.update { it.copy(enabled = value, message = "") }
        if (value) syncScheduler.ensureScheduled()
    }

    fun onUrl(value: String) {
        settings.baseUrl = value
        _uiState.update { it.copy(baseUrl = value) }
    }

    fun onUserId(value: String) {
        val clean = value.filter { it.isLetterOrDigit() || it == '-' || it == '_' }.take(48)
        _uiState.update { it.copy(userId = clean) }
        if (clean.isNotBlank()) settings.userId = clean
    }

    fun syncNow() {
        viewModelScope.launch {
            _uiState.update { it.copy(busy = true, message = "Sincronizando… Render a veces tarda un minuto.") }
            val result = withContext(Dispatchers.IO) { syncPort.sync() }
            val nudges = runCatching { nudgeRepository.todaySnapshot().size }.getOrDefault(0)
            _uiState.update {
                it.copy(
                    busy = false,
                    message = result.fold(
                        onSuccess = { "Listo. Este teléfono tiene $nudges avisos de hoy. Abre Inicio en el otro y pulsa Sincronizar ahora." },
                        onFailure = { error -> "No pude conectar: ${error.message ?: "espera un minuto y reintenta"}" },
                    ),
                )
            }
        }
    }
}
