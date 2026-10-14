package com.example.mobile_smart_ssh.network.smb

import android.util.Log
import com.example.mobile_smart_ssh.core.model.RemoteFileItem
import com.hierynomus.msdtyp.AccessMask
import com.hierynomus.msfscc.FileAttributes
import com.hierynomus.mssmb2.SMB2CreateDisposition
import com.hierynomus.mssmb2.SMB2ShareAccess
import com.hierynomus.smbj.SMBClient
import com.hierynomus.smbj.SmbConfig
import com.hierynomus.smbj.auth.AuthenticationContext
import com.hierynomus.smbj.share.DiskShare
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.OutputStream
import java.util.EnumSet

/**
 * Menadżer zasobów sieciowych Samba (SMB/CIFS) dla Raspberry Pi.
 * Obsługuje:
 * - Udział prywatny użytkownika: \\<IP>\<username> (np. /home/minow)
 * - Udział współdzielony: \\<IP>\shared (znajdujący się w /home/minow/shared)
 */
object SMBStorageManager {

    private const val TAG = "SMBStorageManager"

    private fun createClient(): SMBClient {
        val config = SmbConfig.createDefaultConfig()
        return SMBClient(config)
    }

    private inline fun <T> withDiskShare(
        host: String,
        shareName: String,
        username: String,
        password: String,
        domain: String = "",
        block: (DiskShare) -> T
    ): T {
        val client = createClient()
        val connection = client.connect(host)
        val authContext = if (password.isNotBlank()) {
            AuthenticationContext(username, password.toCharArray(), domain)
        } else {
            AuthenticationContext.anonymous()
        }
        val session = connection.authenticate(authContext)
        val share = session.connectShare(shareName) as DiskShare
        return try {
            block(share)
        } finally {
            try { share.close() } catch (_: Exception) {}
            try { session.close() } catch (_: Exception) {}
            try { connection.close() } catch (_: Exception) {}
            try { client.close() } catch (_: Exception) {}
        }
    }

    /**
     * Pobiera zawartość katalogu z udziału SMB (np. 'minow' lub 'shared').
     */
    suspend fun listFiles(
        host: String,
        shareName: String,
        folderPath: String = "",
        username: String,
        password: String
    ): Result<List<RemoteFileItem>> = withContext(Dispatchers.IO) {
        try {
            val items = withDiskShare(host, shareName, username, password) { share ->
                val normalized = folderPath.trim().replace('/', '\\').trim('\\')
                val entries = share.list(normalized)
                val resultList = mutableListOf<RemoteFileItem>()

                for (entry in entries) {
                    val name = entry.fileName
                    if (name == "." || name == "..") continue
                    val isDir = (entry.fileAttributes and FileAttributes.FILE_ATTRIBUTE_DIRECTORY.value) != 0L
                    val size = entry.endOfFile
                    val mtime = try {
                        entry.changeTime.toEpochMillis()
                    } catch (_: Exception) {
                        0L
                    }

                    val fullPath = if (normalized.isEmpty()) name else "$normalized\\$name"

                    resultList.add(
                        RemoteFileItem(
                            name = name,
                            path = fullPath.replace('\\', '/'),
                            isDirectory = isDir,
                            size = size,
                            lastModified = mtime
                        )
                    )
                }

                resultList.sortWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))
                resultList
            }
            Result.success(items)
        } catch (e: Exception) {
            Log.e(TAG, "Błąd pobierania listy plików SMB: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun downloadFile(
        host: String,
        shareName: String,
        remotePath: String,
        username: String,
        password: String,
        destinationStream: OutputStream
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            withDiskShare(host, shareName, username, password) { share ->
                val normalized = remotePath.trim().replace('/', '\\').trim('\\')
                val file = share.openFile(
                    normalized,
                    EnumSet.of(AccessMask.GENERIC_READ),
                    null,
                    SMB2ShareAccess.ALL,
                    SMB2CreateDisposition.FILE_OPEN,
                    null
                )
                file.use { f ->
                    f.inputStream.use { inStream ->
                        inStream.copyTo(destinationStream)
                    }
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun uploadFile(
        host: String,
        shareName: String,
        remotePath: String,
        username: String,
        password: String,
        sourceStream: InputStream
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            withDiskShare(host, shareName, username, password) { share ->
                val normalized = remotePath.trim().replace('/', '\\').trim('\\')
                val file = share.openFile(
                    normalized,
                    EnumSet.of(AccessMask.GENERIC_WRITE),
                    null,
                    SMB2ShareAccess.ALL,
                    SMB2CreateDisposition.FILE_OVERWRITE_IF,
                    null
                )
                file.use { f ->
                    f.outputStream.use { outStream ->
                        sourceStream.copyTo(outStream)
                    }
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteFile(
        host: String,
        shareName: String,
        remotePath: String,
        username: String,
        password: String,
        isDirectory: Boolean
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            withDiskShare(host, shareName, username, password) { share ->
                val normalized = remotePath.trim().replace('/', '\\').trim('\\')
                if (isDirectory) {
                    share.rmdir(normalized, true)
                } else {
                    share.rm(normalized)
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createDirectory(
        host: String,
        shareName: String,
        remotePath: String,
        username: String,
        password: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            withDiskShare(host, shareName, username, password) { share ->
                val normalized = remotePath.trim().replace('/', '\\').trim('\\')
                share.mkdir(normalized)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
