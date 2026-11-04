package com.example.mobile_smart_ssh

import com.example.mobile_smart_ssh.core.model.ServerConfig
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
    fun `TerminalAction SendCommand executes command on connected session`() = runTest {
        val fakeRepo = FakeConfigRepository()
        val fakeSession = FakeSSHSessionManager(initialConnected = true)

        val viewModel = TerminalViewModel(fakeRepo, fakeSession)
        viewModel.onAction(TerminalAction.SendCommand("uptime"))

        assertTrue(fakeSession.executedCommands.contains("uptime"))
        assertFalse(viewModel.uiState.value.isExecuting)
        assertEquals("", viewModel.uiState.value.currentCommandInput)
    }

    @Test
    fun `TerminalAction CancelCommand resets isExecuting to false`() = runTest {
        val fakeRepo = FakeConfigRepository()
        val fakeSession = FakeSSHSessionManager(initialConnected = true)

        val viewModel = TerminalViewModel(fakeRepo, fakeSession)
        viewModel.onAction(TerminalAction.CancelCommand)

        assertFalse(viewModel.uiState.value.isExecuting)
    }
}
