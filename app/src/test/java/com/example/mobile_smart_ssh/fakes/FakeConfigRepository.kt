package com.example.mobile_smart_ssh.fakes

import com.example.mobile_smart_ssh.core.model.ServerConfig
import com.example.mobile_smart_ssh.data.repository.ConfigRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class FakeConfigRepository(
    initialConfig: ServerConfig? = null,
    initialPrivateKey: String? = null,
    initialPublicKey: String? = null
) : ConfigRepository {

    private var currentConfig: ServerConfig? = initialConfig
    private var privateKey: String? = initialPrivateKey
    private var publicKey: String? = initialPublicKey

    private val _configState = MutableStateFlow(initialConfig)
    override val configState: StateFlow<ServerConfig?> = _configState.asStateFlow()

    override fun isConfigured(): Boolean =
        currentConfig != null && !privateKey.isNullOrBlank()

    override fun loadConfig(): ServerConfig? = currentConfig

    override fun saveConfig(config: ServerConfig) {
        currentConfig = config
        _configState.value = config
    }

    override fun savePrivateKey(pem: String) {
        privateKey = pem
        currentConfig = currentConfig?.copy(hasKey = true)
        _configState.value = currentConfig
    }

    override fun getPrivateKey(): String? = privateKey

    override fun savePublicKey(publicKey: String) {
        this.publicKey = publicKey
    }

    override fun getPublicKey(): String? = publicKey

    override fun saveSmbPassword(password: String) {
        currentConfig = currentConfig?.copy(smbPassword = password)
        _configState.value = currentConfig
    }

    override fun resetConfiguration() {
        currentConfig = null
        privateKey = null
        publicKey = null
        _configState.value = null
    }
}
