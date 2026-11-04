package com.example.mobile_smart_ssh.fakes

import com.example.mobile_smart_ssh.core.model.RemoteFileItem
import com.example.mobile_smart_ssh.core.model.SystemResources
import com.example.mobile_smart_ssh.core.model.TerminalLine
import com.example.mobile_smart_ssh.network.ssh.SSHSessionManager
import com.example.mobile_smart_ssh.network.ssh.SessionState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.io.InputStream
import java.io.OutputStream

class FakeSSHSessionManager(
    initialConnected: Boolean = false
) : SSHSessionManager {

    private val _state = MutableStateFlow(SessionState(isConnected = initialConnected))
    override val state: StateFlow<SessionState> = _state.asStateFlow()

    var connected: Boolean = initialConnected
    val executedCommands = mutableListOf<String>()
    var disconnectCalled = false
    var ensureConnectedCalled = false

    var mockSystemResources: SystemResources = SystemResources(
        uptime = "14 days",
        diskTotal = "64 GB",
        diskUsed = "16 GB",
        diskFree = "48 GB",
        diskPercentage = 25f,
        ramUsed = "1024 MB",
        ramTotal = "4096 MB",
        ramPercentage = 25f,
        cpuPercentage = 15f
    )
    var mockSftpFiles: List<RemoteFileItem> = emptyList()

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

    override suspend fun fetchSystemResources(): Result<SystemResources> {
        return Result.success(mockSystemResources)
    }

    override suspend fun listSftpFiles(remotePath: String): Result<List<RemoteFileItem>> {
        return Result.success(mockSftpFiles)
    }

    override suspend fun createSftpDirectory(remotePath: String): Result<Unit> {
        return Result.success(Unit)
    }

    override suspend fun deleteSftpFile(remotePath: String, isDirectory: Boolean): Result<Unit> {
        return Result.success(Unit)
    }

    override suspend fun downloadSftpFile(remotePath: String, destinationStream: OutputStream): Result<Unit> {
        return Result.success(Unit)
    }

    override suspend fun uploadSftpFile(sourceStream: InputStream, remotePath: String): Result<Unit> {
        return Result.success(Unit)
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
