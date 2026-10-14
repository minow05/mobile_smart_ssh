package com.example.mobile_smart_ssh.core.model

/**
 * Informacje o pojedynczym procesie w systemie Raspberry Pi.
 */
data class ProcessInfo(
    val user: String,
    val pid: String,
    val cpuPercentage: Float,
    val memPercentage: Float,
    val command: String
)

/**
 * Model metryk zużycia zasobów sprzętowych i kwot dyskowych Raspberry Pi.
 */
data class SystemResources(
    val cpuPercentage: Float = 0f,
    val ramUsed: String = "0 B",
    val ramTotal: String = "0 B",
    val ramPercentage: Float = 0f,
    val diskUsed: String = "0 B",
    val diskTotal: String = "0 B",
    val diskPercentage: Float = 0f,
    val diskFree: String = "0 B",
    val userQuotaUsed: String = "0 B",
    val userQuotaLimit: String = "Brak limitu",
    val userQuotaPercentage: Float = 0f,
    val topProcesses: List<ProcessInfo> = emptyList(),
    val uptime: String = "Niedostępny",
    val rawOutput: String = ""
)
