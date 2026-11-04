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

/**
 * ViewModel terminala SSH w architekturze MVI wzorowany na Termux:
 * obsługa bufora linii, klawiszy funkcyjnych CTRL/ALT/ESC/TAB, historii komend powłoki
 * oraz natychmiastowej sygnalizacji procesów (^C, ^L).
 */
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

    /**
     * Jedyny publiczny punkt wejścia w architekturze MVI.
     */
    fun onAction(action: TerminalAction) {
        when (action) {
            is TerminalAction.UpdateInput -> onCommandInputChanged(action.input)
            is TerminalAction.SendCommand -> sendCommand(action.command ?: _uiState.value.currentCommandInput)
            TerminalAction.CancelCommand -> cancelCurrentCommand()
            TerminalAction.Connect -> sessionManager.ensureConnected(forceReconnect = true)
            TerminalAction.HistoryUp -> onHistoryUp()
            TerminalAction.HistoryDown -> onHistoryDown()
            is TerminalAction.InsertKey -> onInsertKey(action.keyText)
            TerminalAction.ToggleCtrl -> onToggleCtrl()
            TerminalAction.ToggleAlt -> onToggleAlt()
            TerminalAction.TabComplete -> onTabComplete()
            TerminalAction.ClearScreen -> onClearScreen()
            TerminalAction.Escape -> onEscape()
        }
    }

    private fun onCommandInputChanged(input: String) {
        val oldInput = _uiState.value.currentCommandInput
        if (_uiState.value.isCtrlActive && input.length > oldInput.length) {
            val inserted = input.substring(oldInput.length)
            if (inserted.equals("c", ignoreCase = true)) {
                cancelCurrentCommand()
                return
            } else if (inserted.equals("l", ignoreCase = true)) {
                onClearScreen()
                _uiState.update { it.copy(isCtrlActive = false) }
                return
            }
            _uiState.update { it.copy(currentCommandInput = input, isCtrlActive = false) }
            return
        }
        _uiState.update { it.copy(currentCommandInput = input) }
    }

    private fun onToggleCtrl() {
        _uiState.update { it.copy(isCtrlActive = !it.isCtrlActive) }
    }

    private fun onToggleAlt() {
        _uiState.update { it.copy(isAltActive = !it.isAltActive) }
    }

    private fun onEscape() {
        _uiState.update {
            it.copy(
                currentCommandInput = "",
                isCtrlActive = false,
                isAltActive = false,
                historyIndex = -1
            )
        }
    }

    private fun onClearScreen() {
        sessionManager.clearLines()
        _uiState.update {
            it.copy(
                currentCommandInput = "",
                historyIndex = -1,
                isCtrlActive = false
            )
        }
    }

    private fun onHistoryUp() {
        val history = _uiState.value.commandHistory
        if (history.isEmpty()) return

        val currentIndex = _uiState.value.historyIndex
        val targetIndex = if (currentIndex == -1) {
            history.size - 1
        } else {
            (currentIndex - 1).coerceAtLeast(0)
        }

        _uiState.update {
            it.copy(
                historyIndex = targetIndex,
                currentCommandInput = history[targetIndex]
            )
        }
    }

    private fun onHistoryDown() {
        val history = _uiState.value.commandHistory
        if (history.isEmpty()) return

        val currentIndex = _uiState.value.historyIndex
        if (currentIndex == -1) return

        if (currentIndex < history.size - 1) {
            val targetIndex = currentIndex + 1
            _uiState.update {
                it.copy(
                    historyIndex = targetIndex,
                    currentCommandInput = history[targetIndex]
                )
            }
        } else {
            // Przejście poza najnowszą historię czyści input do nowej komendy
            _uiState.update {
                it.copy(
                    historyIndex = -1,
                    currentCommandInput = ""
                )
            }
        }
    }

    private fun onInsertKey(keyText: String) {
        if (_uiState.value.isCtrlActive) {
            when {
                keyText.equals("c", ignoreCase = true) -> {
                    cancelCurrentCommand()
                    return
                }
                keyText.equals("l", ignoreCase = true) -> {
                    onClearScreen()
                    _uiState.update { it.copy(isCtrlActive = false) }
                    return
                }
                keyText.equals("d", ignoreCase = true) -> {
                    _uiState.update { it.copy(isCtrlActive = false) }
                    return
                }
                else -> {
                    _uiState.update {
                        it.copy(
                            isCtrlActive = false,
                            currentCommandInput = it.currentCommandInput + keyText
                        )
                    }
                    return
                }
            }
        }

        _uiState.update {
            it.copy(currentCommandInput = it.currentCommandInput + keyText)
        }
    }

    private fun onTabComplete() {
        val input = _uiState.value.currentCommandInput
        if (input.isBlank()) {
            _uiState.update { it.copy(currentCommandInput = it.currentCommandInput + "  ") }
            return
        }

        val trimmed = input.trim()
        val matches = COMMON_COMMANDS.filter { it.startsWith(trimmed, ignoreCase = true) }

        when {
            matches.size == 1 -> {
                _uiState.update { it.copy(currentCommandInput = "${matches.first()} ") }
            }
            matches.size > 1 -> {
                // Jeśli jest kilka dopasowań, pokaż je na ekranie terminala
                val prompt = _uiState.value.prompt
                sessionManager.addLine(TerminalLine("$prompt $trimmed", isCommand = true))
                sessionManager.addLine(TerminalLine(matches.joinToString("   "), isError = false))
                // Znajdź najdłuższy wspólny prefiks
                val commonPrefix = matches.reduce { acc, s -> acc.commonPrefixWith(s) }
                if (commonPrefix.length > trimmed.length) {
                    _uiState.update { it.copy(currentCommandInput = commonPrefix) }
                }
            }
            else -> {
                // Brak dopasowania
            }
        }
    }

    private fun sendCommand(cmd: String = _uiState.value.currentCommandInput) {
        val trimmed = cmd.trim()
        if (trimmed.isEmpty()) return

        if (trimmed == "clear") {
            onClearScreen()
            return
        }

        // Zapisz komendę do historii (unikając dublowania z ostatnią)
        val history = _uiState.value.commandHistory.toMutableList()
        if (history.isEmpty() || history.last() != trimmed) {
            history.add(trimmed)
        }

        if (!_uiState.value.isConnected) {
            sessionManager.addLine(TerminalLine("Brak połączenia z terminalem. Spróbuj połączyć ponownie.", isError = true))
            _uiState.update { it.copy(commandHistory = history, historyIndex = -1) }
            return
        }

        val currentPrompt = _uiState.value.prompt
        _uiState.update {
            it.copy(
                currentCommandInput = "",
                commandHistory = history,
                historyIndex = -1,
                isExecuting = true,
                isCtrlActive = false,
                isAltActive = false
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

    private fun cancelCurrentCommand() {
        sessionManager.cancelCurrentCommand()
        currentCommandJob?.cancel()
        sessionManager.addLine(TerminalLine("^C", isError = true))
        _uiState.update { it.copy(isExecuting = false, isCtrlActive = false) }
    }

    override fun onCleared() {
        super.onCleared()
    }

    companion object {
        private val COMMON_COMMANDS = listOf(
            "cat", "cd", "chmod", "chown", "clear", "cp", "curl", "df", "df -h",
            "docker", "echo", "exit", "free", "free -h", "git", "git status",
            "grep", "head", "history", "htop", "journalctl", "kill", "ls", "ls -la",
            "mkdir", "mv", "nano", "netstat", "ping", "ps", "ps aux", "pwd",
            "quota", "rm", "rm -rf", "scp", "ssh", "sudo", "systemctl", "tail",
            "tar", "top", "touch", "uname -a", "uptime", "vim", "wget", "whoami"
        )
    }
}
