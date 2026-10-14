package com.example.mobile_smart_ssh.ui.dashboard

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.mobile_smart_ssh.core.model.ServerConfig
import com.example.mobile_smart_ssh.data.repository.ConfigRepository
import com.example.mobile_smart_ssh.network.ssh.SSHManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DashboardUiState(
    val config: ServerConfig? = null,
    val isCheckingStatus: Boolean = false,
    val isHostOnline: Boolean? = null,
    val lastPingTimestamp: Long = 0L,
    val quickUptime: String = ""
)

class DashboardViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ConfigRepository.getInstance(application)

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        loadConfigAndCheckStatus()
    }

    fun loadConfigAndCheckStatus() {
        val cfg = repository.loadConfig()
        _uiState.update { it.copy(config = cfg) }

        if (cfg != null) {
            checkHostStatus()
        }
    }

    fun checkHostStatus() {
        val cfg = _uiState.value.config ?: repository.loadConfig() ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isCheckingStatus = true) }
            val online = SSHManager.isHostOnline(cfg.host, cfg.port, timeoutMs = 2500)
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
