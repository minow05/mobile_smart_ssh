package com.example.mobile_smart_ssh.ui.terminal

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.mobile_smart_ssh.core.model.TerminalLine
import com.example.mobile_smart_ssh.data.repository.ConfigRepository
import com.example.mobile_smart_ssh.network.ssh.SSHManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class TerminalUiState(
    val lines: List<TerminalLine> = emptyList(),
    val isConnecting: Boolean = false,
    val isConnected: Boolean = false,
    val currentCommandInput: String = "",
    val error: String? = null,
    val prompt: String = "minow@raspberrypi:~$"
)

class TerminalViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ConfigRepository.getInstance(application)
    private var terminalSession: SSHManager.InteractiveTerminalSession? = null

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

        val prompt = "${config.username}@${config.host}:~$"
        _uiState.update {
            it.copy(
                isConnecting = true,
                prompt = prompt,
                lines = it.lines + TerminalLine("Łączenie z ${config.host}:${config.port} przez SSH (Ed25519)...")
            )
        }

        viewModelScope.launch {
            val result = SSHManager.openInteractiveTerminal(
                host = config.host,
                port = config.port,
                username = config.username,
                privateKeyPem = privateKey
            )

            result.fold(
                onSuccess = { session ->
                    terminalSession = session
                    _uiState.update {
                        it.copy(
                            isConnecting = false,
                            isConnected = true,
                            lines = it.lines + TerminalLine("Połączono z terminalem SSH (Raspberry Pi).")
                        )
                    }

                    // Rozpocznij nasłuchiwanie strumienia wyjściowego terminala
                    viewModelScope.launch(Dispatchers.IO) {
                        session.outputFlow().collect { chunk ->
                            processIncomingChunk(chunk)
                        }
                    }
                },
                onFailure = { err ->
                    _uiState.update {
                        it.copy(
                            isConnecting = false,
                            isConnected = false,
                            error = err.message,
                            lines = it.lines + TerminalLine("Błąd połączenia: ${err.message}", isError = true)
                        )
                    }
                }
            )
        }
    }

    private fun processIncomingChunk(chunk: String) {
        val clean = stripAnsiCodes(chunk).replace("\r\n", "\n").replace("\r", "\n")
        if (clean.isEmpty()) return

        val chunkLines = clean.split("\n")
        viewModelScope.launch(Dispatchers.Main) {
            _uiState.update { current ->
                val currentLines = current.lines.toMutableList()
                for (i in chunkLines.indices) {
                    val lineText = chunkLines[i]
                    if (i == 0) {
                        if (currentLines.isNotEmpty() && !currentLines.last().isCommand) {
                            val last = currentLines.removeAt(currentLines.size - 1)
                            currentLines.add(last.copy(text = last.text + lineText))
                        } else if (lineText.isNotEmpty()) {
                            currentLines.add(TerminalLine(text = lineText))
                        }
                    } else {
                        if (lineText.isNotEmpty() || i < chunkLines.size - 1) {
                            currentLines.add(TerminalLine(text = lineText))
                        }
                    }
                }
                current.copy(lines = currentLines.takeLast(1000))
            }
        }
    }

    fun sendCommand(cmd: String = _uiState.value.currentCommandInput) {
        val trimmed = cmd.trim()
        if (trimmed.isEmpty()) {
            terminalSession?.sendCommand("")
            return
        }

        if (trimmed == "clear") {
            _uiState.update { it.copy(lines = emptyList(), currentCommandInput = "") }
            return
        }

        _uiState.update { it.copy(currentCommandInput = "") }

        val session = terminalSession
        if (session != null && _uiState.value.isConnected) {
            session.sendCommand(trimmed)
        } else {
            _uiState.update {
                it.copy(lines = it.lines + TerminalLine(text = "${it.prompt} $trimmed", isCommand = true))
            }
            executeFallbackCommand(trimmed)
        }
    }

    private fun executeFallbackCommand(command: String) {
        val config = repository.loadConfig() ?: return
        val privateKey = repository.getPrivateKey() ?: return

        viewModelScope.launch {
            val result = SSHManager.executeCommand(
                host = config.host,
                port = config.port,
                username = config.username,
                privateKeyPem = privateKey,
                command = command
            )
            result.fold(
                onSuccess = { output ->
                    val clean = stripAnsiCodes(output).lines().map { TerminalLine(it) }
                    _uiState.update { it.copy(lines = (it.lines + clean).takeLast(1000)) }
                },
                onFailure = { err ->
                    _uiState.update {
                        it.copy(lines = it.lines + TerminalLine("Błąd: ${err.message}", isError = true))
                    }
                }
            )
        }
    }

    private fun stripAnsiCodes(input: String): String {
        return input
            .replace(Regex("\u001B\\[[;?0-9]*[a-zA-Z]"), "")
            .replace(Regex("\u001B\\([a-zA-Z]"), "")
            .replace(Regex("\u001B\\][0-9];[^\u0007]*\u0007"), "")
            .replace(Regex("\u001B[=>]"), "")
    }

    override fun onCleared() {
        super.onCleared()
        terminalSession?.close()
    }
}
