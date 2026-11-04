package com.example.mobile_smart_ssh

import com.example.mobile_smart_ssh.core.model.ServerConfig
import com.example.mobile_smart_ssh.fakes.FakeConfigRepository
import com.example.mobile_smart_ssh.fakes.FakeSSHSessionManager
import com.example.mobile_smart_ssh.fakes.MainDispatcherRule
import com.example.mobile_smart_ssh.ui.resources.ResourceMonitorAction
import com.example.mobile_smart_ssh.ui.resources.ResourceMonitorViewModel
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
class ResourceMonitorViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `init with unconfigured repository sets error in state`() = runTest {
        val fakeRepo = FakeConfigRepository(initialConfig = null, initialPrivateKey = null)
        val fakeSession = FakeSSHSessionManager()

        val viewModel = ResourceMonitorViewModel(fakeRepo, fakeSession)

        assertNotNull(viewModel.uiState.value.error)
        assertTrue(viewModel.uiState.value.error?.contains("Brak konfiguracji") == true)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `ResourceMonitorAction Refresh with unconfigured repository keeps error in state`() = runTest {
        val fakeRepo = FakeConfigRepository(initialConfig = null)
        val fakeSession = FakeSSHSessionManager()

        val viewModel = ResourceMonitorViewModel(fakeRepo, fakeSession)
        viewModel.onAction(ResourceMonitorAction.Refresh)

        assertNotNull(viewModel.uiState.value.error)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `configured repository fetches resources immediately via shared SSHSessionManager`() = runTest {
        val fakeRepo = FakeConfigRepository(
            initialConfig = ServerConfig("minow.rpi", 22, "minow"),
            initialPrivateKey = "MOCK_KEY"
        )
        val fakeSession = FakeSSHSessionManager(initialConnected = true)

        val viewModel = ResourceMonitorViewModel(fakeRepo, fakeSession)

        assertNull(viewModel.uiState.value.error)
        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals("14 days", viewModel.uiState.value.resources.uptime)
        assertEquals("64 GB", viewModel.uiState.value.resources.diskTotal)
        assertEquals("1024 MB", viewModel.uiState.value.resources.ramUsed)
    }
}
