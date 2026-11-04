package com.example.mobile_smart_ssh

import com.example.mobile_smart_ssh.core.model.TerminalLine
import com.example.mobile_smart_ssh.network.ssh.SessionState
import com.example.mobile_smart_ssh.ui.dashboard.DashboardAction
import com.example.mobile_smart_ssh.ui.files.FileBrowserAction
import com.example.mobile_smart_ssh.ui.files.StorageDrive
import com.example.mobile_smart_ssh.ui.resources.ResourceMonitorAction
import com.example.mobile_smart_ssh.ui.settings.SettingsAction
import com.example.mobile_smart_ssh.ui.terminal.TerminalAction
import com.example.mobile_smart_ssh.ui.wizard.WizardAction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MviActionTest {

    @Test
    fun testTerminalActions() {
        val updateAction = TerminalAction.UpdateInput("uptime")
        assertEquals("uptime", updateAction.input)

        val sendAction = TerminalAction.SendCommand("free -m")
        assertEquals("free -m", sendAction.command)

        val cancelAction = TerminalAction.CancelCommand
        assertTrue(cancelAction is TerminalAction)

        val connectAction = TerminalAction.Connect
        assertTrue(connectAction is TerminalAction)

        val historyUp = TerminalAction.HistoryUp
        assertTrue(historyUp is TerminalAction)

        val historyDown = TerminalAction.HistoryDown
        assertTrue(historyDown is TerminalAction)

        val insertKey = TerminalAction.InsertKey("~")
        assertEquals("~", insertKey.keyText)

        val toggleCtrl = TerminalAction.ToggleCtrl
        assertTrue(toggleCtrl is TerminalAction)

        val toggleAlt = TerminalAction.ToggleAlt
        assertTrue(toggleAlt is TerminalAction)

        val tab = TerminalAction.TabComplete
        assertTrue(tab is TerminalAction)

        val clr = TerminalAction.ClearScreen
        assertTrue(clr is TerminalAction)

        val esc = TerminalAction.Escape
        assertTrue(esc is TerminalAction)
    }

    @Test
    fun testDashboardActions() {
        val refresh = DashboardAction.Refresh
        val check = DashboardAction.CheckStatus
        assertTrue(refresh is DashboardAction)
        assertTrue(check is DashboardAction)
    }

    @Test
    fun testFileBrowserActions() {
        val selectDrive = FileBrowserAction.SelectDrive(StorageDrive.SHARED)
        assertEquals(StorageDrive.SHARED, selectDrive.drive)

        val newDir = FileBrowserAction.SetNewDirName("my_folder")
        assertEquals("my_folder", newDir.name)

        val dialogVisible = FileBrowserAction.SetCreateDirDialogVisible(true)
        assertTrue(dialogVisible.visible)

        val navUp = FileBrowserAction.NavigateUp
        assertTrue(navUp is FileBrowserAction)
    }

    @Test
    fun testSettingsActions() {
        val testNotif = SettingsAction.SendTestNotification
        assertTrue(testNotif is SettingsAction)

        val triggerCheck = SettingsAction.TriggerImmediateCheck
        assertTrue(triggerCheck is SettingsAction)

        val toggleMonitor = SettingsAction.ToggleMonitor(false)
        assertFalse(toggleMonitor.enabled)

        val pwdAction = SettingsAction.UpdateSmbPassword("newSecret123")
        assertEquals("newSecret123", pwdAction.password)
    }

    @Test
    fun testWizardActions() {
        val updateHost = WizardAction.UpdateHost("minow.rpi")
        assertEquals("minow.rpi", updateHost.host)

        val updatePort = WizardAction.UpdatePort("2222")
        assertEquals("2222", updatePort.port)

        val updateUsername = WizardAction.UpdateUsername("rachel")
        assertEquals("rachel", updateUsername.username)

        val reset = WizardAction.ResetToInput
        assertTrue(reset is WizardAction)
    }

    @Test
    fun testSessionStateDefaults() {
        val state = SessionState()
        assertFalse(state.isConnected)
        assertFalse(state.isConnecting)
        assertEquals("", state.banner)
        assertEquals("minow@raspberrypi:~$", state.prompt)
        assertTrue(state.lines.isEmpty())
    }

    @Test
    fun testSessionStateWithLines() {
        val lines = listOf(
            TerminalLine("Connected to minow@raspberrypi"),
            TerminalLine("minow@raspberrypi:~$ ls -la", isCommand = true),
            TerminalLine("total 48")
        )
        val state = SessionState(
            isConnected = true,
            banner = "Linux 6.17",
            prompt = "minow@raspberrypi:~/PWR$",
            lines = lines
        )

        assertTrue(state.isConnected)
        assertEquals(3, state.lines.size)
        assertTrue(state.lines[1].isCommand)
        assertEquals("minow@raspberrypi:~/PWR$", state.prompt)
    }
}
