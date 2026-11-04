package com.example.mobile_smart_ssh.ui.files

import android.content.Context
import android.net.Uri
import com.example.mobile_smart_ssh.core.model.RemoteFileItem

sealed interface FileBrowserAction {
    data class SelectDrive(val drive: StorageDrive) : FileBrowserAction
    data class SetNewDirName(val name: String) : FileBrowserAction
    data class SetCreateDirDialogVisible(val visible: Boolean) : FileBrowserAction
    data class SelectItemForAction(val item: RemoteFileItem?) : FileBrowserAction
    data object ClearStatusMessage : FileBrowserAction
    data class LoadDirectory(val relativePath: String? = null) : FileBrowserAction
    data class NavigateIntoFolder(val item: RemoteFileItem) : FileBrowserAction
    data object NavigateUp : FileBrowserAction
    data class CreateDirectory(val dirName: String) : FileBrowserAction
    data class DeleteItem(val item: RemoteFileItem) : FileBrowserAction
    data class DownloadFile(val item: RemoteFileItem, val context: Context) : FileBrowserAction
    data class UploadFile(val uri: Uri, val context: Context) : FileBrowserAction
}
