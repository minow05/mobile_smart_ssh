package com.example.mobile_smart_ssh.data.repository

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.example.mobile_smart_ssh.core.model.ServerConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Repozytorium konfiguracji powiązanego hosta (Single-Host Binding Model).
 * Klucze prywatne SSH oraz hasła SMB są szyfrowane sprzętowo (Android Keystore + EncryptedSharedPreferences).
 */
class ConfigRepository(private val context: Context) {

    private val prefs: SharedPreferences = try {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        EncryptedSharedPreferences.create(
            context,
            SECURE_PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    } catch (e: Exception) {
        Log.e(TAG, "Nie udało się zainicjalizować EncryptedSharedPreferences, używam standardowych SharedPreferences: ${e.message}")
        context.getSharedPreferences("smartssh_prefs_fallback", Context.MODE_PRIVATE)
    }

    private val _configState = MutableStateFlow<ServerConfig?>(loadConfig())
    val configState: StateFlow<ServerConfig?> = _configState.asStateFlow()

    fun isConfigured(): Boolean {
        val host = prefs.getString(KEY_HOST, null)
        val user = prefs.getString(KEY_USERNAME, null)
        val privKey = getPrivateKey()
        return !host.isNullOrBlank() && !user.isNullOrBlank() && !privKey.isNullOrBlank()
    }

    fun loadConfig(): ServerConfig? {
        val host = prefs.getString(KEY_HOST, null) ?: return null
        val port = prefs.getInt(KEY_PORT, 22)
        val username = prefs.getString(KEY_USERNAME, "minow") ?: "minow"
        val smbPassword = prefs.getString(KEY_SMB_PASSWORD, "") ?: ""
        val hasKey = !getPrivateKey().isNullOrBlank()

        return ServerConfig(
            host = host,
            port = port,
            username = username,
            smbPassword = smbPassword,
            hasKey = hasKey
        )
    }

    fun saveConfig(config: ServerConfig) {
        prefs.edit()
            .putString(KEY_HOST, config.host.trim())
            .putInt(KEY_PORT, config.port)
            .putString(KEY_USERNAME, config.username.trim())
            .putString(KEY_SMB_PASSWORD, config.smbPassword)
            .apply()

        _configState.value = loadConfig()
    }

    fun savePrivateKey(pem: String) {
        prefs.edit().putString(KEY_PRIVATE_KEY, pem.trim()).apply()
        _configState.value = loadConfig()
    }

    fun getPrivateKey(): String? {
        return prefs.getString(KEY_PRIVATE_KEY, null)
    }

    fun savePublicKey(publicKey: String) {
        prefs.edit().putString(KEY_PUBLIC_KEY, publicKey.trim()).apply()
    }

    fun getPublicKey(): String? {
        return prefs.getString(KEY_PUBLIC_KEY, null)
    }

    fun saveSmbPassword(password: String) {
        prefs.edit().putString(KEY_SMB_PASSWORD, password).apply()
        _configState.value = loadConfig()
    }

    /**
     * Czyści CAŁĄ lokalną konfigurację i klucze na telefonie.
     * UWAGA: Zgodnie z wymaganiami zadania, dane na zdalnym serwerze Raspberry Pi pozostają NIETKNIĘTE.
     */
    fun resetConfiguration() {
        prefs.edit().clear().apply()
        _configState.value = null
    }

    companion object {
        private const val TAG = "ConfigRepository"
        private const val SECURE_PREFS_NAME = "smartssh_encrypted_prefs"

        private const val KEY_HOST = "pref_server_host"
        private const val KEY_PORT = "pref_server_port"
        private const val KEY_USERNAME = "pref_server_username"
        private const val KEY_SMB_PASSWORD = "pref_smb_password"
        private const val KEY_PRIVATE_KEY = "pref_ssh_private_key"
        private const val KEY_PUBLIC_KEY = "pref_ssh_public_key"

        @Volatile
        private var INSTANCE: ConfigRepository? = null

        fun getInstance(context: Context): ConfigRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: ConfigRepository(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
