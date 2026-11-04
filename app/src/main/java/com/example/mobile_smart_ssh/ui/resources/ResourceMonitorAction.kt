package com.example.mobile_smart_ssh.ui.resources

sealed interface ResourceMonitorAction {
    data object Refresh : ResourceMonitorAction
}
