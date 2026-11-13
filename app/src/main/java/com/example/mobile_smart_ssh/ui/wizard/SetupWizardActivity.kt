package com.example.mobile_smart_ssh.ui.wizard

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.mobile_smart_ssh.MainActivity
import com.example.mobile_smart_ssh.ui.theme.SmartSSHTheme

/**
 * Activity kreatora konfiguracji pierwszego uruchomienia (Setup Wizard).
 * Obsługuje wprowadzenie adresu hosta, portu, loginu i klucza Ed25519.
 * Po pomyślnym zakończeniu przekierowuje do MainActivity (Dashboard).
 */
class SetupWizardActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            SmartSSHTheme(darkTheme = true) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    SetupWizardScreen(
                        onFinished = {
                            startActivity(MainActivity.createIntent(this))
                            finish()
                        }
                    )
                }
            }
        }
    }

    companion object {
        fun createIntent(context: Context): Intent =
            Intent(context, SetupWizardActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
    }
}
