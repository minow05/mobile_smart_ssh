package com.example.mobile_smart_ssh

import android.Manifest
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
import com.example.mobile_smart_ssh.monitor.ServerMonitor
import com.example.mobile_smart_ssh.ui.navigation.AppNavGraph
import com.example.mobile_smart_ssh.ui.theme.SmartSSHTheme

class MainActivity : ComponentActivity() {

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        Log.i("MainActivity", "POST_NOTIFICATIONS permission granted: $isGranted")
        if (isGranted && ServerMonitor.isEnabled(this)) {
            ServerMonitor.schedule(this)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (ServerMonitor.isEnabled(this)) {
            ServerMonitor.schedule(this)
            if (!ServerMonitor.hasNotificationPermission(this) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        setContent {
            SmartSSHTheme(darkTheme = true) {
                Surface(
                    modifier = Modifier.fillMaxSize()
                ) {
                    AppNavGraph()
                }
            }
        }
    }
}
