package com.example.mobile_smart_ssh.ui.dashboard

sealed interface DashboardAction {
    data object Refresh : DashboardAction
    data object CheckStatus : DashboardAction
}
