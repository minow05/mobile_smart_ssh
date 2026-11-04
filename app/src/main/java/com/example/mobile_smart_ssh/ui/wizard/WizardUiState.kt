package com.example.mobile_smart_ssh.ui.wizard

import com.example.mobile_smart_ssh.core.security.SshKeyPair

data class WizardUiState(
    val host: String = "minow.rpi",
    val port: String = "22",
    val username: String = "minow",
    val password: String = "",
    val useAsSmbPassword: Boolean = true,
    val currentStage: WizardStage = WizardStage.INPUT,
    val isRunning: Boolean = false,
    val logs: List<WizardLogEntry> = emptyList(),
    val error: String? = null,
    val keyPair: SshKeyPair? = null
)
