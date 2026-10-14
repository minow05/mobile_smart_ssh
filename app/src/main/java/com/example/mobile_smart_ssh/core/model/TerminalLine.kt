package com.example.mobile_smart_ssh.core.model

/**
 * Pojedyncza linia wyjściowa lub wejściowa w terminalu SSH.
 */
data class TerminalLine(
    val text: String,
    val isCommand: Boolean = false,
    val isError: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)
