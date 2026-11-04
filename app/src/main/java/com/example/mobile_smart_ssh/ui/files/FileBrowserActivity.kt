package com.example.mobile_smart_ssh.ui.files

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
 * Activity zdalnego menedżera plików (SFTP / Samba).
 * Uruchamiane jako osobny komponent z własnym cyklem życia i stosem cofania.
 */
class FileBrowserActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            SmartSSHTheme(darkTheme = true) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    FileBrowserScreen(
                        onNavigateBack = { finish() }
                    )
                }
            }
        }
    }

    companion object {
        fun createIntent(context: Context): Intent =
            Intent(context, FileBrowserActivity::class.java)
    }
}
