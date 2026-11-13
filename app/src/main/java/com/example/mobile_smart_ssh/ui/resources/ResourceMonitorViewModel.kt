package com.example.mobile_smart_ssh.ui.resources

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mobile_smart_ssh.data.repository.ConfigRepository
import com.example.mobile_smart_ssh.network.ssh.SSHSessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel monitora zasobów serwera w architekturze MVI.
 * Korzysta ze wspólnej, trwałej sesji SSHSessionManager (brak renegocjacji kluczy przy każdym wejściu na ekran).
 */
class ResourceMonitorViewModel(
    private val repository: ConfigRepository,
    private val sessionManager: SSHSessionManager
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
            val result = sessionManager.fetchSystemResources()

            result.fold(
                onSuccess = { res ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            resources = res,
                            lastRefreshTime = System.currentTimeMillis(),
                            error = null
                        )
                    }
                },
                onFailure = { err ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = err.message ?: "Nie udało się pobrać danych zasobów"
                        )
                    }
                }
            )
        }
    }
}
