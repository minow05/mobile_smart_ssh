package com.example.mobile_smart_ssh

import com.example.mobile_smart_ssh.core.model.ServerConfig
import com.example.mobile_smart_ssh.fakes.FakeConfigRepository
import com.example.mobile_smart_ssh.fakes.MainDispatcherRule
import com.example.mobile_smart_ssh.ui.resources.ResourceMonitorAction
import com.example.mobile_smart_ssh.ui.resources.ResourceMonitorViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
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

        val viewModel = ResourceMonitorViewModel(fakeRepo)

        assertNotNull(viewModel.uiState.value.error)
        assertTrue(viewModel.uiState.value.error?.contains("Brak konfiguracji") == true)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `ResourceMonitorAction Refresh with unconfigured repository keeps error in state`() = runTest {
        val fakeRepo = FakeConfigRepository(initialConfig = null)

        val viewModel = ResourceMonitorViewModel(fakeRepo)
        viewModel.onAction(ResourceMonitorAction.Refresh)

        assertNotNull(viewModel.uiState.value.error)
        assertFalse(viewModel.uiState.value.isLoading)
    }
}
