package com.example.mobile_smart_ssh.ui.resources

import com.example.mobile_smart_ssh.core.model.SystemResources

data class ResourceMonitorUiState(
    val resources: SystemResources = SystemResources(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val lastRefreshTime: Long = 0L
)
