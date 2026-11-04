package com.example.mobile_smart_ssh

import com.example.mobile_smart_ssh.ui.terminal.TerminalFormatter
import org.junit.Assert.assertEquals
import org.junit.Test

class TerminalFormatterTest {

    @Test
    fun testComputePrompt() {
        // Katalog domowy
        assertEquals("minow@172.30.0.1:~$", TerminalFormatter.computePrompt("minow", "172.30.0.1", "~"))
        assertEquals("minow@172.30.0.1:~$", TerminalFormatter.computePrompt("minow", "172.30.0.1", "/home/minow"))
        assertEquals("minow@172.30.0.1:~$", TerminalFormatter.computePrompt("minow", "172.30.0.1", ""))

        // Podkatalog w katalogu domowym
        assertEquals("minow@172.30.0.1:~/shared$", TerminalFormatter.computePrompt("minow", "172.30.0.1", "/home/minow/shared"))
        assertEquals("minow@172.30.0.1:~/PWR/docs$", TerminalFormatter.computePrompt("minow", "172.30.0.1", "/home/minow/PWR/docs"))

        // Ścieżka systemowa poza katalogiem domowym
        assertEquals("minow@172.30.0.1:/var/log$", TerminalFormatter.computePrompt("minow", "172.30.0.1", "/var/log"))
        assertEquals("minow@172.30.0.1:/etc/samba$", TerminalFormatter.computePrompt("minow", "172.30.0.1", "/etc/samba"))
    }

    @Test
    fun testStripAnsiCodes() {
        val ansiColoredText = "\u001B[32mLinux minowRPI 6.17\u001B[0m"
        assertEquals("Linux minowRPI 6.17", TerminalFormatter.stripAnsiCodes(ansiColoredText))

        val ansiClearLine = "\u001B[2K\u001B[1Gminow@minowRPI"
        assertEquals("minow@minowRPI", TerminalFormatter.stripAnsiCodes(ansiClearLine))

        val oscTitleSeq = "\u001B]0;minow@minowRPI: ~\u0007Some text"
        assertEquals("Some text", TerminalFormatter.stripAnsiCodes(oscTitleSeq))

        val carriageReturnText = "hello world\r"
        assertEquals("hello world", TerminalFormatter.stripAnsiCodes(carriageReturnText))
    }
}
