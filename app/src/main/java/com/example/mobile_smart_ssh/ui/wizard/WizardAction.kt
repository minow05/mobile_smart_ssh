package com.example.mobile_smart_ssh.ui.wizard

sealed interface WizardAction {
    data class UpdateHost(val host: String) : WizardAction
    data class UpdatePort(val port: String) : WizardAction
    data class UpdateUsername(val username: String) : WizardAction
    data class UpdatePassword(val password: String) : WizardAction
    data class UpdateUseAsSmbPassword(val use: Boolean) : WizardAction
    data object ResetToInput : WizardAction
    data class StartAutomatedSetup(val onFinished: () -> Unit) : WizardAction
}
