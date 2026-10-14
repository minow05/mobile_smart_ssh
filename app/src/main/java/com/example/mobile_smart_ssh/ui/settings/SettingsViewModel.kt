package com.example.mobile_smart_ssh.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.example.mobile_smart_ssh.core.model.ServerConfig
import com.example.mobile_smart_ssh.core.security.KeyManager
import com.example.mobile_smart_ssh.data.repository.ConfigRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class SettingsUiState(
    val config: ServerConfig? = null,
    val publicKey: String = "",
    val fingerprint: String = "",
    val isResetDialogOpen: Boolean = false,
    val smbPasswordInput: String = ""
)

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ConfigRepository.getInstance(application)

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadSettings()
    }

    fun loadSettings() {
        val cfg = repository.loadConfig()
        val pubKey = repository.getPublicKey() ?: ""
        val fp = if (pubKey.isNotBlank()) KeyManager.calculateFingerprint(pubKey) else "Brak klucza"

        _uiState.update {
            it.copy(
                config = cfg,
                publicKey = pubKey,
                fingerprint = fp,
                smbPasswordInput = cfg?.smbPassword ?: ""
            )
        }
    }

    fun onSmbPasswordChanged(pwd: String) {
        _uiState.update { it.copy(smbPasswordInput = pwd) }
        repository.saveSmbPassword(pwd)
    }

    fun setResetDialogOpen(open: Boolean) {
        _uiState.update { it.copy(isResetDialogOpen = open) }
    }

    /**
     * Czyści lokalną konfigurację telefonu i wywołuje akcję powrotu do kreatora.
     * Zgodnie z wymaganiami zadania, dane na Raspberry Pi pozostają NIETKNIĘTE.
     */
    fun resetConfiguration(onResetComplete: () -> Unit) {
        repository.resetConfiguration()
        _uiState.update { it.copy(isResetDialogOpen = false) }
        onResetComplete()
    }
}
