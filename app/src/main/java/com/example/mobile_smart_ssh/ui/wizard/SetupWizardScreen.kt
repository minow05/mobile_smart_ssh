package com.example.mobile_smart_ssh.ui.wizard

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
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
import com.example.mobile_smart_ssh.ui.theme.TerminalCyan
import com.example.mobile_smart_ssh.ui.theme.TerminalGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetupWizardScreen(
    onFinished: () -> Unit,
    viewModel: WizardViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()

    // Autoprzewijanie logów konsoli
    LaunchedEffect(state.logs.size) {
        if (state.logs.isNotEmpty()) {
            listState.animateScrollToItem(state.logs.size - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Terminal, contentDescription = null, tint = EmeraldPrimary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "SmartSSH Setup Wizard",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = Color.White
                            )
                            Text(
                                text = "Automatyczna konfiguracja Single-Host",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
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
                .padding(16.dp)
        ) {
            when (state.currentStage) {
                WizardStage.INPUT -> {
                    WizardInputContent(
                        state = state,
                        onHostChange = viewModel::onHostChanged,
                        onUserChange = viewModel::onUsernameChanged,
                        onPasswordChange = viewModel::onPasswordChanged,
                        onUseAsSmbChange = viewModel::onUseAsSmbPasswordChanged,
                        onStartSetup = {
                            viewModel.startAutomatedSetup(onFinished)
                        }
                    )
                }

                WizardStage.RUNNING, WizardStage.COMPLETED, WizardStage.FAILED -> {
                    WizardExecutionConsoleContent(
                        state = state,
                        listState = listState,
                        onRetry = viewModel::resetToInput,
                        onProceed = onFinished
                    )
                }
            }
        }
    }
}

@Composable
private fun WizardInputContent(
    state: WizardUiState,
    onHostChange: (String) -> Unit,
    onUserChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onUseAsSmbChange: (Boolean) -> Unit,
    onStartSetup: () -> Unit
) {
    var isPasswordVisible by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Nagłówek wizualny
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = SlateSurface),
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, SlateBorder)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Konfiguracja Zdalnego Dostępu (Raspberry Pi)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = EmeraldPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Podaj dane serwera. Aplikacja automatycznie wygeneruje parę kluczy Ed25519, " +
                            "zainstaluje klucz publiczny na Raspberry Pi (ssh-copy-id) i przetestuje połączenie.",
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Adres Raspberry Pi / IP
        OutlinedTextField(
            value = state.host,
            onValueChange = onHostChange,
            label = { Text("Adres Raspberry Pi / IP") },
            placeholder = { Text("np. minow.rpi, 172.30.0.1, 10.147.19.x") },
            leadingIcon = { Icon(Icons.Default.Dns, contentDescription = null, tint = CyanAccent) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = EmeraldPrimary,
                unfocusedBorderColor = SlateBorder
            )
        )

        // Użytkownik Raspberry Pi
        OutlinedTextField(
            value = state.username,
            onValueChange = onUserChange,
            label = { Text("Użytkownik") },
            placeholder = { Text("np. minow") },
            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = CyanAccent) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = EmeraldPrimary,
                unfocusedBorderColor = SlateBorder
            )
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Hasło SSH (jednorazowa instalacja klucza na serwerze)
        OutlinedTextField(
            value = state.password,
            onValueChange = onPasswordChange,
            label = { Text("Hasło użytkownika") },
            placeholder = { Text("Hasło do Raspberry Pi") },
            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = EmeraldPrimary) },
            trailingIcon = {
                IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                    Icon(
                        imageVector = if (isPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = "Pokaż hasło",
                        tint = Color.Gray
                    )
                }
            },
            visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = EmeraldPrimary,
                unfocusedBorderColor = SlateBorder
            )
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Checkbox: Hasło dla Samby
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = state.useAsSmbPassword,
                onCheckedChange = onUseAsSmbChange,
                colors = CheckboxDefaults.colors(checkedColor = EmeraldPrimary)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "Użyj tego samego hasła dla zasobów Samba (SMB)",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Komunikat błędu walidacji formularza
        AnimatedVisibility(visible = state.error != null) {
            Text(
                text = state.error ?: "",
                color = RubyRed,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Główny przycisk automatycznej konfiguracji
        Button(
            onClick = onStartSetup,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = EmeraldPrimary,
                contentColor = Color.Black
            )
        ) {
            Icon(Icons.Default.PlayArrow, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Uruchom automatyczną konfigurację",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Blok podsumowujący operacje
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = SlateSurfaceVariant),
            shape = RoundedCornerShape(8.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(text = "Co wykona SmartSSH Mobile?", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.White)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = "• Wygeneruje bezpieczną parę kluczy Ed25519 (BouncyCastle)\n" +
                        "• Prześle klucz publiczny do ~/.ssh/authorized_keys (ssh-copy-id)\n" +
                        "• Przetestuje bezhasłowe logowanie przez klucz SSH\n" +
                        "• Zapisze powiązanie (Single-Host) w Android Keystore",
                    fontSize = 11.sp,
                    color = Color.LightGray,
                    lineHeight = 16.sp
                )
            }
        }
    }
}

@Composable
private fun WizardExecutionConsoleContent(
    state: WizardUiState,
    listState: androidx.compose.foundation.lazy.LazyListState,
    onRetry: () -> Unit,
    onProceed: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Pasek statusu wykonania
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                when (state.currentStage) {
                    WizardStage.RUNNING -> {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = EmeraldPrimary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Instalacja klucza SSH na serwerze...", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White)
                    }
                    WizardStage.COMPLETED -> {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Gotowe! Host powiązany.", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = EmeraldPrimary)
                    }
                    WizardStage.FAILED -> {
                        Icon(Icons.Default.Error, contentDescription = null, tint = RubyRed, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Niepowodzenie konfiguracji", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = RubyRed)
                    }
                    else -> {}
                }
            }

            Text(
                text = "${state.username}@${state.host}",
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                color = CyanAccent
            )
        }

        // Konsola wyjściowa z logami na żywo
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(TerminalBlack)
                .border(1.dp, SlateBorder, RoundedCornerShape(10.dp))
                .padding(12.dp)
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize()
            ) {
                items(state.logs) { entry ->
                    val color = when (entry.type) {
                        LogType.HEADER -> CyanAccent
                        LogType.INFO -> Color(0xFFCBD5E1)
                        LogType.SUCCESS -> TerminalGreen
                        LogType.ERROR -> RubyRed
                    }

                    Text(
                        text = entry.text,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        lineHeight = 15.sp,
                        color = color
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Przyciski akcji po zakończeniu lub błędzie
        if (state.currentStage == WizardStage.FAILED) {
            Button(
                onClick = onRetry,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SlateSurfaceVariant, contentColor = Color.White)
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Popraw dane i spróbuj ponownie", fontWeight = FontWeight.SemiBold)
            }
        } else if (state.currentStage == WizardStage.COMPLETED) {
            Button(
                onClick = onProceed,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary, contentColor = Color.Black)
            ) {
                Text("Przejdź do Dashboardu", fontWeight = FontWeight.Bold)
            }
        }
    }
}
