package com.example.mobile_smart_ssh.ui.terminal

/**
 * Narzędzia formatowania tekstu terminala, promptu oraz czyszczenia kodów ANSI.
 */
object TerminalFormatter {

    fun computePrompt(user: String, host: String, rawDir: String): String {
        val homeDir = "/home/$user"
        val displayDir = when {
            rawDir.isBlank() || rawDir == homeDir || rawDir == "~" -> "~"
            rawDir.startsWith("$homeDir/") -> "~" + rawDir.removePrefix(homeDir)
            else -> rawDir
        }
        return "$user@$host:$displayDir$"
    }

    fun stripAnsiCodes(input: String): String {
        return input
            .replace(Regex("\u001B\\[[;?0-9]*[a-zA-Z]"), "")
            .replace(Regex("\u001B\\][^\u0007\u001B]*(\u0007|\u001B\\\\)"), "")
            .replace(Regex("\u001B[()][A-Za-z0-9]"), "")
            .replace(Regex("\u001B[=>]"), "")
            .replace("\r", "")
    }
}
