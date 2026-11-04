package com.example.mobile_smart_ssh.ui.terminal

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.mobile_smart_ssh.ui.theme.SmartSSHTheme
import org.koin.androidx.viewmodel.ext.android.viewModel

/**
 * Drugie Activity aplikacji (wzorzec Multi-Activity dla celów akademickich i dedykowanej sesji konsoli).
 * Demonstruje:
 * 1. Osobny cykl życia komponentu Activity (onCreate, finish).
 * 2. Odbiór parametrów z intencji (Intent extras) - np. polecenia startowego.
 * 3. Wpis w AndroidManifest.xml z konfiguracją adjustResize.
 * 4. Integrację Koin DI bez bezpośredniego przekazywania instancji Context.
 */
class TerminalActivity : ComponentActivity() {

    private val terminalViewModel: TerminalViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val initialCommand = intent.getStringExtra(EXTRA_INITIAL_COMMAND)
        Log.i(TAG, "TerminalActivity uruchomione z Intent (initialCommand: $initialCommand)")

        if (!initialCommand.isNullOrBlank()) {
            terminalViewModel.onAction(TerminalAction.SendCommand(initialCommand))
        }

        setContent {
            SmartSSHTheme(darkTheme = true) {
                Surface(
                    modifier = Modifier.fillMaxSize()
                ) {
                    TerminalScreen(
                        onNavigateBack = { finish() },
                        viewModel = terminalViewModel
                    )
                }
            }
        }
    }

    companion object {
        private const val TAG = "TerminalActivity"
        const val EXTRA_INITIAL_COMMAND = "extra_initial_command"

        /**
         * Statyczna metoda pomocnicza do tworzenia intencji uruchomienia TerminalActivity.
         */
        fun createIntent(context: Context, initialCommand: String? = null): Intent {
            return Intent(context, TerminalActivity::class.java).apply {
                if (!initialCommand.isNullOrBlank()) {
                    putExtra(EXTRA_INITIAL_COMMAND, initialCommand)
                }
            }
        }
    }
}
