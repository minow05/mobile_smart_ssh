package com.example.mobile_smart_ssh

import com.example.mobile_smart_ssh.core.model.ServerConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ServerConfigTest {

    @Test
    fun testServerConfigDefaultValues() {
        val config = ServerConfig()

        assertEquals("172.30.0.1", config.host)
        assertEquals("minow", config.username)
        assertEquals(22, config.port)
        assertEquals("", config.smbPassword)
        assertFalse(config.hasKey)
        assertEquals("minow@172.30.0.1:22", config.connectionString)
        assertTrue(config.isZeroTierIp)
    }

    @Test
    fun testServerConfigCustomValues() {
        val config = ServerConfig(
            host = "minow.rpi",
            port = 2222,
            username = "rachel",
            smbPassword = "secretPassword",
            hasKey = true
        )

        assertEquals("minow.rpi", config.host)
        assertEquals(2222, config.port)
        assertEquals("rachel", config.username)
        assertEquals("secretPassword", config.smbPassword)
        assertTrue(config.hasKey)
        assertEquals("rachel@minow.rpi:2222", config.connectionString)
        assertFalse(config.isZeroTierIp)
    }
}
