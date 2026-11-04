package com.example.mobile_smart_ssh

import com.example.mobile_smart_ssh.network.ssh.SystemResourcesParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SystemResourcesParserTest {

    @Test
    fun testParseFullSystemResourcesOutput() {
        val mockOutput = """
             14:17:57 up 28 days, 23:10,  2 users,  load average: 0.08, 0.04, 0.00
            ---DELIM_DF---
            Filesystem      Size  Used Avail Use% Mounted on
            /dev/mmcblk0p2  117G   33G   80G  29% /
            ---DELIM_FREE---
                           total        used        free      shared  buff/cache   available
            Mem:            7934         632         263          10        7243        7301
            Swap:              0           0           0
            ---DELIM_PS---
            USER         PID %CPU %MEM    VSZ   RSS TTY      STAT START   TIME COMMAND
            root      170478  2.3  0.1  20528 10404 ?        Ss   14:17   0:00 sshd-session: minow [priv]
            zerotie+     972  1.5  0.2 593756 24104 ?        Ssl  Oct06  31:18 /usr/sbin/zerotier-one
            root         975  0.2  0.5 1480168 47968 ?       Ssl  Oct06   9:12 /usr/bin/containerd
            ---DELIM_QUOTA---
            26G	/home/minow
        """.trimIndent()

        val result = SystemResourcesParser.parse(mockOutput, "minow")

        assertNotNull(result)
        assertTrue(result.uptime.contains("up 28 days"))

        // Dysk
        assertEquals("117G", result.diskTotal)
        assertEquals("33G", result.diskUsed)
        assertEquals("80G", result.diskFree)
        assertEquals(29f, result.diskPercentage, 0.1f)

        // RAM
        assertEquals("7934 MB", result.ramTotal)
        assertEquals("632 MB", result.ramUsed)
        val expectedRamPercentage = (632f / 7934f) * 100f
        assertEquals(expectedRamPercentage, result.ramPercentage, 0.1f)

        // Procesy
        assertEquals(3, result.topProcesses.size)
        assertEquals("170478", result.topProcesses[0].pid)
        assertEquals(2.3f, result.topProcesses[0].cpuPercentage, 0.01f)
        assertEquals(4.0f, result.cpuPercentage, 0.1f) // 2.3 + 1.5 + 0.2

        // Kwota
        assertEquals("26G", result.userQuotaUsed)
        assertEquals("Katalog domowy (~26G)", result.userQuotaLimit)
    }

    @Test
    fun testParseQuotaBlocksFormat() {
        val mockQuotaOutput = """
            uptime info
            ---DELIM_DF---
            Filesystem 10G 5G 5G 50% /
            ---DELIM_FREE---
            Mem: 1000 500 500 0
            ---DELIM_PS---
            ---DELIM_QUOTA---
            Disk quotas for user minow (uid 1000): 
                 Filesystem  blocks   quota   limit   grace   files   quota   limit   grace
             /dev/mmcblk0p2 500000  1000000 1200000               0       0       0        
        """.trimIndent()

        val result = SystemResourcesParser.parse(mockQuotaOutput, "minow")

        assertEquals("500000 KB", result.userQuotaUsed)
        assertEquals("1000000 KB", result.userQuotaLimit)
        assertEquals(50f, result.userQuotaPercentage, 0.1f)
    }

    @Test
    fun testParseEmptyOutputHandlesGracefully() {
        val result = SystemResourcesParser.parse("", "minow")

        assertEquals("0 GB", result.diskTotal)
        assertEquals("0 MB", result.ramTotal)
        assertEquals(0f, result.cpuPercentage, 0.01f)
        assertTrue(result.topProcesses.isEmpty())
    }
}
