package com.example.mobile_smart_ssh.ui.terminal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mobile_smart_ssh.core.model.TerminalLine
import com.example.mobile_smart_ssh.data.repository.ConfigRepository
import com.example.mobile_smart_ssh.network.ssh.SSHSessionManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TerminalUiState(
    val lines: List<TerminalLine> = emptyList(),
    val isConnecting: Boolean = false,
    val isConnected: Boolean = false,
    val isExecuting: Boolean = false,
    val currentCommandInput: String = "",
    val error: String? = null,
    val prompt: String = "minow@raspberrypi:~$"
)

sealed interface TerminalAction {
    data class UpdateInput(val input: String) : TerminalAction
    data class SendCommand(val command: String? = null) : TerminalAction
    data object CancelCommand : TerminalAction
    data object Connect : TerminalAction
}

class TerminalViewModel(
    private val repository: ConfigRepository,
    private val sessionManager: SSHSessionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(TerminalUiState())
    val uiState: StateFlow<TerminalUiState> = _uiState.asStateFlow()

    private var currentCommandJob: Job? = null

    init {
        // Obserwuj trwały stan sesji z SSHSessionManager (jak w wersji desktopowej)
        viewModelScope.launch {
            sessionManager.state.collect { sessionState ->
                _uiState.update { current ->
                    current.copy(
                        lines = sessionState.lines,
                        isConnecting = sessionState.isConnecting,
                        isConnected = sessionState.isConnected,
                        prompt = sessionState.prompt,
                        error = sessionState.error
                    )
                }
            }
        }
        // Upewnij się, że sesja jest połączona. Jeśli połączyła się na starcie aplikacji, używa jej natychmiast!
        sessionManager.ensureConnected(forceReconnect = false)
    }

    fun onAction(action: TerminalAction) {
        when (action) {
            is TerminalAction.UpdateInput -> onCommandInputChanged(action.input)
            is TerminalAction.SendCommand -> sendCommand(action.command ?: _uiState.value.currentCommandInput)
            TerminalAction.CancelCommand -> cancelCurrentCommand()
            TerminalAction.Connect -> sessionManager.ensureConnected(forceReconnect = true)
        }
    }

    fun onCommandInputChanged(input: String) {
        _uiState.update { it.copy(currentCommandInput = input) }
    }

    fun sendCommand(cmd: String = _uiState.value.currentCommandInput) {
        val trimmed = cmd.trim()
        if (trimmed.isEmpty()) return

        if (trimmed == "clear") {
            sessionManager.clearLines()
            _uiState.update { it.copy(currentCommandInput = "") }
            return
        }

        if (!_uiState.value.isConnected) {
            sessionManager.addLine(TerminalLine("Brak połączenia z terminalem. Spróbuj połączyć ponownie.", isError = true))
            return
        }

        val currentPrompt = _uiState.value.prompt
        _uiState.update {
            it.copy(
                currentCommandInput = "",
                isExecuting = true
            )
        }
        sessionManager.addLine(TerminalLine(text = "$currentPrompt $trimmed", isCommand = true))

        currentCommandJob = viewModelScope.launch {
            val result = sessionManager.executeCommand(trimmed) { line, isError ->
                val cleanLine = TerminalFormatter.stripAnsiCodes(line)
                if (cleanLine.isNotBlank() || !isError) {
                    sessionManager.addLine(TerminalLine(cleanLine, isError = isError))
                }
            }

            result.fold(
                onSuccess = {
                    _uiState.update { it.copy(isExecuting = false) }
                },
                onFailure = { err ->
                    sessionManager.addLine(TerminalLine("Błąd: ${err.message}", isError = true))
                    _uiState.update { it.copy(isExecuting = false) }
                }
            )
        }
    }

    fun cancelCurrentCommand() {
        sessionManager.cancelCurrentCommand()
        currentCommandJob?.cancel()
        _uiState.update { it.copy(isExecuting = false) }
    }

    override fun onCleared() {
        super.onCleared()
        // Trwała sesja SSH pozostaje aktywna w tle w SSHSessionManager (nie rozłączamy jej przy wyjściu z ekranu)
    }
}
