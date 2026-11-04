package com.example.mobile_smart_ssh.network.ssh

import com.example.mobile_smart_ssh.core.model.ProcessInfo
import com.example.mobile_smart_ssh.core.model.SystemResources

/**
 * Parser odpowiedzi tekstowych z komend diagnostycznych Raspberry Pi.
 */
object SystemResourcesParser {

    fun parse(output: String, username: String): SystemResources {
        val parts = output.split("---DELIM_")
        var uptimeStr = "Brak danych"
        var diskTotal = "0 GB"
        var diskUsed = "0 GB"
        var diskFree = "0 GB"
        var diskPercentage = 0f
        var ramUsed = "0 MB"
        var ramTotal = "0 MB"
        var ramPercentage = 0f
        var cpuSum = 0f
        var quotaUsed = "0 B"
        var quotaLimit = "Brak limitu"
        var quotaPercentage = 0f
        val processes = mutableListOf<ProcessInfo>()

        for (part in parts) {
            val trimmed = part.trim()
            when {
                trimmed.startsWith("DF---") -> {
                    val lines = trimmed.removePrefix("DF---").trim().lines()
                    if (lines.size >= 2) {
                        val row = lines[1].split("\\s+".toRegex())
                        if (row.size >= 5) {
                            diskTotal = row[1]
                            diskUsed = row[2]
                            diskFree = row[3]
                            diskPercentage = row[4].trimEnd('%').toFloatOrNull() ?: 0f
                        }
                    }
                }
                trimmed.startsWith("FREE---") -> {
                    val lines = trimmed.removePrefix("FREE---").trim().lines()
                    for (line in lines) {
                        if (line.startsWith("Mem:")) {
                            val row = line.split("\\s+".toRegex())
                            if (row.size >= 4) {
                                val totalMb = row[1].toFloatOrNull() ?: 1f
                                val usedMb = row[2].toFloatOrNull() ?: 0f
                                ramTotal = "${totalMb.toInt()} MB"
                                ramUsed = "${usedMb.toInt()} MB"
                                ramPercentage = (usedMb / totalMb * 100f).coerceIn(0f, 100f)
                            }
                        }
                    }
                }
                trimmed.startsWith("PS---") -> {
                    val lines = trimmed.removePrefix("PS---").trim().lines()
                    for (i in 1 until lines.size) {
                        val row = lines[i].trim().split("\\s+".toRegex())
                        if (row.size >= 11) {
                            val procUser = row[0]
                            val pid = row[1]
                            val cpu = row[2].toFloatOrNull() ?: 0f
                            val mem = row[3].toFloatOrNull() ?: 0f
                            val cmd = row.subList(10, row.size).joinToString(" ")
                            processes.add(ProcessInfo(procUser, pid, cpu, mem, cmd))
                            cpuSum += cpu
                        }
                    }
                }
                trimmed.startsWith("QUOTA---") -> {
                    val quotaBody = trimmed.removePrefix("QUOTA---").trim()
                    if (quotaBody.contains("Disk quotas for user") || quotaBody.lines().size > 2) {
                        val lines = quotaBody.lines()
                        for (line in lines) {
                            if (line.contains("/") || line.contains("sda") || line.contains("mmcblk")) {
                                val cols = line.trim().split("\\s+".toRegex())
                                if (cols.size >= 4) {
                                    val usedBlocks = cols[1].replace("*", "")
                                    val quotaSoft = cols[2]
                                    quotaUsed = "$usedBlocks KB"
                                    quotaLimit = if (quotaSoft == "0") "Bez limitu" else "$quotaSoft KB"
                                    val usedVal = usedBlocks.toFloatOrNull() ?: 0f
                                    val limVal = quotaSoft.toFloatOrNull() ?: 0f
                                    if (limVal > 0) {
                                        quotaPercentage = (usedVal / limVal * 100f).coerceIn(0f, 100f)
                                    }
                                }
                            }
                        }
                    } else if (quotaBody.isNotEmpty() && quotaBody != "Brak danych") {
                        val cols = quotaBody.split("\\s+".toRegex())
                        if (cols.isNotEmpty()) {
                            quotaUsed = cols[0]
                            quotaLimit = "Katalog domowy (~$quotaUsed)"
                        }
                    }
                }
                else -> {
                    if (trimmed.isNotEmpty() && !trimmed.startsWith("---")) {
                        uptimeStr = trimmed.lines().firstOrNull() ?: trimmed
                    }
                }
            }
        }

        return SystemResources(
            cpuPercentage = cpuSum.coerceIn(0f, 100f),
            ramUsed = ramUsed,
            ramTotal = ramTotal,
            ramPercentage = ramPercentage,
            diskUsed = diskUsed,
            diskTotal = diskTotal,
            diskPercentage = diskPercentage,
            diskFree = diskFree,
            userQuotaUsed = quotaUsed,
            userQuotaLimit = quotaLimit,
            userQuotaPercentage = quotaPercentage,
            topProcesses = processes,
            uptime = uptimeStr,
            rawOutput = output
        )
    }
}
