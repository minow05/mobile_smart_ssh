package com.example.mobile_smart_ssh

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.mobile_smart_ssh.ui.navigation.AppNavGraph
import com.example.mobile_smart_ssh.ui.theme.SmartSSHTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
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