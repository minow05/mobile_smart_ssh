package com.example.mobile_smart_ssh.ui.settings

sealed interface SettingsAction {
    data object LoadSettings : SettingsAction
    data class UpdateSmbPassword(val password: String) : SettingsAction
    data class ToggleMonitor(val enabled: Boolean) : SettingsAction
    data class SetResetDialogOpen(val open: Boolean) : SettingsAction
    data class ResetConfiguration(val onResetComplete: () -> Unit) : SettingsAction
    data object SendTestNotification : SettingsAction
    data object TriggerImmediateCheck : SettingsAction
    data object ClearStatusMessage : SettingsAction
}
