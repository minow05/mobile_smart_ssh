package com.example.mobile_smart_ssh.network.ssh

import com.example.mobile_smart_ssh.core.model.RemoteFileItem
import com.example.mobile_smart_ssh.core.model.SystemResources
import com.example.mobile_smart_ssh.core.model.TerminalLine
import com.example.mobile_smart_ssh.data.repository.ConfigRepository
import kotlinx.coroutines.flow.StateFlow
import java.io.InputStream
import java.io.OutputStream

/**
 * Interfejs globalnego menedżera pojedynczej, trwałej sesji SSH (odpowiednik active_session m_session z C++).
 * Utrzymuje pojedyncze połączenie TCP/SSH nawiązywane na starcie aplikacji i współdzieli je
 * za pomocą multipleksingu kanałów (SSH Channel Multiplexing) pomiędzy:
 * - Terminal (interaktywna sesja ze strumieniowaniem stdout/stderr)
 * - Monitor zasobów (błyskawiczne pobieranie metryk CPU/RAM/htop w dziesiątkach milisekund bez renegocjacji SSH)
 * - Menedżer plików (kanały SFTP działające na tym samym gnieździe TCP)
 */
interface SSHSessionManager {
    val state: StateFlow<SessionState>

    fun isConnected(): Boolean
    fun ensureConnected(forceReconnect: Boolean = false)
    suspend fun executeCommand(command: String, onLineOutput: ((String, Boolean) -> Unit)? = null): Result<Int>
    suspend fun fetchSystemResources(): Result<SystemResources>
    suspend fun listSftpFiles(remotePath: String): Result<List<RemoteFileItem>>
    suspend fun createSftpDirectory(remotePath: String): Result<Unit>
    suspend fun deleteSftpFile(remotePath: String, isDirectory: Boolean): Result<Unit>
    suspend fun downloadSftpFile(remotePath: String, destinationStream: OutputStream): Result<Unit>
    suspend fun uploadSftpFile(sourceStream: InputStream, remotePath: String): Result<Unit>
    fun cancelCurrentCommand()
    fun addLine(line: TerminalLine)
    fun clearLines()
    fun disconnect()

    companion object {
        fun create(repository: ConfigRepository): SSHSessionManager =
            DefaultSSHSessionManager(repository)
    }
}
