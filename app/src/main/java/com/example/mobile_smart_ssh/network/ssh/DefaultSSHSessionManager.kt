package com.example.mobile_smart_ssh.network.ssh

import android.util.Log
import com.example.mobile_smart_ssh.core.model.TerminalLine
import com.example.mobile_smart_ssh.data.repository.ConfigRepository
import com.example.mobile_smart_ssh.ui.terminal.TerminalFormatter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Domyślna implementacja SSHSessionManager zarządzająca pojedynczą, trwałą sesją SSH
 * przez cały cykl życia aplikacji (odpowiednik active_session m_session z wersji desktopowej).
 */
class DefaultSSHSessionManager(
    private val repository: ConfigRepository
) : SSHSessionManager {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mutex = Mutex()

    private var terminalSession: SSHManager.TerminalSession? = null
    private var currentCommandJob: Job? = null

    private val _state = MutableStateFlow(SessionState())
    override val state: StateFlow<SessionState> = _state.asStateFlow()

    override fun isConnected(): Boolean = terminalSession?.isConnected() == true

    override fun ensureConnected(forceReconnect: Boolean) {
        val config = repository.loadConfig()
        val privateKey = repository.getPrivateKey()

        if (config == null || privateKey.isNullOrBlank()) {
            _state.update {
                it.copy(
                    isConnecting = false,
                    isConnected = false,
                    error = "Brak konfiguracji powiązanego hosta lub klucza prywatnego."
                )
            }
            return
        }

        if (!forceReconnect && terminalSession?.isConnected() == true) {
            Log.d(TAG, "Sesja SSH jest już aktywna (${config.username}@${config.host}). Pomijanie ponownego łączenia.")
            return
        }

        scope.launch {
            mutex.withLock {
                if (!forceReconnect && terminalSession?.isConnected() == true) return@launch

                val initialPrompt = "${config.username}@${config.host}:~$"
                _state.update {
                    it.copy(
                        isConnecting = true,
                        error = null,
                        prompt = if (it.prompt.isNotBlank() && it.prompt != "minow@raspberrypi:~$") it.prompt else initialPrompt,
                        lines = if (forceReconnect || it.lines.isEmpty()) {
                            it.lines + TerminalLine("Łączenie z ${config.host} przez SSH (Ed25519)...")
                        } else it.lines
                    )
                }

                try {
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
                            val prompt = TerminalFormatter.computePrompt(config.username, config.host, session.currentDirectory)
                            _state.update {
                                it.copy(
                                    isConnecting = false,
                                    isConnected = true,
                                    banner = banner,
                                    prompt = prompt,
                                    lines = it.lines + listOf(
                                        TerminalLine("Połączono pomyślnie z Raspberry Pi (${config.username}@${config.host}:${config.port})"),
                                        TerminalLine("Trwała sesja SSH aktywna w tle.")
                                    )
                                )
                            }
                        },
                        onFailure = { err ->
                            _state.update {
                                it.copy(
                                    isConnecting = false,
                                    isConnected = false,
                                    error = err.message,
                                    lines = it.lines + TerminalLine("Błąd połączenia SSH: ${err.message}", isError = true)
                                )
                            }
                        }
                    )
                } catch (e: Exception) {
                    _state.update {
                        it.copy(
                            isConnecting = false,
                            isConnected = false,
                            error = e.message,
                            lines = it.lines + TerminalLine("Błąd: ${e.message}", isError = true)
                        )
                    }
                }
            }
        }
    }

    override suspend fun executeCommand(command: String, onLineOutput: ((String, Boolean) -> Unit)?): Result<Int> {
        val session = terminalSession
        if (session == null || !session.isConnected()) {
            return Result.failure(IllegalStateException("Brak aktywnej sesji SSH. Połącz ponownie."))
        }

        return try {
            val result = session.executeCommand(command, onLineOutput ?: { _, _ -> })
            result.onSuccess {
                val config = repository.loadConfig()
                if (config != null) {
                    val newPrompt = TerminalFormatter.computePrompt(config.username, config.host, session.currentDirectory)
                    _state.update { it.copy(prompt = newPrompt) }
                }
            }
            result
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun cancelCurrentCommand() {
        terminalSession?.cancelCurrentCommand()
        currentCommandJob?.cancel()
    }

    override fun addLine(line: TerminalLine) {
        _state.update { it.copy(lines = it.lines + line) }
    }

    override fun clearLines() {
        _state.update { it.copy(lines = emptyList()) }
    }

    override fun disconnect() {
        try {
            terminalSession?.disconnect()
            terminalSession = null
        } catch (e: Exception) {
            Log.e(TAG, "Błąd podczas rozłączania sesji: ${e.message}")
        }
        _state.update {
            it.copy(
                isConnecting = false,
                isConnected = false,
                lines = it.lines + TerminalLine("Sesja SSH rozłączona.")
            )
        }
    }

    companion object {
        private const val TAG = "DefaultSSHSessionManager"
    }
}
