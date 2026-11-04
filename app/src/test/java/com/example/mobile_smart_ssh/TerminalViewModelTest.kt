package com.example.mobile_smart_ssh

import com.example.mobile_smart_ssh.fakes.FakeConfigRepository
import com.example.mobile_smart_ssh.fakes.FakeSSHSessionManager
import com.example.mobile_smart_ssh.fakes.MainDispatcherRule
import com.example.mobile_smart_ssh.ui.terminal.TerminalAction
import com.example.mobile_smart_ssh.ui.terminal.TerminalViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TerminalViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `init ensures session connected and observes session lines`() = runTest {
        val fakeRepo = FakeConfigRepository()
        val fakeSession = FakeSSHSessionManager(initialConnected = true)

        val viewModel = TerminalViewModel(fakeRepo, fakeSession)

        assertTrue(fakeSession.ensureConnectedCalled)
        assertTrue(viewModel.uiState.value.isConnected)
    }

    @Test
    fun `TerminalAction UpdateInput updates current command input`() = runTest {
        val fakeRepo = FakeConfigRepository()
        val fakeSession = FakeSSHSessionManager()

        val viewModel = TerminalViewModel(fakeRepo, fakeSession)

        viewModel.onAction(TerminalAction.UpdateInput("df -h"))
        assertEquals("df -h", viewModel.uiState.value.currentCommandInput)
    }

    @Test
    fun `TerminalAction SendCommand with clear command clears session lines`() = runTest {
        val fakeRepo = FakeConfigRepository()
        val fakeSession = FakeSSHSessionManager(initialConnected = true)

        val viewModel = TerminalViewModel(fakeRepo, fakeSession)
        viewModel.onAction(TerminalAction.UpdateInput("clear"))
        viewModel.onAction(TerminalAction.SendCommand())

        assertEquals("", viewModel.uiState.value.currentCommandInput)
        assertTrue(fakeSession.state.value.lines.isEmpty())
    }

    @Test
    fun `TerminalAction SendCommand executes command on connected session and adds to commandHistory`() = runTest {
        val fakeRepo = FakeConfigRepository()
        val fakeSession = FakeSSHSessionManager(initialConnected = true)

        val viewModel = TerminalViewModel(fakeRepo, fakeSession)
        viewModel.onAction(TerminalAction.SendCommand("uptime"))

        assertTrue(fakeSession.executedCommands.contains("uptime"))
        assertFalse(viewModel.uiState.value.isExecuting)
        assertEquals("", viewModel.uiState.value.currentCommandInput)
        assertEquals(listOf("uptime"), viewModel.uiState.value.commandHistory)
    }

    @Test
    fun `TerminalAction CancelCommand resets isExecuting to false and adds ^C`() = runTest {
        val fakeRepo = FakeConfigRepository()
        val fakeSession = FakeSSHSessionManager(initialConnected = true)

        val viewModel = TerminalViewModel(fakeRepo, fakeSession)
        viewModel.onAction(TerminalAction.CancelCommand)

        assertFalse(viewModel.uiState.value.isExecuting)
        assertTrue(fakeSession.state.value.lines.any { it.text == "^C" })
    }

    @Test
    fun `HistoryUp and HistoryDown cycle through executed commands`() = runTest {
        val fakeRepo = FakeConfigRepository()
        val fakeSession = FakeSSHSessionManager(initialConnected = true)

        val viewModel = TerminalViewModel(fakeRepo, fakeSession)
        viewModel.onAction(TerminalAction.SendCommand("ls -la"))
        viewModel.onAction(TerminalAction.SendCommand("uname -a"))

        // Naciśnięcie HistoryUp wybiera ostatnie polecenie ("uname -a")
        viewModel.onAction(TerminalAction.HistoryUp)
        assertEquals("uname -a", viewModel.uiState.value.currentCommandInput)

        // Kolejne HistoryUp cofa się do wcześniejszego ("ls -la")
        viewModel.onAction(TerminalAction.HistoryUp)
        assertEquals("ls -la", viewModel.uiState.value.currentCommandInput)

        // HistoryDown przechodzi w dół historii ("uname -a")
        viewModel.onAction(TerminalAction.HistoryDown)
        assertEquals("uname -a", viewModel.uiState.value.currentCommandInput)

        // HistoryDown na końcu przywraca pusty input
        viewModel.onAction(TerminalAction.HistoryDown)
        assertEquals("", viewModel.uiState.value.currentCommandInput)
    }

    @Test
    fun `ToggleCtrl and ToggleAlt change modifier flags`() = runTest {
        val fakeRepo = FakeConfigRepository()
        val fakeSession = FakeSSHSessionManager()

        val viewModel = TerminalViewModel(fakeRepo, fakeSession)
        assertFalse(viewModel.uiState.value.isCtrlActive)
        assertFalse(viewModel.uiState.value.isAltActive)

        viewModel.onAction(TerminalAction.ToggleCtrl)
        assertTrue(viewModel.uiState.value.isCtrlActive)

        viewModel.onAction(TerminalAction.ToggleAlt)
        assertTrue(viewModel.uiState.value.isAltActive)
    }

    @Test
    fun `InsertKey appends character to input`() = runTest {
        val fakeRepo = FakeConfigRepository()
        val fakeSession = FakeSSHSessionManager()

        val viewModel = TerminalViewModel(fakeRepo, fakeSession)
        viewModel.onAction(TerminalAction.InsertKey("~"))
        viewModel.onAction(TerminalAction.InsertKey("/"))

        assertEquals("~/", viewModel.uiState.value.currentCommandInput)
    }

    @Test
    fun `InsertKey with c while Ctrl is active cancels command`() = runTest {
        val fakeRepo = FakeConfigRepository()
        val fakeSession = FakeSSHSessionManager(initialConnected = true)

        val viewModel = TerminalViewModel(fakeRepo, fakeSession)
        viewModel.onAction(TerminalAction.ToggleCtrl)
        assertTrue(viewModel.uiState.value.isCtrlActive)

        viewModel.onAction(TerminalAction.InsertKey("c"))
        assertFalse(viewModel.uiState.value.isCtrlActive)
        assertTrue(fakeSession.state.value.lines.any { it.text == "^C" })
    }

    @Test
    fun `TabComplete auto-completes unique prefix`() = runTest {
        val fakeRepo = FakeConfigRepository()
        val fakeSession = FakeSSHSessionManager()

        val viewModel = TerminalViewModel(fakeRepo, fakeSession)
        viewModel.onAction(TerminalAction.UpdateInput("upti"))
        viewModel.onAction(TerminalAction.TabComplete)

        assertEquals("uptime ", viewModel.uiState.value.currentCommandInput)
    }

    @Test
    fun `Escape clears input and modifier state`() = runTest {
        val fakeRepo = FakeConfigRepository()
        val fakeSession = FakeSSHSessionManager()

        val viewModel = TerminalViewModel(fakeRepo, fakeSession)
        viewModel.onAction(TerminalAction.UpdateInput("some text"))
        viewModel.onAction(TerminalAction.ToggleCtrl)
        viewModel.onAction(TerminalAction.ToggleAlt)

        viewModel.onAction(TerminalAction.Escape)
        assertEquals("", viewModel.uiState.value.currentCommandInput)
        assertFalse(viewModel.uiState.value.isCtrlActive)
        assertFalse(viewModel.uiState.value.isAltActive)
    }
}
