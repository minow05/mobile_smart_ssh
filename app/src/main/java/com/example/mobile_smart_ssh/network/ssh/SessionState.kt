package com.example.mobile_smart_ssh.network.ssh

import com.example.mobile_smart_ssh.core.model.TerminalLine

data class SessionState(
    val isConnecting: Boolean = false,
    val isConnected: Boolean = false,
    val banner: String = "",
    val prompt: String = "minow@raspberrypi:~$",
    val lines: List<TerminalLine> = emptyList(),
    val error: String? = null
)
