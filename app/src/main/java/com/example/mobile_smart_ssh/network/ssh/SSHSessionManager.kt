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

data class SessionState(
    val isConnecting: Boolean = false,
    val isConnected: Boolean = false,
    val banner: String = "",
    val prompt: String = "minow@raspberrypi:~$",
    val lines: List<TerminalLine> = emptyList(),
    val error: String? = null
)

/**
 * Globalny menedżer sesji SSH (odpowiednik active_session m_session z wersji desktopowej).
 * Utrzymuje pojedyncze, trwałe połączenie SSH przez cały cykl życia aplikacji,
 * dzięki czemu wejście w terminal nie wymaga ponownego łączenia za każdym razem.
 */
class SSHSessionManager(
    private val repository: ConfigRepository
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mutex = Mutex()

    private var terminalSession: SSHManager.TerminalSession? = null
    private var currentCommandJob: Job? = null

    private val _state = MutableStateFlow(SessionState())
    val state: StateFlow<SessionState> = _state.asStateFlow()

    fun isConnected(): Boolean = terminalSession?.isConnected() == true

    /**
     * Nawiązuje połączenie w tle (na startupie aplikacji lub przy otwarciu terminala).
     * Jeśli sesja jest już aktywna, pomija ponowne łączenie (zero opóźnienia przy wchodzeniu do terminala).
     */
    fun ensureConnected(forceReconnect: Boolean = false) {
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
                                        TerminalLine("Połączono z ${config.username}@${config.host} ($banner)"),
                                        TerminalLine("Wpisz polecenie lub wybierz z listy poniżej.")
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

    suspend fun executeCommand(
        command: String,
        onOutput: (line: String, isError: Boolean) -> Unit
    ): Result<Int> {
        val session = terminalSession
        if (session == null || !session.isConnected()) {
            return Result.failure(Exception("Brak aktywnego połączenia SSH."))
        }
        val result = session.executeCommand(command, onOutput)
        val config = repository.loadConfig()
        val user = config?.username ?: "minow"
        val host = config?.host ?: "rpi"
        val updatedPrompt = TerminalFormatter.computePrompt(user, host, session.currentDirectory)
        _state.update { it.copy(prompt = updatedPrompt) }
        return result
    }

    fun addLine(line: TerminalLine) {
        _state.update { it.copy(lines = (it.lines + line).takeLast(1000)) }
    }

    fun clearLines() {
        _state.update { it.copy(lines = emptyList()) }
    }

    fun cancelCurrentCommand() {
        terminalSession?.cancelCurrentCommand()
        currentCommandJob?.cancel()
        addLine(TerminalLine("^C (Zatrzymano)", isError = true))
    }

    fun disconnect() {
        terminalSession?.disconnect()
        terminalSession = null
        _state.update {
            it.copy(
                isConnected = false,
                isConnecting = false
            )
        }
    }

    companion object {
        private const val TAG = "SSHSessionManager"
    }
}
