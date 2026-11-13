package com.example.mobile_smart_ssh.ui.resources

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mobile_smart_ssh.data.repository.ConfigRepository
import com.example.mobile_smart_ssh.network.ssh.SSHManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ResourceMonitorViewModel(
    private val repository: ConfigRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ResourceMonitorUiState())
    val uiState: StateFlow<ResourceMonitorUiState> = _uiState.asStateFlow()

    init {
        refreshResources()
    }

    fun onAction(action: ResourceMonitorAction) {
        when (action) {
            ResourceMonitorAction.Refresh -> refreshResources()
        }
    }

    private fun refreshResources() {
        val config = repository.loadConfig()
        val privateKey = repository.getPrivateKey()

        if (config == null || privateKey.isNullOrBlank()) {
            _uiState.update { it.copy(error = "Brak konfiguracji powiązanego hosta lub klucza") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val result = SSHManager.fetchSystemResources(
                host = config.host,
                port = config.port,
                username = config.username,
                privateKeyPem = privateKey
            )

            result.fold(
                onSuccess = { res ->
                    _uiState.update {
                        it.copy(
                            resources = res,
                            isLoading = false,
                            lastRefreshTime = System.currentTimeMillis()
                        )
                    }
                },
                onFailure = { err ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = "Nie udało się pobrać metryk: ${err.message}"
                        )
                    }
                }
            )
        }
    }
}
