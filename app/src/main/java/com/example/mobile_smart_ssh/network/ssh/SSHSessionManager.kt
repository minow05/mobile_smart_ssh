package com.example.mobile_smart_ssh.network.ssh

import com.example.mobile_smart_ssh.core.model.TerminalLine
import com.example.mobile_smart_ssh.data.repository.ConfigRepository
import kotlinx.coroutines.flow.StateFlow

/**
 * Interfejs globalnego menedżera sesji SSH.
 * Pozwala na utrzymanie trwałej sesji w tle oraz łatwe mockowanie w testach jednostkowych.
 */
interface SSHSessionManager {
    val state: StateFlow<SessionState>

    fun isConnected(): Boolean
    fun ensureConnected(forceReconnect: Boolean = false)
    suspend fun executeCommand(command: String, onLineOutput: ((String, Boolean) -> Unit)? = null): Result<Int>
    fun cancelCurrentCommand()
    fun addLine(line: TerminalLine)
    fun clearLines()
    fun disconnect()

    companion object {
        fun create(repository: ConfigRepository): SSHSessionManager =
            DefaultSSHSessionManager(repository)
    }
}
