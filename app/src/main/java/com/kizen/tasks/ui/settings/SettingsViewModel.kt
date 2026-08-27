package com.kizen.tasks.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kizen.tasks.sync.SyncPort
import com.kizen.tasks.sync.SyncSettings
import com.kizen.tasks.work.SyncScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val enabled: Boolean = false,
    val baseUrl: String = SyncSettings.DEFAULT_URL,
    val userId: String = "",
    val busy: Boolean = false,
    val message: String = "",
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settings: SyncSettings,
    private val syncPort: SyncPort,
    private val syncScheduler: SyncScheduler,
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
            _uiState.update { it.copy(busy = true, message = "Sincronizando…") }
            val result = syncPort.sync()
            _uiState.update {
                it.copy(
                    busy = false,
                    message = result.fold(
                        onSuccess = { "Listo. Nubi ya está en la nube." },
                        onFailure = { error -> "No pude conectar: ${error.message ?: "revisa la URL"}" },
                    ),
                )
            }
        }
    }
}
