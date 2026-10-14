package com.example.mobile_smart_ssh.core.model

/**
 * Model konfiguracji pojedynczego hosta Raspberry Pi (Single-Host Binding Model).
 * Przechowuje parametry sieciowe i uwierzytelniania powiązanego serwera.
 */
data class ServerConfig(
    val host: String = "172.30.0.1",
    val port: Int = 22,
    val username: String = "minow",
    val smbPassword: String = "",
    val hasKey: Boolean = false
) {
    val connectionString: String
        get() = "$username@$host:$port"

    val isZeroTierIp: Boolean
        get() = host.startsWith("172.30.") || host.startsWith("10.147.") || host.startsWith("192.168.")
}
