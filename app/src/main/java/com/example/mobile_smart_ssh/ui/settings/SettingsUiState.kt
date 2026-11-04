package com.example.mobile_smart_ssh.ui.settings

import com.example.mobile_smart_ssh.core.model.ServerConfig

data class SettingsUiState(
    val config: ServerConfig? = null,
    val publicKey: String = "",
    val fingerprint: String = "",
    val isResetDialogOpen: Boolean = false,
    val smbPasswordInput: String = "",
    val monitorEnabled: Boolean = true,
    val hasNotificationPermission: Boolean = false,
    val statusMessage: String? = null
)
