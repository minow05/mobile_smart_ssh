package com.example.mobile_smart_ssh

import com.example.mobile_smart_ssh.core.model.ServerConfig
import com.example.mobile_smart_ssh.fakes.FakeConfigRepository
import com.example.mobile_smart_ssh.fakes.FakeSSHSessionManager
import com.example.mobile_smart_ssh.fakes.MainDispatcherRule
import com.example.mobile_smart_ssh.ui.wizard.WizardAction
import com.example.mobile_smart_ssh.ui.wizard.WizardStage
import com.example.mobile_smart_ssh.ui.wizard.WizardViewModel
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
class WizardViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `init pre-populates existing host and username if repository is already configured`() = runTest {
        val existing = ServerConfig(host = "10.0.0.15", port = 2222, username = "ubuntu")
        val fakeRepo = FakeConfigRepository(initialConfig = existing)
        val fakeSession = FakeSSHSessionManager()

        val viewModel = WizardViewModel(fakeRepo, fakeSession)

        assertEquals("10.0.0.15", viewModel.uiState.value.host)
        assertEquals("2222", viewModel.uiState.value.port)
        assertEquals("ubuntu", viewModel.uiState.value.username)
    }

    @Test
    fun `WizardAction UpdateHost updates host in state`() = runTest {
        val fakeRepo = FakeConfigRepository()
        val fakeSession = FakeSSHSessionManager()

        val viewModel = WizardViewModel(fakeRepo, fakeSession)
        viewModel.onAction(WizardAction.UpdateHost("raspberrypi.local"))

        assertEquals("raspberrypi.local", viewModel.uiState.value.host)
    }

    @Test
    fun `WizardAction UpdatePort filters non-numeric characters`() = runTest {
        val fakeRepo = FakeConfigRepository()
        val fakeSession = FakeSSHSessionManager()

        val viewModel = WizardViewModel(fakeRepo, fakeSession)
        viewModel.onAction(WizardAction.UpdatePort("22ab88#"))

        assertEquals("2288", viewModel.uiState.value.port)
    }

    @Test
    fun `WizardAction UpdateUsername and UpdatePassword update credentials`() = runTest {
        val fakeRepo = FakeConfigRepository()
        val fakeSession = FakeSSHSessionManager()

        val viewModel = WizardViewModel(fakeRepo, fakeSession)
        viewModel.onAction(WizardAction.UpdateUsername("admin"))
        viewModel.onAction(WizardAction.UpdatePassword("superSecret123"))

        assertEquals("admin", viewModel.uiState.value.username)
        assertEquals("superSecret123", viewModel.uiState.value.password)
    }

    @Test
    fun `WizardAction UpdateUseAsSmbPassword updates flag`() = runTest {
        val fakeRepo = FakeConfigRepository()
        val fakeSession = FakeSSHSessionManager()

        val viewModel = WizardViewModel(fakeRepo, fakeSession)
        viewModel.onAction(WizardAction.UpdateUseAsSmbPassword(false))

        assertFalse(viewModel.uiState.value.useAsSmbPassword)
    }

    @Test
    fun `WizardAction ResetToInput resets stage to INPUT and clears errors`() = runTest {
        val fakeRepo = FakeConfigRepository()
        val fakeSession = FakeSSHSessionManager()

        val viewModel = WizardViewModel(fakeRepo, fakeSession)

        // Trigger validation error by starting setup with empty fields
        viewModel.onAction(WizardAction.UpdateHost(""))
        viewModel.onAction(WizardAction.StartAutomatedSetup {})
        assertNotNull(viewModel.uiState.value.error)

        viewModel.onAction(WizardAction.ResetToInput)
        assertEquals(WizardStage.INPUT, viewModel.uiState.value.currentStage)
        assertNull(viewModel.uiState.value.error)
        assertFalse(viewModel.uiState.value.isRunning)
    }

    @Test
    fun `WizardAction StartAutomatedSetup with blank host sets validation error`() = runTest {
        val fakeRepo = FakeConfigRepository()
        val fakeSession = FakeSSHSessionManager()

        val viewModel = WizardViewModel(fakeRepo, fakeSession)
        viewModel.onAction(WizardAction.UpdateHost("   "))
        viewModel.onAction(WizardAction.StartAutomatedSetup {})

        assertTrue(viewModel.uiState.value.error?.contains("Wprowadź adres IP") == true)
        assertFalse(viewModel.uiState.value.isRunning)
    }
}
