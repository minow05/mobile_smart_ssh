package com.example.mobile_smart_ssh.ui.settings

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.mobile_smart_ssh.ui.theme.SmartSSHTheme
import com.example.mobile_smart_ssh.ui.wizard.SetupWizardActivity

/**
 * Activity ekranu ustawień aplikacji (monitoring, powiadomienia, hasło SMB, reset konfiguracji).
 */
class SettingsActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            SmartSSHTheme(darkTheme = true) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    SettingsScreen(
                        onNavigateBack = { finish() },
                        onResetToWizard = {
                            startActivity(SetupWizardActivity.createIntent(this))
                            finishAffinity()
                        }
                    )
                }
            }
        }
    }

    companion object {
        fun createIntent(context: Context): Intent =
            Intent(context, SettingsActivity::class.java)
    }
}
