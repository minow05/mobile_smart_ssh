package com.example.mobile_smart_ssh.ui.terminal

sealed interface TerminalAction {
    data class UpdateInput(val input: String) : TerminalAction
    data class SendCommand(val command: String? = null) : TerminalAction
    data object CancelCommand : TerminalAction
    data object Connect : TerminalAction
}
