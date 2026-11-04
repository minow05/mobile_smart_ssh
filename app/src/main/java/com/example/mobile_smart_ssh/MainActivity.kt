package com.example.mobile_smart_ssh

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.mobile_smart_ssh.data.repository.ConfigRepository
import com.example.mobile_smart_ssh.monitor.ServerMonitor
import com.example.mobile_smart_ssh.network.ssh.SSHSessionManager
import com.example.mobile_smart_ssh.ui.dashboard.DashboardScreen
import com.example.mobile_smart_ssh.ui.files.FileBrowserActivity
import com.example.mobile_smart_ssh.ui.resources.ResourceMonitorActivity
import com.example.mobile_smart_ssh.ui.settings.SettingsActivity
import com.example.mobile_smart_ssh.ui.terminal.TerminalActivity
import com.example.mobile_smart_ssh.ui.theme.SmartSSHTheme
import com.example.mobile_smart_ssh.ui.wizard.SetupWizardActivity
import org.koin.android.ext.android.inject

/**
 * Główne Activity aplikacji w architekturze Multi-Activity.
 * Pełni rolę punktu wejściowego (Dashboard) oraz koordynatora nawigacji Intent do dedykowanych Activity:
 * - SetupWizardActivity (jeśli serwer nie jest powiązany)
 * - TerminalActivity (dedykowana konsola w stylu Termux)
 * - FileBrowserActivity (zdalny menedżer plików SFTP/SMB)
 * - ResourceMonitorActivity (monitor zasobów systemowych)
 * - SettingsActivity (ustawienia i powiadomienia)
 */
class MainActivity : ComponentActivity() {

    private val serverMonitor: ServerMonitor by inject()
    private val repository: ConfigRepository by inject()
    private val sessionManager: SSHSessionManager by inject()

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        Log.i(TAG, "POST_NOTIFICATIONS permission granted: $isGranted")
        if (isGranted && serverMonitor.isEnabled()) {
            serverMonitor.schedule()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Sprawdź konfigurację: jeśli brak powiązania z serwerem, przekieruj do kreatora
        if (!repository.isConfigured()) {
            Log.i(TAG, "Brak konfiguracji serwera - uruchamianie SetupWizardActivity")
            startActivity(SetupWizardActivity.createIntent(this))
            finish()
            return
        }

        // Natychmiastowe nawiązanie trwałej sesji SSH w tle przy starcie aplikacji (odpowiednik m_session z desktopu)
        sessionManager.ensureConnected(forceReconnect = false)

        // Harmonogram monitora zasobów w tle
        if (serverMonitor.isEnabled()) {
            serverMonitor.schedule()
            if (!serverMonitor.hasNotificationPermission() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        setContent {
            SmartSSHTheme(darkTheme = true) {
                Surface(
                    modifier = Modifier.fillMaxSize()
                ) {
                    DashboardScreen(
                        onNavigateToTerminal = {
                            startActivity(TerminalActivity.createIntent(this))
                        },
                        onNavigateToFiles = {
                            startActivity(FileBrowserActivity.createIntent(this))
                        },
                        onNavigateToResources = {
                            startActivity(ResourceMonitorActivity.createIntent(this))
                        },
                        onNavigateToSettings = {
                            startActivity(SettingsActivity.createIntent(this))
                        }
                    )
                }
            }
        }
    }

    companion object {
        private const val TAG = "MainActivity"

        fun createIntent(context: Context): Intent =
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
    }
}
