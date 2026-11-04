package com.example.mobile_smart_ssh.fakes

import com.example.mobile_smart_ssh.core.model.TerminalLine
import com.example.mobile_smart_ssh.network.ssh.SSHSessionManager
import com.example.mobile_smart_ssh.network.ssh.SessionState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class FakeSSHSessionManager(
    initialConnected: Boolean = false
) : SSHSessionManager {

    private val _state = MutableStateFlow(SessionState(isConnected = initialConnected))
    override val state: StateFlow<SessionState> = _state.asStateFlow()

    var connected: Boolean = initialConnected
    val executedCommands = mutableListOf<String>()
    var disconnectCalled = false
    var ensureConnectedCalled = false

    override fun isConnected(): Boolean = connected

    override fun ensureConnected(forceReconnect: Boolean) {
        ensureConnectedCalled = true
        connected = true
        _state.update { it.copy(isConnected = true, isConnecting = false) }
    }

    override suspend fun executeCommand(
        command: String,
        onLineOutput: ((String, Boolean) -> Unit)?
    ): Result<Int> {
        executedCommands.add(command)
        onLineOutput?.invoke("Output for $command", false)
        return Result.success(0)
    }

    override fun cancelCurrentCommand() {
        // no-op
    }

    override fun addLine(line: TerminalLine) {
        _state.update { it.copy(lines = it.lines + line) }
    }

    override fun clearLines() {
        _state.update { it.copy(lines = emptyList()) }
    }

    override fun disconnect() {
        disconnectCalled = true
        connected = false
        _state.update { it.copy(isConnected = false) }
    }
}
