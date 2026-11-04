package com.example.mobile_smart_ssh.ui.settings

import androidx.lifecycle.ViewModel
import com.example.mobile_smart_ssh.core.security.KeyManager
import com.example.mobile_smart_ssh.data.repository.ConfigRepository
import com.example.mobile_smart_ssh.monitor.ServerMonitor
import com.example.mobile_smart_ssh.network.ssh.SSHSessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class SettingsViewModel(
    private val repository: ConfigRepository,
    private val serverMonitor: ServerMonitor,
    private val sessionManager: SSHSessionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadSettings()
    }

    fun onAction(action: SettingsAction) {
        when (action) {
            SettingsAction.LoadSettings -> loadSettings()
            is SettingsAction.UpdateSmbPassword -> onSmbPasswordChanged(action.password)
            is SettingsAction.ToggleMonitor -> onMonitorEnabledChanged(action.enabled)
            is SettingsAction.SetResetDialogOpen -> setResetDialogOpen(action.open)
            is SettingsAction.ResetConfiguration -> resetConfiguration(action.onResetComplete)
            SettingsAction.SendTestNotification -> {
                val sent = serverMonitor.sendTestNotification()
                val msg = if (sent) {
                    "Wysłano powiadomienie testowe! Sprawdź górny pasek systemowy."
                } else {
                    "Brak uprawnienia do wyświetlania powiadomień w systemie Android."
                }
                _uiState.update {
                    it.copy(
                        statusMessage = msg,
                        hasNotificationPermission = serverMonitor.hasNotificationPermission()
                    )
                }
            }
            SettingsAction.TriggerImmediateCheck -> {
                serverMonitor.triggerImmediateCheck()
                _uiState.update { it.copy(statusMessage = "Zlecono natychmiastowe sprawdzenie serwera w tle (WorkManager).") }
            }
            SettingsAction.ClearStatusMessage -> {
                _uiState.update { it.copy(statusMessage = null) }
            }
        }
    }

    private fun loadSettings() {
        val cfg = repository.loadConfig()
        val pubKey = repository.getPublicKey() ?: ""
        val fp = if (pubKey.isNotBlank()) KeyManager.calculateFingerprint(pubKey) else "Brak klucza"

        _uiState.update {
            it.copy(
                config = cfg,
                publicKey = pubKey,
                fingerprint = fp,
                smbPasswordInput = cfg?.smbPassword ?: "",
                monitorEnabled = serverMonitor.isEnabled(),
                hasNotificationPermission = serverMonitor.hasNotificationPermission()
            )
        }
    }

    private fun onSmbPasswordChanged(pwd: String) {
        _uiState.update { it.copy(smbPasswordInput = pwd) }
        repository.saveSmbPassword(pwd)
    }

    private fun onMonitorEnabledChanged(enabled: Boolean) {
        serverMonitor.setEnabled(enabled)
        _uiState.update {
            it.copy(
                monitorEnabled = enabled,
                hasNotificationPermission = serverMonitor.hasNotificationPermission()
            )
        }
    }

    private fun setResetDialogOpen(open: Boolean) {
        _uiState.update { it.copy(isResetDialogOpen = open) }
    }

    /**
     * Czyści lokalną konfigurację telefonu i wywołuje akcję powrotu do kreatora.
     * Zgodnie z wymaganiami zadania, dane na Raspberry Pi pozostają NIETKNIĘTE.
     */
    private fun resetConfiguration(onResetComplete: () -> Unit) {
        sessionManager.disconnect()
        repository.resetConfiguration()
        _uiState.update { it.copy(isResetDialogOpen = false) }
        onResetComplete()
    }
}
