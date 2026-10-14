package com.example.mobile_smart_ssh.ui.wizard

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.mobile_smart_ssh.core.model.ServerConfig
import com.example.mobile_smart_ssh.core.security.KeyManager
import com.example.mobile_smart_ssh.core.security.SshKeyPair
import com.example.mobile_smart_ssh.data.repository.ConfigRepository
import com.example.mobile_smart_ssh.network.ssh.SSHManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class LogType {
    HEADER,
    INFO,
    SUCCESS,
    ERROR
}

data class WizardLogEntry(
    val text: String,
    val type: LogType = LogType.INFO
)

enum class WizardStage {
    INPUT,       // Wprowadzanie IP, usera i hasła
    RUNNING,     // Automatyczny proces wdrożenia (logi na żywo)
    COMPLETED,   // Pomyślnie zweryfikowano i zapisano
    FAILED       // Błąd połączenia / wdrożenia
}

data class WizardUiState(
    val host: String = "minow.rpi",
    val port: String = "22",
    val username: String = "minow",
    val password: String = "",
    val useAsSmbPassword: Boolean = true,
    val currentStage: WizardStage = WizardStage.INPUT,
    val isRunning: Boolean = false,
    val logs: List<WizardLogEntry> = emptyList(),
    val error: String? = null,
    val keyPair: SshKeyPair? = null
)

class WizardViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ConfigRepository.getInstance(application)

    private val _uiState = MutableStateFlow(WizardUiState())
    val uiState: StateFlow<WizardUiState> = _uiState.asStateFlow()

    init {
        // Jeśli powiązanie było wcześniej skonfigurowane (np. po resecie), załaduj poprzednie IP/User dla wygody
        val existing = repository.loadConfig()
        if (existing != null) {
            _uiState.update {
                it.copy(
                    host = existing.host,
                    port = existing.port.toString(),
                    username = existing.username
                )
            }
        }
    }

    fun onHostChanged(host: String) {
        _uiState.update { it.copy(host = host) }
    }

    fun onPortChanged(port: String) {
        _uiState.update { it.copy(port = port.filter { char -> char.isDigit() }) }
    }

    fun onUsernameChanged(username: String) {
        _uiState.update { it.copy(username = username) }
    }

    fun onPasswordChanged(password: String) {
        _uiState.update { it.copy(password = password) }
    }

    fun onUseAsSmbPasswordChanged(use: Boolean) {
        _uiState.update { it.copy(useAsSmbPassword = use) }
    }

    fun resetToInput() {
        _uiState.update {
            it.copy(
                currentStage = WizardStage.INPUT,
                isRunning = false,
                error = null
            )
        }
    }

    private fun addLog(text: String, type: LogType = LogType.INFO) {
        _uiState.update {
            it.copy(logs = it.logs + WizardLogEntry(text, type))
        }
    }

    /**
     * W pełni automatyczny pipeline konfiguracyjny (1-Click Automated Setup):
     * 1. Generowanie klucza Ed25519
     * 2. Wdrożenie klucza publicznego na serwer (ssh-copy-id przez hasło)
     * 3. Weryfikacja połączenia bezhasłowego kluczem Ed25519
     * 4. Zapis powiązania (Single-Host Binding) i przejście do Dashboardu
     */
    fun startAutomatedSetup(onFinished: () -> Unit) {
        val state = _uiState.value
        val host = state.host.trim()
        val portInt = state.port.toIntOrNull() ?: 22
        val user = state.username.trim()
        val pwd = state.password

        if (host.isBlank()) {
            _uiState.update { it.copy(error = "Wprowadź adres IP lub domenę Raspberry Pi") }
            return
        }
        if (user.isBlank()) {
            _uiState.update { it.copy(error = "Wprowadź nazwę użytkownika") }
            return
        }
        if (pwd.isBlank()) {
            _uiState.update { it.copy(error = "Wprowadź hasło jednorazowe SSH") }
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isRunning = true,
                    currentStage = WizardStage.RUNNING,
                    error = null,
                    logs = emptyList()
                )
            }

            addLog("========================================================", LogType.HEADER)
            addLog("  SmartSSH First-Run Setup & Configuration Wizard", LogType.HEADER)
            addLog("========================================================", LogType.HEADER)
            addLog("[i] Cel: $user@$host:$portInt")
            delay(200)

            // --- KROK 1: Generowanie pary kluczy Ed25519 ---
            addLog("")
            addLog("========================================================", LogType.HEADER)
            addLog("  Step 1: SSH Key Authentication Setup", LogType.HEADER)
            addLog("========================================================", LogType.HEADER)
            addLog("[i] Generowanie nowej pary kluczy SSH Ed25519...")

            val keyPair = KeyManager.generateEd25519KeyPair(user)
            _uiState.update { it.copy(keyPair = keyPair) }
            addLog("[+] Para kluczy Ed25519 wygenerowana pomyślnie!")
            addLog("[i] Fingerprint: ${keyPair.fingerprint}")

            delay(300)

            // --- Instalacja klucza na Raspberry Pi (ssh-copy-id) ---
            addLog("[i] Deploying public key to $user@$host:$portInt...")
            addLog("[i] Łączenie hasłem SSH w celu dodania klucza...")

            val deployResult = SSHManager.deployPublicKeyWithPassword(
                host = host,
                port = portInt,
                username = user,
                password = pwd,
                publicKeyString = keyPair.publicKey
            )

            if (deployResult.isFailure) {
                val err = deployResult.exceptionOrNull()?.message ?: "Nieznany błąd instalacji klucza"
                addLog("[-] Błąd instalacji klucza na serwerze: $err", LogType.ERROR)
                _uiState.update {
                    it.copy(
                        isRunning = false,
                        currentStage = WizardStage.FAILED,
                        error = err
                    )
                }
                return@launch
            }

            addLog("[+] Klucz publiczny zainstalowany w ~/.ssh/authorized_keys (chmod 600)!", LogType.SUCCESS)

            delay(350)

            // --- KROK 2: Weryfikacja połączenia bezhasłowego ---
            addLog("")
            addLog("========================================================", LogType.HEADER)
            addLog("  Step 2: Connection Verification", LogType.HEADER)
            addLog("========================================================", LogType.HEADER)
            addLog("[i] Testing connection to $host:$portInt using Ed25519 key...")

            val verifyResult = SSHManager.testConnection(
                host = host,
                port = portInt,
                username = user,
                privateKeyPem = keyPair.privateKeyPem
            )

            if (verifyResult.isFailure) {
                val err = verifyResult.exceptionOrNull()?.message ?: "Błąd weryfikacji połączenia"
                addLog("[-] Weryfikacja połączenia kluczem nie powiodła się: $err", LogType.ERROR)
                _uiState.update {
                    it.copy(
                        isRunning = false,
                        currentStage = WizardStage.FAILED,
                        error = err
                    )
                }
                return@launch
            }

            val serverBanner = verifyResult.getOrNull() ?: ""
            addLog("[+] Connection to Raspberry Pi verified successfully!", LogType.SUCCESS)
            if (serverBanner.isNotBlank()) {
                addLog("    System: $serverBanner", LogType.SUCCESS)
            }

            delay(300)

            // --- KROK 3: Zapis trwałego powiązania ---
            addLog("")
            addLog("[i] Zapisywanie konfiguracji powiązania w Android Keystore...")

            val config = ServerConfig(
                host = host,
                port = portInt,
                username = user,
                smbPassword = if (state.useAsSmbPassword) pwd else "",
                hasKey = true
            )

            repository.saveConfig(config)
            repository.savePrivateKey(keyPair.privateKeyPem)
            repository.savePublicKey(keyPair.publicKey)

            addLog("[+] Single-Host Binding zapisany pomyślnie w bezpiecznym magazynie!", LogType.SUCCESS)
            addLog("========================================================", LogType.HEADER)
            addLog("  Konfiguracja zakończona sukcesem! Uruchamianie...", LogType.SUCCESS)
            addLog("========================================================", LogType.HEADER)

            _uiState.update {
                it.copy(
                    isRunning = false,
                    currentStage = WizardStage.COMPLETED
                )
            }

            delay(1200)
            onFinished()
        }
    }
}
