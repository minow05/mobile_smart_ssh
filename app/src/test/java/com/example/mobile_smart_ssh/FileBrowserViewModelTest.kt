package com.example.mobile_smart_ssh

import com.example.mobile_smart_ssh.core.model.RemoteFileItem
import com.example.mobile_smart_ssh.core.model.ServerConfig
import com.example.mobile_smart_ssh.fakes.FakeConfigRepository
import com.example.mobile_smart_ssh.fakes.FakeSSHSessionManager
import com.example.mobile_smart_ssh.fakes.MainDispatcherRule
import com.example.mobile_smart_ssh.ui.files.FileBrowserAction
import com.example.mobile_smart_ssh.ui.files.FileBrowserViewModel
import com.example.mobile_smart_ssh.ui.files.StorageDrive
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
class FileBrowserViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `init starts on HOME drive and empty current path`() = runTest {
        val fakeRepo = FakeConfigRepository()
        val fakeSession = FakeSSHSessionManager()

        val viewModel = FileBrowserViewModel(fakeRepo, fakeSession)

        assertEquals(StorageDrive.HOME, viewModel.uiState.value.currentDrive)
        assertEquals("", viewModel.uiState.value.currentPath)
    }

    @Test
    fun `FileBrowserAction SelectDrive switches drive and resets path`() = runTest {
        val fakeRepo = FakeConfigRepository()
        val fakeSession = FakeSSHSessionManager()

        val viewModel = FileBrowserViewModel(fakeRepo, fakeSession)
        viewModel.onAction(FileBrowserAction.SelectDrive(StorageDrive.SHARED))

        assertEquals(StorageDrive.SHARED, viewModel.uiState.value.currentDrive)
        assertEquals("", viewModel.uiState.value.currentPath)
    }

    @Test
    fun `FileBrowserAction SetNewDirName updates directory name`() = runTest {
        val fakeRepo = FakeConfigRepository()
        val fakeSession = FakeSSHSessionManager()

        val viewModel = FileBrowserViewModel(fakeRepo, fakeSession)
        viewModel.onAction(FileBrowserAction.SetNewDirName("my_new_folder"))

        assertEquals("my_new_folder", viewModel.uiState.value.newDirName)
    }

    @Test
    fun `FileBrowserAction SetCreateDirDialogVisible toggles dialog and resets newDirName`() = runTest {
        val fakeRepo = FakeConfigRepository()
        val fakeSession = FakeSSHSessionManager()

        val viewModel = FileBrowserViewModel(fakeRepo, fakeSession)
        viewModel.onAction(FileBrowserAction.SetNewDirName("temp"))
        viewModel.onAction(FileBrowserAction.SetCreateDirDialogVisible(true))

        assertTrue(viewModel.uiState.value.isCreateDirDialogVisible)
        assertEquals("", viewModel.uiState.value.newDirName)

        viewModel.onAction(FileBrowserAction.SetCreateDirDialogVisible(false))
        assertFalse(viewModel.uiState.value.isCreateDirDialogVisible)
    }

    @Test
    fun `FileBrowserAction SelectItemForAction sets and clears active file item`() = runTest {
        val fakeRepo = FakeConfigRepository()
        val fakeSession = FakeSSHSessionManager()
        val item = RemoteFileItem(name = "test.txt", path = "test.txt", isDirectory = false, size = 100, lastModified = 0)

        val viewModel = FileBrowserViewModel(fakeRepo, fakeSession)
        viewModel.onAction(FileBrowserAction.SelectItemForAction(item))

        assertEquals("test.txt", viewModel.uiState.value.selectedItemForAction?.name)

        viewModel.onAction(FileBrowserAction.SelectItemForAction(null))
        assertNull(viewModel.uiState.value.selectedItemForAction)
    }

    @Test
    fun `FileBrowserAction ClearStatusMessage clears status and error in state`() = runTest {
        val fakeRepo = FakeConfigRepository()
        val fakeSession = FakeSSHSessionManager()

        val viewModel = FileBrowserViewModel(fakeRepo, fakeSession)
        viewModel.onAction(FileBrowserAction.ClearStatusMessage)

        assertNull(viewModel.uiState.value.statusMessage)
        assertNull(viewModel.uiState.value.error)
    }
}
