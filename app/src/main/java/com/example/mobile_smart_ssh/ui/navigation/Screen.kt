package com.example.mobile_smart_ssh.ui.navigation

sealed class Screen(val route: String) {
    object SetupWizard : Screen("setup_wizard")
    object Dashboard : Screen("dashboard")
    object Terminal : Screen("terminal")
    object FileBrowser : Screen("file_browser")
    object ResourceMonitor : Screen("resource_monitor")
    object Settings : Screen("settings")
}
