package com.example.mobile_smart_ssh.ui.dashboard

import com.example.mobile_smart_ssh.core.model.ServerConfig

data class DashboardUiState(
    val config: ServerConfig? = null,
    val isCheckingStatus: Boolean = false,
    val isHostOnline: Boolean? = null,
    val lastPingTimestamp: Long = 0L,
    val quickUptime: String = ""
)
