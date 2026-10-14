package com.example.mobile_smart_ssh.core.model

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Reprezentacja pliku lub katalogu na zdalnym serwerze Raspberry Pi.
 */
data class RemoteFileItem(
    val name: String,
    val path: String,
    val isDirectory: Boolean,
    val size: Long,
    val lastModified: Long,
    val permissions: String = ""
) {
    val formattedSize: String
        get() {
            if (isDirectory) return "<KATALOG>"
            if (size < 1024) return "$size B"
            val kb = size / 1024.0
            if (kb < 1024) return String.format(Locale.US, "%.1f KB", kb)
            val mb = kb / 1024.0
            if (mb < 1024) return String.format(Locale.US, "%.1f MB", mb)
            val gb = mb / 1024.0
            return String.format(Locale.US, "%.2f GB", gb)
        }

    val formattedDate: String
        get() {
            if (lastModified <= 0) return "-"
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
            return sdf.format(Date(lastModified))
        }
}
