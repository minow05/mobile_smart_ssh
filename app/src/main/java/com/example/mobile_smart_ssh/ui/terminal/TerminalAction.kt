package com.example.mobile_smart_ssh.ui.terminal

sealed interface TerminalAction {
    data class UpdateInput(val input: String) : TerminalAction
    data class SendCommand(val command: String? = null) : TerminalAction
    data object CancelCommand : TerminalAction
    data object Connect : TerminalAction
    data object HistoryUp : TerminalAction
    data object HistoryDown : TerminalAction
    data class InsertKey(val keyText: String) : TerminalAction
    data object ToggleCtrl : TerminalAction
    data object ToggleAlt : TerminalAction
    data object TabComplete : TerminalAction
    data object ClearScreen : TerminalAction
    data object Escape : TerminalAction
}
