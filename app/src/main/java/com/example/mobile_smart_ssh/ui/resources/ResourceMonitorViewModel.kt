package com.example.mobile_smart_ssh.ui.resources

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.mobile_smart_ssh.core.model.SystemResources
import com.example.mobile_smart_ssh.data.repository.ConfigRepository
import com.example.mobile_smart_ssh.network.ssh.SSHManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ResourceMonitorUiState(
    val resources: SystemResources = SystemResources(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val lastRefreshTime: Long = 0L
)

class ResourceMonitorViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ConfigRepository.getInstance(application)

    private val _uiState = MutableStateFlow(ResourceMonitorUiState())
    val uiState: StateFlow<ResourceMonitorUiState> = _uiState.asStateFlow()

    init {
        refreshResources()
    }

    fun refreshResources() {
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
