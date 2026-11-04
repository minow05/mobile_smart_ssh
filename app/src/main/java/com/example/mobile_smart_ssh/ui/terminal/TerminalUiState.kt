package com.example.mobile_smart_ssh.ui.terminal

import com.example.mobile_smart_ssh.core.model.TerminalLine

/**
 * Stan interfejsu terminala w stylu Termux:
 * obsługa historii komend, klawiszy funkcyjnych CTRL/ALT oraz linii bufora.
 */
data class TerminalUiState(
    val lines: List<TerminalLine> = emptyList(),
    val isConnecting: Boolean = false,
    val isConnected: Boolean = false,
    val isExecuting: Boolean = false,
    val currentCommandInput: String = "",
    val error: String? = null,
    val prompt: String = "minow@raspberrypi:~$",
    val commandHistory: List<String> = emptyList(),
    val historyIndex: Int = -1,
    val isCtrlActive: Boolean = false,
    val isAltActive: Boolean = false
)
