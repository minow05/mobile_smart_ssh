package com.example.mobile_smart_ssh.network.ssh

import android.util.Log
import com.example.mobile_smart_ssh.core.model.RemoteFileItem
import com.example.mobile_smart_ssh.core.model.SystemResources
import com.example.mobile_smart_ssh.core.model.TerminalLine
import com.example.mobile_smart_ssh.data.repository.ConfigRepository
import com.example.mobile_smart_ssh.ui.terminal.TerminalFormatter
import com.jcraft.jsch.ChannelExec
import com.jcraft.jsch.Session
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStream
import java.util.Base64

/**
 * Domyślna implementacja SSHSessionManager zarządzająca pojedynczą, trwałą sesją SSH
 * przez cały cykl życia aplikacji (odpowiednik active_session m_session z wersji desktopowej).
 *
 * Wszystkie moduły (Terminal, Monitor zasobów, SFTP) współdzielą jedno fizyczne połączenie TCP/SSH
 * za pomocą multipleksingu kanałów (SSH Channel Multiplexing), dzięki czemu:
 * 1. Aplikacja łączy się natychmiast na starcie w tle.
 * 2. Przejścia do Terminala i Monitora zasobów nie wymagają ponownej negocjacji kluczy ani czekania.
 * 3. Zapytania o stan procesów/CPU trwają ułamek sekundy zamiast 2-3 sekund.
 */
class DefaultSSHSessionManager(
    private val repository: ConfigRepository
) : SSHSessionManager {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mutex = Mutex()

    private var rawSession: Session? = null
    private var activeExecChannel: ChannelExec? = null
    private var currentCommandJob: Job? = null
    private var currentDirectory: String = "~"

    private val _state = MutableStateFlow(SessionState())
    override val state: StateFlow<SessionState> = _state.asStateFlow()

    override fun isConnected(): Boolean = rawSession?.isConnected == true

    override fun ensureConnected(forceReconnect: Boolean) {
        scope.launch {
            ensureConnectedInternal(forceReconnect)
        }
    }

    private suspend fun ensureConnectedInternal(forceReconnect: Boolean = false) {
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

        if (!forceReconnect && rawSession?.isConnected == true) {
            return
        }

        mutex.withLock {
            if (!forceReconnect && rawSession?.isConnected == true) return

            val initialPrompt = "${config.username}@${config.host}:~$"
            _state.update {
                it.copy(
                    isConnecting = true,
                    error = null,
                    prompt = if (it.prompt.isNotBlank() && it.prompt != "minow@raspberrypi:~$") it.prompt else initialPrompt,
                    lines = if (forceReconnect || it.lines.isEmpty()) {
                        it.lines + TerminalLine("Łączenie z ${config.host} przez trwałą sesję SSH (Ed25519)...")
                    } else it.lines
                )
            }

            try {
                disconnectInternal()
                val session = SSHManager.createSession(
                    host = config.host,
                    port = config.port,
                    username = config.username,
                    privateKeyPem = privateKey
                )
                rawSession = session

                // Pobierz banner powitalny i początkowy katalog roboczy
                val initialInfo = try {
                    SSHManager.executeCommandOnSession(session, "pwd; uname -srm; uptime -p 2>/dev/null || true")
                } catch (_: Exception) {
                    "Linux Raspberry Pi"
                }

                val lines = initialInfo.trim().lines()
                if (lines.isNotEmpty() && lines[0].trim().startsWith("/")) {
                    currentDirectory = lines[0].trim()
                }
                val banner = if (lines.size > 1) lines.subList(1, lines.size).joinToString(" • ") else "Raspberry Pi Linux"
                val prompt = TerminalFormatter.computePrompt(config.username, config.host, currentDirectory)

                _state.update {
                    it.copy(
                        isConnecting = false,
                        isConnected = true,
                        banner = banner,
                        prompt = prompt,
                        lines = it.lines + listOf(
                            TerminalLine("Połączono pomyślnie z Raspberry Pi (${config.username}@${config.host}:${config.port})"),
                            TerminalLine("Trwała sesja SSH (m_session) aktywna dla wszystkich modułów.")
                        )
                    )
                }
                Log.i(TAG, "Główna sesja SSH nawiązana pomyślnie (${config.username}@${config.host})")
            } catch (e: Exception) {
                Log.e(TAG, "Błąd nawiązywania głównej sesji SSH: ${e.message}", e)
                _state.update {
                    it.copy(
                        isConnecting = false,
                        isConnected = false,
                        error = e.message,
                        lines = it.lines + TerminalLine("Błąd połączenia SSH: ${e.message}", isError = true)
                    )
                }
            }
        }
    }

    override suspend fun executeCommand(
        command: String,
        onLineOutput: ((String, Boolean) -> Unit)?
    ): Result<Int> = withContext(Dispatchers.IO) {
        ensureConnectedInternal(forceReconnect = false)
        val session = rawSession
        if (session == null || !session.isConnected) {
            return@withContext Result.failure(IllegalStateException("Brak aktywnego połączenia SSH."))
        }

        var channel: ChannelExec? = null
        try {
            channel = session.openChannel("exec") as ChannelExec
            activeExecChannel = channel

            val safeDir = if (currentDirectory.isBlank() || currentDirectory == "~") "" else currentDirectory
            val b64Command = Base64.getEncoder().encodeToString(command.toByteArray(Charsets.UTF_8))
            val b64Dir = Base64.getEncoder().encodeToString(safeDir.toByteArray(Charsets.UTF_8))

            val wrappedCommand = """
                /bin/sh -c '
                TARGET_DIR="${'$'}(echo "$b64Dir" | base64 -d 2>/dev/null || echo "$b64Dir" | base64 --decode 2>/dev/null)"
                if [ -n "${'$'}TARGET_DIR" ] && [ "${'$'}TARGET_DIR" != "~" ]; then
                    cd "${'$'}TARGET_DIR" 2>/dev/null || cd ~ 2>/dev/null || cd
                fi
                eval "${'$'}(echo "$b64Command" | base64 -d 2>/dev/null || echo "$b64Command" | base64 --decode 2>/dev/null)"
                __CMD_EXIT__=${'$'}?
                echo "___SMARTSSH_PWD___"
                pwd
                exit ${'$'}__CMD_EXIT__
                '
            """.trimIndent()

            channel.setCommand(wrappedCommand)
            channel.setInputStream(null)

            val stdout = channel.inputStream
            val stderr = channel.errStream

            channel.connect(10000)

            val outReader = BufferedReader(InputStreamReader(stdout, Charsets.UTF_8))
            val errReader = BufferedReader(InputStreamReader(stderr, Charsets.UTF_8))

            var reachedMarker = false
            var updatedPwd = currentDirectory

            coroutineScope {
                val stdoutJob = launch {
                    while (coroutineContext.isActive) {
                        val line = outReader.readLine() ?: break
                        if (line == "___SMARTSSH_PWD___") {
                            reachedMarker = true
                            continue
                        }
                        if (reachedMarker) {
                            val trimmed = line.trim()
                            if (trimmed.startsWith("/")) {
                                updatedPwd = trimmed
                            }
                        } else {
                            onLineOutput?.invoke(line, false)
                        }
                    }
                }

                val stderrJob = launch {
                    while (coroutineContext.isActive) {
                        val errLine = errReader.readLine() ?: break
                        onLineOutput?.invoke(errLine, true)
                    }
                }

                stdoutJob.join()
                stderrJob.join()
            }

            while (!channel.isClosed && coroutineContext.isActive) {
                delay(30)
            }

            currentDirectory = updatedPwd
            val config = repository.loadConfig()
            if (config != null) {
                val newPrompt = TerminalFormatter.computePrompt(config.username, config.host, currentDirectory)
                _state.update { it.copy(prompt = newPrompt) }
            }

            Result.success(channel.exitStatus)
        } catch (e: Exception) {
            Log.e(TAG, "Błąd wykonania polecenia w terminalu: ${e.message}", e)
            Result.failure(e)
        } finally {
            activeExecChannel = null
            channel?.disconnect()
        }
    }

    override suspend fun fetchSystemResources(): Result<SystemResources> = withContext(Dispatchers.IO) {
        ensureConnectedInternal(forceReconnect = false)
        val session = rawSession
        val config = repository.loadConfig()
        if (session == null || !session.isConnected || config == null) {
            return@withContext Result.failure(IllegalStateException("Brak aktywnego połączenia SSH."))
        }

        try {
            val compositeCommand = """
                /bin/sh -c '
                uptime
                echo "---DELIM_DF---"
                df -h /
                echo "---DELIM_FREE---"
                free -m
                echo "---DELIM_PS---"
                ps aux --sort=-%cpu | head -n 13
                echo "---DELIM_QUOTA---"
                quota -u ${config.username} 2>/dev/null || du -sh /home/${config.username} 2>/dev/null || echo "Brak danych"
                '
            """.trimIndent()

            val output = SSHManager.executeCommandOnSession(session, compositeCommand)
            val resources = SystemResourcesParser.parse(output, config.username)
            Result.success(resources)
        } catch (e: Exception) {
            Log.e(TAG, "Błąd pobierania zasobów na wspólnej sesji: ${e.message}", e)
            Result.failure(e)
        }
    }

    override suspend fun listSftpFiles(remotePath: String): Result<List<RemoteFileItem>> = withContext(Dispatchers.IO) {
        ensureConnectedInternal(forceReconnect = false)
        val session = rawSession
        if (session == null || !session.isConnected) {
            return@withContext Result.failure(IllegalStateException("Brak aktywnego połączenia SSH."))
        }

        try {
            val items = SSHManager.listSftpFilesOnSession(session, remotePath)
            Result.success(items)
        } catch (e: Exception) {
            Log.e(TAG, "Błąd listowania SFTP na wspólnej sesji: ${e.message}", e)
            Result.failure(e)
        }
    }

    override suspend fun createSftpDirectory(remotePath: String): Result<Unit> = withContext(Dispatchers.IO) {
        ensureConnectedInternal(forceReconnect = false)
        val session = rawSession
        if (session == null || !session.isConnected) {
            return@withContext Result.failure(IllegalStateException("Brak aktywnego połączenia SSH."))
        }

        try {
            SSHManager.createSftpDirectoryOnSession(session, remotePath)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteSftpFile(remotePath: String, isDirectory: Boolean): Result<Unit> = withContext(Dispatchers.IO) {
        ensureConnectedInternal(forceReconnect = false)
        val session = rawSession
        if (session == null || !session.isConnected) {
            return@withContext Result.failure(IllegalStateException("Brak aktywnego połączenia SSH."))
        }

        try {
            SSHManager.deleteSftpFileOnSession(session, remotePath, isDirectory)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun downloadSftpFile(remotePath: String, destinationStream: OutputStream): Result<Unit> = withContext(Dispatchers.IO) {
        ensureConnectedInternal(forceReconnect = false)
        val session = rawSession
        if (session == null || !session.isConnected) {
            return@withContext Result.failure(IllegalStateException("Brak aktywnego połączenia SSH."))
        }

        try {
            SSHManager.downloadSftpFileOnSession(session, remotePath, destinationStream)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun uploadSftpFile(sourceStream: InputStream, remotePath: String): Result<Unit> = withContext(Dispatchers.IO) {
        ensureConnectedInternal(forceReconnect = false)
        val session = rawSession
        if (session == null || !session.isConnected) {
            return@withContext Result.failure(IllegalStateException("Brak aktywnego połączenia SSH."))
        }

        try {
            SSHManager.uploadSftpFileOnSession(session, sourceStream, remotePath)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun cancelCurrentCommand() {
        try {
            activeExecChannel?.disconnect()
            activeExecChannel = null
        } catch (_: Exception) {}
        currentCommandJob?.cancel()
    }

    override fun addLine(line: TerminalLine) {
        _state.update { it.copy(lines = it.lines + line) }
    }

    override fun clearLines() {
        _state.update { it.copy(lines = emptyList()) }
    }

    override fun disconnect() {
        scope.launch {
            mutex.withLock {
                disconnectInternal()
                _state.update {
                    it.copy(
                        isConnecting = false,
                        isConnected = false,
                        lines = it.lines + TerminalLine("Sesja SSH rozłączona.")
                    )
                }
            }
        }
    }

    private fun disconnectInternal() {
        try { activeExecChannel?.disconnect() } catch (_: Exception) {}
        activeExecChannel = null
        try { rawSession?.disconnect() } catch (_: Exception) {}
        rawSession = null
    }

    companion object {
        private const val TAG = "DefaultSSHSessionManager"
    }
}
