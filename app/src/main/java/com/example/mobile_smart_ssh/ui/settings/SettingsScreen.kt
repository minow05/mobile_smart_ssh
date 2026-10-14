package com.example.mobile_smart_ssh.ui.settings

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.mobile_smart_ssh.ui.theme.CyanAccent
import com.example.mobile_smart_ssh.ui.theme.EmeraldPrimary
import com.example.mobile_smart_ssh.ui.theme.RubyRed
import com.example.mobile_smart_ssh.ui.theme.SlateBorder
import com.example.mobile_smart_ssh.ui.theme.SlateSurface
import com.example.mobile_smart_ssh.ui.theme.SlateSurfaceVariant
import com.example.mobile_smart_ssh.ui.theme.TerminalBlack

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onResetToWizard: () -> Unit,
    viewModel: SettingsViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ustawienia", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Wróć")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SlateSurface)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            // Karta 1: Parametry hosta
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = SlateSurface),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, SlateBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Dns, contentDescription = null, tint = CyanAccent)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Parametry hosta Raspberry Pi", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    SettingParamRow(label = "Adres IP / Host", value = state.config?.host ?: "-")
                    SettingParamRow(label = "Użytkownik SSH", value = state.config?.username ?: "minow")
                    SettingParamRow(label = "Model powiązania", value = "Single-Host Binding Model")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Karta 2: Klucz SSH Ed25519
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = SlateSurface),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, SlateBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Key, contentDescription = null, tint = EmeraldPrimary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Klucz SSH (Ed25519)", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White)
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text("Odcisk palca (SHA256 Fingerprint):", fontSize = 11.sp, color = Color.Gray)
                    Text(
                        text = state.fingerprint,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        color = CyanAccent
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text("Magazyn klucza prywatnego:", fontSize = 11.sp, color = Color.Gray)
                    Text(
                        text = "Android Keystore (Szyfrowanie sprzętowe AES-256)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = EmeraldPrimary
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(state.publicKey))
                            Toast.makeText(context, "Klucz publiczny skopiowany do schowka!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = SlateSurfaceVariant)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Kopiuj klucz publiczny OpenSSH", fontSize = 12.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Karta 3: Hasło SMB
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = SlateSurface),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, SlateBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Lock, contentDescription = null, tint = CyanAccent)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Uwierzytelnianie Samba (SMB/CIFS)", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White)
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Hasło konta Samba dla udziałów [${state.config?.username ?: "minow"}] i [shared]. Pozostaw puste, jeśli serwer dopuszcza dostęp anonimowy/gościa.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = state.smbPasswordInput,
                        onValueChange = viewModel::onSmbPasswordChanged,
                        label = { Text("Hasło Samba") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Karta 4: Reset Configuration (Single-Host Unbinding)
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF280B0B)),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, RubyRed.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = RubyRed)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Reset powiązania hosta",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = RubyRed
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Opcja ta czyści lokalną konfigurację na telefonie (adres IP, użytkownik, klucze SSH) i cofa aplikację do kreatora konfiguracji.\n\n" +
                                "UWAGA: Wszystkie dane, pliki oraz konfiguracja na Twoim serwerze Raspberry Pi pozostają NIETKNIĘTE.",
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        color = Color(0xFFFECACA)
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = { viewModel.setResetDialogOpen(true) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = RubyRed, contentColor = Color.White)
                    ) {
                        Icon(Icons.Default.DeleteForever, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Resetuj konfigurację hosta", fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // Modalne potwierdzenie resetu konfiguracji
    if (state.isResetDialogOpen) {
        AlertDialog(
            onDismissRequest = { viewModel.setResetDialogOpen(false) },
            title = { Text("Zresetować powiązanie hosta?") },
            text = {
                Text(
                    text = "Czy na pewno chcesz usunąć lokalne powiązanie z serwerem Raspberry Pi?\n\n" +
                            "Aplikacja powróci do kreatora początkowego (Setup Wizard). Twoje dane na Raspberry Pi NIE zostaną usunięte.",
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.resetConfiguration(onResetToWizard)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RubyRed, contentColor = Color.White)
                ) {
                    Text("Potwierdź i zresetuj")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.setResetDialogOpen(false) }) {
                    Text("Anuluj")
                }
            }
        )
    }
}

@Composable
private fun SettingParamRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, fontSize = 12.sp, color = Color.Gray)
        Text(text = value, fontSize = 12.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.SemiBold, color = Color.White)
    }
}
