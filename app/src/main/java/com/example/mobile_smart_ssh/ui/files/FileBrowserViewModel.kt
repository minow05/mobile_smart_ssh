package com.example.mobile_smart_ssh.ui.files

import android.app.Application
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.mobile_smart_ssh.core.model.RemoteFileItem
import com.example.mobile_smart_ssh.data.repository.ConfigRepository
import com.example.mobile_smart_ssh.network.smb.SMBStorageManager
import com.example.mobile_smart_ssh.network.ssh.SSHManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

enum class StorageDrive {
    HOME,   // [minow]
    SHARED  // [shared]
}

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

class FileBrowserViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ConfigRepository.getInstance(application)

    private val _uiState = MutableStateFlow(FileBrowserUiState())
    val uiState: StateFlow<FileBrowserUiState> = _uiState.asStateFlow()

    init {
        loadDirectory("")
    }

    fun selectDrive(drive: StorageDrive) {
        if (_uiState.value.currentDrive != drive) {
            _uiState.update { it.copy(currentDrive = drive, currentPath = "") }
            loadDirectory("")
        }
    }

    fun setNewDirName(name: String) {
        _uiState.update { it.copy(newDirName = name) }
    }

    fun setCreateDirDialogVisible(visible: Boolean) {
        _uiState.update { it.copy(isCreateDirDialogVisible = visible, newDirName = "") }
    }

    fun selectItemForAction(item: RemoteFileItem?) {
        _uiState.update { it.copy(selectedItemForAction = item) }
    }

    fun clearStatusMessage() {
        _uiState.update { it.copy(statusMessage = null, error = null) }
    }

    private fun resolveSftpPath(drive: StorageDrive, relativePath: String, user: String): String {
        val root = when (drive) {
            StorageDrive.HOME -> "/home/$user"
            StorageDrive.SHARED -> "/home/$user/shared"
        }
        return if (relativePath.isBlank()) root else "$root/$relativePath"
    }

    /**
     * Główna funkcja pobierania zawartości katalogu:
     * 1. Zgodnie ze specyfikacją domyślnie używa protokołu Samba (SMB/CIFS) dla [minow] i [shared].
     * 2. Jeśli Samba nie jest dostępna, automatycznie przełącza się na SFTP (fallback).
     */
    fun loadDirectory(relativePath: String = _uiState.value.currentPath) {
        val config = repository.loadConfig() ?: return
        val privateKey = repository.getPrivateKey() ?: ""

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, currentPath = relativePath, error = null) }

            val drive = _uiState.value.currentDrive
            val shareName = when (drive) {
                StorageDrive.HOME -> config.username
                StorageDrive.SHARED -> "shared"
            }

            // 1. Próba pobrania listy plików przez Samba (SMB)
            val smbResult = SMBStorageManager.listFiles(
                host = config.host,
                shareName = shareName,
                folderPath = relativePath,
                username = config.username,
                password = config.smbPassword
            )

            if (smbResult.isSuccess) {
                val list = smbResult.getOrNull() ?: emptyList()
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        items = list,
                        activeProtocol = "Samba (SMB)"
                    )
                }
                return@launch
            }

            // 2. Automatyczny Fallback na SFTP, jeśli połączenie SMB nie powiodło się
            Log.w(TAG, "Samba (SMB) niedostępna, próba pobrania przez SFTP fallback: ${smbResult.exceptionOrNull()?.message}")
            val fullPath = resolveSftpPath(drive, relativePath, config.username)
            val sftpResult = SSHManager.listSftpFiles(
                host = config.host,
                port = config.port,
                username = config.username,
                privateKeyPem = privateKey,
                remotePath = fullPath
            )

            sftpResult.fold(
                onSuccess = { list ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            items = list,
                            activeProtocol = "SFTP (Fallback)"
                        )
                    }
                },
                onFailure = { err ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = "Błąd pobierania zasobów (Samba & SFTP): ${err.message}"
                        )
                    }
                }
            )
        }
    }

    fun navigateIntoFolder(item: RemoteFileItem) {
        if (!item.isDirectory) return
        val current = _uiState.value.currentPath
        val next = if (current.isEmpty()) item.name else "$current/${item.name}"
        loadDirectory(next)
    }

    fun navigateUp() {
        val current = _uiState.value.currentPath
        if (current.isEmpty()) return
        val parts = current.split("/").filter { it.isNotEmpty() }
        val up = if (parts.size <= 1) "" else parts.dropLast(1).joinToString("/")
        loadDirectory(up)
    }

    fun createDirectory(dirName: String) {
        val trimmed = dirName.trim()
        if (trimmed.isEmpty()) return

        val config = repository.loadConfig() ?: return
        val privateKey = repository.getPrivateKey() ?: ""

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, isCreateDirDialogVisible = false) }
            val current = _uiState.value.currentPath
            val newRelative = if (current.isEmpty()) trimmed else "$current/$trimmed"

            val shareName = when (_uiState.value.currentDrive) {
                StorageDrive.HOME -> config.username
                StorageDrive.SHARED -> "shared"
            }

            // Próba SMB
            val smbSuccess = SMBStorageManager.createDirectory(
                host = config.host,
                shareName = shareName,
                remotePath = newRelative,
                username = config.username,
                password = config.smbPassword
            ).isSuccess

            val success = if (smbSuccess) {
                true
            } else {
                // Fallback SFTP
                val fullPath = resolveSftpPath(_uiState.value.currentDrive, newRelative, config.username)
                SSHManager.createSftpDirectory(
                    host = config.host,
                    port = config.port,
                    username = config.username,
                    privateKeyPem = privateKey,
                    remotePath = fullPath
                ).isSuccess
            }

            if (success) {
                _uiState.update { it.copy(statusMessage = "Utworzono katalog '$trimmed'") }
                loadDirectory()
            } else {
                _uiState.update { it.copy(isLoading = false, error = "Nie udało się utworzyć katalogu") }
            }
        }
    }

    fun deleteItem(item: RemoteFileItem) {
        val config = repository.loadConfig() ?: return
        val privateKey = repository.getPrivateKey() ?: ""

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, selectedItemForAction = null) }

            val shareName = when (_uiState.value.currentDrive) {
                StorageDrive.HOME -> config.username
                StorageDrive.SHARED -> "shared"
            }

            val smbSuccess = SMBStorageManager.deleteFile(
                host = config.host,
                shareName = shareName,
                remotePath = item.path,
                username = config.username,
                password = config.smbPassword,
                isDirectory = item.isDirectory
            ).isSuccess

            val success = if (smbSuccess) {
                true
            } else {
                SSHManager.deleteSftpFile(
                    host = config.host,
                    port = config.port,
                    username = config.username,
                    privateKeyPem = privateKey,
                    remotePath = item.path,
                    isDirectory = item.isDirectory
                ).isSuccess
            }

            if (success) {
                _uiState.update { it.copy(statusMessage = "Usunięto '${item.name}'") }
                loadDirectory()
            } else {
                _uiState.update { it.copy(isLoading = false, error = "Błąd usuwania '${item.name}'") }
            }
        }
    }

    fun downloadFileToDevice(item: RemoteFileItem, context: Context) {
        val config = repository.loadConfig() ?: return
        val privateKey = repository.getPrivateKey() ?: ""

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, selectedItemForAction = null) }

            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                ?: context.filesDir
            val destFile = File(downloadsDir, item.name)

            val shareName = when (_uiState.value.currentDrive) {
                StorageDrive.HOME -> config.username
                StorageDrive.SHARED -> "shared"
            }

            val result = withContext(Dispatchers.IO) {
                try {
                    val out = FileOutputStream(destFile)
                    out.use { stream ->
                        val smbResult = SMBStorageManager.downloadFile(
                            host = config.host,
                            shareName = shareName,
                            remotePath = item.path,
                            username = config.username,
                            password = config.smbPassword,
                            destinationStream = stream
                        )

                        if (smbResult.isSuccess) {
                            Result.success(Unit)
                        } else {
                            // Fallback SFTP
                            SSHManager.downloadSftpFile(
                                host = config.host,
                                port = config.port,
                                username = config.username,
                                privateKeyPem = privateKey,
                                remotePath = item.path,
                                destinationStream = stream
                            )
                        }
                    }
                } catch (e: Exception) {
                    Result.failure(e)
                }
            }

            result.fold(
                onSuccess = {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            statusMessage = "Pobrano plik '${item.name}' do Pobrane: ${destFile.absolutePath}"
                        )
                    }
                },
                onFailure = { err ->
                    _uiState.update {
                        it.copy(isLoading = false, error = "Błąd pobierania pliku: ${err.message}")
                    }
                }
            )
        }
    }

    fun uploadFileFromDevice(uri: Uri, context: Context) {
        val config = repository.loadConfig() ?: return
        val privateKey = repository.getPrivateKey() ?: ""

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val fileName = queryFileName(uri, context) ?: "uploaded_${System.currentTimeMillis()}"
            val current = _uiState.value.currentPath
            val destRelative = if (current.isEmpty()) fileName else "$current/$fileName"

            val shareName = when (_uiState.value.currentDrive) {
                StorageDrive.HOME -> config.username
                StorageDrive.SHARED -> "shared"
            }

            val result = withContext(Dispatchers.IO) {
                try {
                    context.contentResolver.openInputStream(uri)?.use { inStream ->
                        val smbRes = SMBStorageManager.uploadFile(
                            host = config.host,
                            shareName = shareName,
                            remotePath = destRelative,
                            username = config.username,
                            password = config.smbPassword,
                            sourceStream = inStream
                        )

                        if (smbRes.isSuccess) {
                            Result.success(Unit)
                        } else {
                            // Fallback SFTP: otwórz strumień ponownie dla SFTP
                            context.contentResolver.openInputStream(uri)?.use { sftpInStream ->
                                val fullPath = resolveSftpPath(_uiState.value.currentDrive, destRelative, config.username)
                                SSHManager.uploadSftpFile(
                                    host = config.host,
                                    port = config.port,
                                    username = config.username,
                                    privateKeyPem = privateKey,
                                    sourceStream = sftpInStream,
                                    remotePath = fullPath
                                )
                            } ?: Result.failure(Exception("Nie można otworzyć pliku"))
                        }
                    } ?: Result.failure(Exception("Nie można otworzyć strumienia pliku z urządzenia"))
                } catch (e: Exception) {
                    Result.failure(e)
                }
            }

            result.fold(
                onSuccess = {
                    _uiState.update { it.copy(statusMessage = "Przesłano '$fileName' na serwer") }
                    loadDirectory()
                },
                onFailure = { err ->
                    _uiState.update { it.copy(isLoading = false, error = "Błąd wysyłania: ${err.message}") }
                }
            )
        }
    }

    private fun queryFileName(uri: Uri, context: Context): String? {
        var name: String? = null
        if (uri.scheme == "content") {
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val idx = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (idx >= 0) name = it.getString(idx)
                }
            }
        }
        if (name == null) {
            name = uri.path
            val cut = name?.lastIndexOf('/')
            if (cut != null && cut != -1) {
                name = name?.substring(cut + 1)
            }
        }
        return name
    }

    companion object {
        private const val TAG = "FileBrowserViewModel"
    }
}
