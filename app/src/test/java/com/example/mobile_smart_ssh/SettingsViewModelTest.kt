package com.example.mobile_smart_ssh

import com.example.mobile_smart_ssh.core.model.ServerConfig
import com.example.mobile_smart_ssh.fakes.FakeConfigRepository
import com.example.mobile_smart_ssh.fakes.FakeSSHSessionManager
import com.example.mobile_smart_ssh.fakes.FakeServerMonitor
import com.example.mobile_smart_ssh.fakes.MainDispatcherRule
import com.example.mobile_smart_ssh.ui.settings.SettingsAction
import com.example.mobile_smart_ssh.ui.settings.SettingsViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `init loads config, public key, and monitor state`() = runTest {
        val config = ServerConfig(host = "minow.rpi", port = 22, username = "minow", smbPassword = "oldPassword")
        val fakeRepo = FakeConfigRepository(
            initialConfig = config,
            initialPrivateKey = "priv_key",
            initialPublicKey = "ssh-ed25519 AAAAC3NzaC1lZDI1NTE5AAAAI0123456789 minow"
        )
        val fakeMonitor = FakeServerMonitor(monitorEnabled = true, hasPermission = true)
        val fakeSession = FakeSSHSessionManager()

        val viewModel = SettingsViewModel(fakeRepo, fakeMonitor, fakeSession)

        val state = viewModel.uiState.value
        assertEquals("minow.rpi", state.config?.host)
        assertEquals("oldPassword", state.smbPasswordInput)
        assertTrue(state.monitorEnabled)
        assertTrue(state.hasNotificationPermission)
        assertTrue(state.fingerprint.startsWith("SHA256:"))
    }

    @Test
    fun `SettingsAction UpdateSmbPassword updates uiState and persists password in repository`() = runTest {
        val config = ServerConfig(host = "minow.rpi", port = 22, username = "minow", smbPassword = "oldPassword")
        val fakeRepo = FakeConfigRepository(initialConfig = config)
        val fakeMonitor = FakeServerMonitor()
        val fakeSession = FakeSSHSessionManager()

        val viewModel = SettingsViewModel(fakeRepo, fakeMonitor, fakeSession)

        viewModel.onAction(SettingsAction.UpdateSmbPassword("newSecretPassword123"))

        assertEquals("newSecretPassword123", viewModel.uiState.value.smbPasswordInput)
        assertEquals("newSecretPassword123", fakeRepo.loadConfig()?.smbPassword)
    }

    @Test
    fun `SettingsAction ToggleMonitor toggles monitor in service and uiState`() = runTest {
        val fakeRepo = FakeConfigRepository()
        val fakeMonitor = FakeServerMonitor(monitorEnabled = true)
        val fakeSession = FakeSSHSessionManager()

        val viewModel = SettingsViewModel(fakeRepo, fakeMonitor, fakeSession)

        viewModel.onAction(SettingsAction.ToggleMonitor(false))

        assertFalse(viewModel.uiState.value.monitorEnabled)
        assertFalse(fakeMonitor.isEnabled())
        assertTrue(fakeMonitor.cancelCalled)
    }

    @Test
    fun `SettingsAction SetResetDialogOpen changes dialog visibility`() = runTest {
        val fakeRepo = FakeConfigRepository()
        val fakeMonitor = FakeServerMonitor()
        val fakeSession = FakeSSHSessionManager()

        val viewModel = SettingsViewModel(fakeRepo, fakeMonitor, fakeSession)

        assertFalse(viewModel.uiState.value.isResetDialogOpen)

        viewModel.onAction(SettingsAction.SetResetDialogOpen(true))
        assertTrue(viewModel.uiState.value.isResetDialogOpen)

        viewModel.onAction(SettingsAction.SetResetDialogOpen(false))
        assertFalse(viewModel.uiState.value.isResetDialogOpen)
    }

    @Test
    fun `SettingsAction ResetConfiguration disconnects session, resets repository and invokes callback`() = runTest {
        val config = ServerConfig(host = "minow.rpi", port = 22, username = "minow")
        val fakeRepo = FakeConfigRepository(initialConfig = config, initialPrivateKey = "key")
        val fakeMonitor = FakeServerMonitor()
        val fakeSession = FakeSSHSessionManager(initialConnected = true)

        val viewModel = SettingsViewModel(fakeRepo, fakeMonitor, fakeSession)
        viewModel.onAction(SettingsAction.SetResetDialogOpen(true))

        var resetCallbackInvoked = false
        viewModel.onAction(SettingsAction.ResetConfiguration {
            resetCallbackInvoked = true
        })

        assertTrue(fakeSession.disconnectCalled)
        assertNull(fakeRepo.loadConfig())
        assertFalse(fakeRepo.isConfigured())
        assertFalse(viewModel.uiState.value.isResetDialogOpen)
        assertTrue(resetCallbackInvoked)
    }

    @Test
    fun `SettingsAction SendTestNotification sends notification and sets status message`() = runTest {
        val fakeRepo = FakeConfigRepository()
        val fakeMonitor = FakeServerMonitor(hasPermission = true)
        val fakeSession = FakeSSHSessionManager()

        val viewModel = SettingsViewModel(fakeRepo, fakeMonitor, fakeSession)

        viewModel.onAction(SettingsAction.SendTestNotification)

        assertTrue(fakeMonitor.testNotificationSent)
        assertTrue(viewModel.uiState.value.statusMessage?.contains("Wysłano powiadomienie testowe") == true)

        viewModel.onAction(SettingsAction.ClearStatusMessage)
        assertNull(viewModel.uiState.value.statusMessage)
    }
}
