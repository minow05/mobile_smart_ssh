package com.example.mobile_smart_ssh.data.repository

import android.content.Context
import com.example.mobile_smart_ssh.core.model.ServerConfig
import kotlinx.coroutines.flow.StateFlow

/**
 * Interfejs repozytorium konfiguracji powiązanego hosta (Single-Host Binding Model).
 * Umożliwia pełną izolację warstwy danych od widoków i łatwe mockowanie/testowanie jednostkowe.
 */
interface ConfigRepository {
    val configState: StateFlow<ServerConfig?>

    fun isConfigured(): Boolean
    fun loadConfig(): ServerConfig?
    fun saveConfig(config: ServerConfig)
    fun savePrivateKey(pem: String)
    fun getPrivateKey(): String?
    fun savePublicKey(publicKey: String)
    fun getPublicKey(): String?
    fun saveSmbPassword(password: String)
    fun resetConfiguration()

    companion object {
        fun getInstance(context: Context): ConfigRepository =
            EncryptedConfigRepository.getInstance(context)
    }
}
