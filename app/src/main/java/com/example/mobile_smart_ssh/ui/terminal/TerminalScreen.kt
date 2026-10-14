package com.example.mobile_smart_ssh.ui.terminal

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
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
import com.example.mobile_smart_ssh.ui.theme.TerminalYellow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TerminalScreen(
    onNavigateBack: () -> Unit,
    viewModel: TerminalViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()

    // Automatyczne przewijanie do najnowszych linii terminala
    LaunchedEffect(state.lines.size) {
        if (state.lines.isNotEmpty()) {
            listState.animateScrollToItem(state.lines.size - 1)
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
                            Text("Terminal SSH", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text(state.prompt, fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = CyanAccent)
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Wróć")
                    }
                },
                actions = {
                    // Wskaźnik połączenia
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (state.isConnected) EmeraldPrimary else RubyRed)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (state.isConnected) "ONLINE" else "OFFLINE",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (state.isConnected) EmeraldPrimary else RubyRed
                        )
                    }

                    IconButton(
                        onClick = viewModel::connectToTerminal,
                        enabled = !state.isConnecting
                    ) {
                        if (state.isConnecting) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = EmeraldPrimary)
                        } else {
                            Icon(Icons.Default.Refresh, contentDescription = "Połącz ponownie")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SlateSurface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Pasek szybkich komend (Quick Actions)
            QuickCommandChips(
                onSelectCommand = { cmd -> viewModel.sendCommand(cmd) }
            )

            // Konsola terminala (Czarny ekran z czcionką monospaced)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp)
                    .background(TerminalBlack, RoundedCornerShape(10.dp))
                    .border(1.dp, SlateBorder, RoundedCornerShape(10.dp))
                    .padding(8.dp)
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(state.lines) { line ->
                        val textColor = when {
                            line.isError -> RubyRed
                            line.isCommand -> TerminalCyan
                            line.text.startsWith("Linux") || line.text.startsWith("Welcome") -> TerminalGreen
                            line.text.contains("warning", ignoreCase = true) -> TerminalYellow
                            else -> Color(0xFFE2E8F0)
                        }

                        Text(
                            text = line.text,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            lineHeight = 16.sp,
                            color = textColor,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // Pasek wprowadzania polecenia powłoki
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = state.currentCommandInput,
                    onValueChange = viewModel::onCommandInputChanged,
                    placeholder = { Text("Wpisz polecenie...", fontSize = 13.sp) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(
                        onSend = { viewModel.sendCommand() }
                    ),
                    modifier = Modifier.weight(1f),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = EmeraldPrimary,
                        unfocusedBorderColor = SlateBorder,
                        focusedContainerColor = SlateSurface,
                        unfocusedContainerColor = SlateSurface
                    )
                )

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = {
                        if (state.isExecuting) {
                            viewModel.cancelCurrentCommand()
                        } else {
                            viewModel.sendCommand()
                        }
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (state.isExecuting) RubyRed else EmeraldPrimary),
                    colors = IconButtonDefaults.iconButtonColors(contentColor = Color.Black)
                ) {
                    if (state.isExecuting) {
                        Icon(Icons.Default.Stop, contentDescription = "Zatrzymaj", tint = Color.White)
                    } else {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Wyślij")
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickCommandChips(onSelectCommand: (String) -> Unit) {
    val quickCmds = listOf(
        "ls -la",
        "uptime",
        "df -h /",
        "free -h",
        "ps aux --sort=-%cpu | head -n 10",
        "quota -u $(whoami)",
        "uname -a",
        "clear"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        quickCmds.forEach { cmd ->
            FilterChip(
                selected = false,
                onClick = { onSelectCommand(cmd) },
                label = {
                    Text(
                        text = cmd,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = Color.White
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = SlateSurfaceVariant
                ),
                border = FilterChipDefaults.filterChipBorder(
                    borderColor = SlateBorder,
                    enabled = true,
                    selected = false
                )
            )
        }
    }
}
