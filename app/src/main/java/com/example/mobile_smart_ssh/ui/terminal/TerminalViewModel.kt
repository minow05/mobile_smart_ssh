package com.example.mobile_smart_ssh.ui.terminal

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.mobile_smart_ssh.core.model.TerminalLine
import com.example.mobile_smart_ssh.data.repository.ConfigRepository
import com.example.mobile_smart_ssh.network.ssh.SSHManager
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

class TerminalViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ConfigRepository.getInstance(application)
    private var terminalSession: SSHManager.TerminalSession? = null
    private var currentCommandJob: Job? = null

    private val _uiState = MutableStateFlow(TerminalUiState())
    val uiState: StateFlow<TerminalUiState> = _uiState.asStateFlow()

    init {
        connectToTerminal()
    }

    fun onCommandInputChanged(input: String) {
        _uiState.update { it.copy(currentCommandInput = input) }
    }

    fun connectToTerminal() {
        val config = repository.loadConfig()
        val privateKey = repository.getPrivateKey()

        if (config == null || privateKey.isNullOrBlank()) {
            _uiState.update {
                it.copy(
                    error = "Brak konfiguracji powiązanego hosta lub klucza prywatnego.",
                    lines = it.lines + TerminalLine("Błąd: brak klucza SSH", isError = true)
                )
            }
            return
        }

        val initialPrompt = "${config.username}@${config.host}:~$"
        _uiState.update {
            it.copy(
                isConnecting = true,
                prompt = initialPrompt,
                lines = it.lines + TerminalLine("Łączenie z ${config.host} przez SSH (Ed25519)...")
            )
        }

        viewModelScope.launch {
            terminalSession?.disconnect()
            val session = SSHManager.createTerminalSession(
                host = config.host,
                port = config.port,
                username = config.username,
                privateKeyPem = privateKey
            )
            terminalSession = session

            val connectResult = session.connect()
            connectResult.fold(
                onSuccess = { banner ->
                    val prompt = computePrompt(config.username, config.host, session.currentDirectory)
                    _uiState.update {
                        it.copy(
                            isConnecting = false,
                            isConnected = true,
                            prompt = prompt,
                            lines = it.lines + listOf(
                                TerminalLine("Połączono z ${config.username}@${config.host} ($banner)"),
                                TerminalLine("Wpisz polecenie lub wybierz z listy poniżej.")
                            )
                        )
                    }
                },
                onFailure = { err ->
                    _uiState.update {
                        it.copy(
                            isConnecting = false,
                            isConnected = false,
                            error = err.message,
                            lines = it.lines + TerminalLine("Błąd połączenia SSH: ${err.message}", isError = true)
                        )
                    }
                }
            )
        }
    }

    fun sendCommand(cmd: String = _uiState.value.currentCommandInput) {
        val trimmed = cmd.trim()
        if (trimmed.isEmpty()) return

        if (trimmed == "clear") {
            _uiState.update { it.copy(lines = emptyList(), currentCommandInput = "") }
            return
        }

        val config = repository.loadConfig()
        val session = terminalSession
        if (session == null || !_uiState.value.isConnected) {
            _uiState.update {
                it.copy(
                    lines = it.lines + TerminalLine("Brak połączenia z terminalem. Spróbuj połączyć ponownie.", isError = true)
                )
            }
            return
        }

        val currentPrompt = _uiState.value.prompt
        _uiState.update {
            it.copy(
                currentCommandInput = "",
                isExecuting = true,
                lines = it.lines + TerminalLine(text = "$currentPrompt $trimmed", isCommand = true)
            )
        }

        currentCommandJob = viewModelScope.launch {
            val result = session.executeCommand(trimmed) { line, isError ->
                val cleanLine = stripAnsiCodes(line)
                if (cleanLine.isNotBlank() || !isError) {
                    _uiState.update { state ->
                        state.copy(
                            lines = (state.lines + TerminalLine(cleanLine, isError = isError)).takeLast(1000)
                        )
                    }
                }
            }

            result.fold(
                onSuccess = { exitStatus ->
                    val user = config?.username ?: "minow"
                    val host = config?.host ?: "rpi"
                    val updatedPrompt = computePrompt(user, host, session.currentDirectory)
                    _uiState.update {
                        it.copy(
                            isExecuting = false,
                            prompt = updatedPrompt
                        )
                    }
                },
                onFailure = { err ->
                    _uiState.update {
                        it.copy(
                            isExecuting = false,
                            lines = (it.lines + TerminalLine("Błąd: ${err.message}", isError = true)).takeLast(1000)
                        )
                    }
                }
            )
        }
    }

    fun cancelCurrentCommand() {
        terminalSession?.cancelCurrentCommand()
        currentCommandJob?.cancel()
        _uiState.update {
            it.copy(
                isExecuting = false,
                lines = it.lines + TerminalLine("^C (Zatrzymano)", isError = true)
            )
        }
    }

    private fun computePrompt(user: String, host: String, rawDir: String): String {
        val homeDir = "/home/$user"
        val displayDir = when {
            rawDir == homeDir || rawDir == "~" -> "~"
            rawDir.startsWith("$homeDir/") -> "~" + rawDir.removePrefix(homeDir)
            else -> rawDir
        }
        return "$user@$host:$displayDir$"
    }

    private fun stripAnsiCodes(input: String): String {
        return input
            .replace(Regex("\u001B\\[[;?0-9]*[a-zA-Z]"), "")
            .replace(Regex("\u001B\\][^\u0007\u001B]*(\u0007|\u001B\\\\)"), "")
            .replace(Regex("\u001B[()][A-Za-z0-9]"), "")
            .replace(Regex("\u001B[=>]"), "")
            .replace("\r", "")
    }

    override fun onCleared() {
        super.onCleared()
        terminalSession?.disconnect()
    }
}
