package com.example.mobile_smart_ssh

import com.example.mobile_smart_ssh.core.model.ServerConfig
import com.example.mobile_smart_ssh.fakes.FakeConfigRepository
import com.example.mobile_smart_ssh.fakes.FakeSSHSessionManager
import com.example.mobile_smart_ssh.fakes.MainDispatcherRule
import com.example.mobile_smart_ssh.ui.dashboard.DashboardAction
import com.example.mobile_smart_ssh.ui.dashboard.DashboardViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `init loads config and triggers session connection when host configured`() = runTest {
        val config = ServerConfig(host = "192.168.1.100", port = 22, username = "minow")
        val fakeRepo = FakeConfigRepository(initialConfig = config, initialPrivateKey = "pem_data")
        val fakeSession = FakeSSHSessionManager(initialConnected = true)

        val viewModel = DashboardViewModel(fakeRepo, fakeSession)

        val state = viewModel.uiState.value
        assertEquals("192.168.1.100", state.config?.host)
        assertTrue(fakeSession.ensureConnectedCalled)
        assertEquals(true, state.isHostOnline)
        assertFalse(state.isCheckingStatus)
    }

    @Test
    fun `init with no config keeps state config null and does not connect`() = runTest {
        val fakeRepo = FakeConfigRepository(initialConfig = null)
        val fakeSession = FakeSSHSessionManager()

        val viewModel = DashboardViewModel(fakeRepo, fakeSession)

        assertNull(viewModel.uiState.value.config)
        assertFalse(fakeSession.ensureConnectedCalled)
    }

    @Test
    fun `DashboardAction Refresh reloads updated configuration`() = runTest {
        val fakeRepo = FakeConfigRepository(initialConfig = null)
        val fakeSession = FakeSSHSessionManager()

        val viewModel = DashboardViewModel(fakeRepo, fakeSession)
        assertNull(viewModel.uiState.value.config)

        val newConfig = ServerConfig(host = "10.0.0.5", port = 2222, username = "pi")
        fakeRepo.saveConfig(newConfig)

        viewModel.onAction(DashboardAction.Refresh)

        assertEquals("10.0.0.5", viewModel.uiState.value.config?.host)
        assertEquals(2222, viewModel.uiState.value.config?.port)
    }

    @Test
    fun `DashboardAction CheckStatus sets isHostOnline true when session connected`() = runTest {
        val config = ServerConfig(host = "192.168.1.50", port = 22, username = "minow")
        val fakeRepo = FakeConfigRepository(initialConfig = config)
        val fakeSession = FakeSSHSessionManager(initialConnected = true)

        val viewModel = DashboardViewModel(fakeRepo, fakeSession)
        viewModel.onAction(DashboardAction.CheckStatus)

        val state = viewModel.uiState.value
        assertEquals(true, state.isHostOnline)
        assertFalse(state.isCheckingStatus)
        assertTrue(state.lastPingTimestamp > 0L)
    }
}
