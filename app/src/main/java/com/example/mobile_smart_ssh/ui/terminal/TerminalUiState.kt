package com.example.mobile_smart_ssh.ui.terminal

import com.example.mobile_smart_ssh.core.model.TerminalLine

data class TerminalUiState(
    val lines: List<TerminalLine> = emptyList(),
    val isConnecting: Boolean = false,
    val isConnected: Boolean = false,
    val isExecuting: Boolean = false,
    val currentCommandInput: String = "",
    val error: String? = null,
    val prompt: String = "minow@raspberrypi:~$"
)
