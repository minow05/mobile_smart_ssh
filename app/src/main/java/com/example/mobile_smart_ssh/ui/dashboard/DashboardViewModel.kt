package com.example.mobile_smart_ssh.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mobile_smart_ssh.data.repository.ConfigRepository
import com.example.mobile_smart_ssh.network.ssh.SSHManager
import com.example.mobile_smart_ssh.network.ssh.SSHSessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class DashboardViewModel(
    private val repository: ConfigRepository,
    private val sessionManager: SSHSessionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        loadConfigAndCheckStatus()
        viewModelScope.launch {
            sessionManager.state.collect { sessionState ->
                if (sessionState.isConnected) {
                    _uiState.update { it.copy(isHostOnline = true, isCheckingStatus = false) }
                }
            }
        }
    }

    fun onAction(action: DashboardAction) {
        when (action) {
            DashboardAction.Refresh -> loadConfigAndCheckStatus()
            DashboardAction.CheckStatus -> checkHostStatus()
        }
    }

    private fun loadConfigAndCheckStatus() {
        val cfg = repository.loadConfig()
        _uiState.update { it.copy(config = cfg) }

        if (cfg != null) {
            // Nawiązanie trwałej sesji SSH w tle już na starcie aplikacji (jak w wersji desktopowej)
            sessionManager.ensureConnected(forceReconnect = false)
            checkHostStatus()
        }
    }

    private fun checkHostStatus() {
        val cfg = _uiState.value.config ?: repository.loadConfig() ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isCheckingStatus = true) }
            val online = if (sessionManager.isConnected()) {
                true
            } else {
                SSHManager.isHostOnline(cfg.host, cfg.port, timeoutMs = 2500)
            }
            _uiState.update {
                it.copy(
                    isCheckingStatus = false,
                    isHostOnline = online,
                    lastPingTimestamp = System.currentTimeMillis()
                )
            }
        }
    }
}
