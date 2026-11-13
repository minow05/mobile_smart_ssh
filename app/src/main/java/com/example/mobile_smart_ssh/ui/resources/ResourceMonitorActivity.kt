package com.example.mobile_smart_ssh.ui.resources

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

/**
 * Activity monitora zasobów serwera (CPU, RAM, Dysk, Procesy).
 * Uruchamiane jako dedykowany ekran z osobnym cyklem życia.
 */
class ResourceMonitorActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            SmartSSHTheme(darkTheme = true) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    ResourceMonitorScreen(
                        onNavigateBack = { finish() }
                    )
                }
            }
        }
    }

    companion object {
        fun createIntent(context: Context): Intent =
            Intent(context, ResourceMonitorActivity::class.java)
    }
}
