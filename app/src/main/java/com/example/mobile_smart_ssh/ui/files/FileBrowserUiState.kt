package com.example.mobile_smart_ssh.ui.files

import com.example.mobile_smart_ssh.core.model.RemoteFileItem

data class FileBrowserUiState(
    val currentDrive: StorageDrive = StorageDrive.HOME,
    val activeProtocol: String = "Samba (SMB)",
    val currentPath: String = "",
    val items: List<RemoteFileItem> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val statusMessage: String? = null,
    val isCreateDirDialogVisible: Boolean = false,
    val newDirName: String = "",
    val selectedItemForAction: RemoteFileItem? = null
)
