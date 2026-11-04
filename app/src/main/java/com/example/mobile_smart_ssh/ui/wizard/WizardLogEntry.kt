package com.example.mobile_smart_ssh.ui.wizard

data class WizardLogEntry(
    val text: String,
    val type: LogType = LogType.INFO
)
