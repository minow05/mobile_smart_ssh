package com.example.mobile_smart_ssh.ui.terminal

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mobile_smart_ssh.core.model.TerminalLine
import com.example.mobile_smart_ssh.ui.theme.RubyRed
import com.example.mobile_smart_ssh.ui.theme.SmartSSHTheme
import com.example.mobile_smart_ssh.ui.theme.TerminalCyan
import com.example.mobile_smart_ssh.ui.theme.TerminalGreen
import com.example.mobile_smart_ssh.ui.theme.TerminalYellow
import org.koin.androidx.compose.koinViewModel

// Kolorystyka w stylu Termux (Pitch Black, Slate i Neon Green)
private val TermuxPitchBlack = Color(0xFF000000)
private val TermuxConsoleDark = Color(0xFF080B10)
private val TermuxHeaderDark = Color(0xFF0D1117)
private val TermuxKeySurface = Color(0xFF21262D)
private val TermuxKeyBorder = Color(0xFF30363D)
private val TermuxKeyActive = Color(0xFF5AF78E)
private val TermuxPromptColor = Color(0xFF5AF78E)
private val TermuxTextPrimary = Color(0xFFE6EDF3)
private val TermuxTextDim = Color(0xFF8B949E)

@Composable
fun TerminalScreen(
    onNavigateBack: () -> Unit,
    viewModel: TerminalViewModel = koinViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    TerminalContent(
        state = state,
        onAction = viewModel::onAction,
        onNavigateBack = onNavigateBack
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TerminalContent(
    state: TerminalUiState,
    onAction: (TerminalAction) -> Unit,
    onNavigateBack: () -> Unit
) {
    val listState = rememberLazyListState()
    val focusRequester = remember { FocusRequester() }

    // Automatyczne przewijanie do najnowszych wpisów bufora
    LaunchedEffect(state.lines.size) {
        if (state.lines.isNotEmpty()) {
            listState.animateScrollToItem(state.lines.size - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Terminal,
                            contentDescription = null,
                            tint = TermuxPromptColor,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "termux // ssh",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                fontFamily = FontFamily.Monospace,
                                color = Color.White
                            )
                            Text(
                                text = state.prompt,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                color = TermuxPromptColor
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Wróć",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    // Wskaźnik stanu sesji ONLINE / OFFLINE
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(end = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(if (state.isConnected) TermuxPromptColor else RubyRed)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = if (state.isConnected) "ONLINE" else "OFFLINE",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = if (state.isConnected) TermuxPromptColor else RubyRed
                        )
                    }

                    // Przycisk wyczyszczenia ekranu (CLR)
                    Surface(
                        onClick = { onAction(TerminalAction.ClearScreen) },
                        shape = RoundedCornerShape(4.dp),
                        color = TermuxKeySurface,
                        border = androidx.compose.foundation.BorderStroke(1.dp, TermuxKeyBorder),
                        modifier = Modifier.padding(horizontal = 4.dp)
                    ) {
                        Text(
                            text = "CLR",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            color = TermuxTextPrimary,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp)
                        )
                    }

                    // Przycisk ponownego połączenia (Reconnect)
                    IconButton(
                        onClick = { onAction(TerminalAction.Connect) },
                        enabled = !state.isConnecting
                    ) {
                        if (state.isConnecting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = TermuxPromptColor
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Połącz ponownie",
                                tint = Color.White
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = TermuxHeaderDark
                )
            )
        },
        containerColor = TermuxPitchBlack
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
                .navigationBarsPadding()
        ) {
            // Główny obszar konsoli Termux (Pitch Black z autoscrollem i klikiem skupiającym input)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(TermuxPitchBlack)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        focusRequester.requestFocus()
                    }
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(state.lines) { line ->
                        val textColor = when {
                            line.isError -> RubyRed
                            line.isCommand -> TermuxPromptColor
                            line.text.startsWith("Linux") || line.text.startsWith("Welcome") -> TerminalGreen
                            line.text.contains("warning", ignoreCase = true) -> TerminalYellow
                            line.text.startsWith("[") -> TerminalCyan
                            else -> TermuxTextPrimary
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

            // Inline pole wpisywania polecenia w stylu powłoki bash Termux
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(TermuxConsoleDark)
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$ ",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = TermuxPromptColor
                )

                BasicTextField(
                    value = state.currentCommandInput,
                    onValueChange = { onAction(TerminalAction.UpdateInput(it)) },
                    modifier = Modifier
                        .weight(1f)
                        .focusRequester(focusRequester),
                    textStyle = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                        color = Color.White
                    ),
                    cursorBrush = SolidColor(TermuxPromptColor),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(
                        onSend = { onAction(TerminalAction.SendCommand()) }
                    ),
                    decorationBox = { innerTextField ->
                        if (state.currentCommandInput.isEmpty()) {
                            Text(
                                text = "polecenie (np. ls, htop, df)...",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                color = TermuxTextDim
                            )
                        }
                        innerTextField()
                    }
                )

                Spacer(modifier = Modifier.width(6.dp))

                // Przycisk akcji (Wyślij / Zatrzymaj aktywny proces)
                Surface(
                    onClick = {
                        if (state.isExecuting) {
                            onAction(TerminalAction.CancelCommand)
                        } else {
                            onAction(TerminalAction.SendCommand())
                        }
                    },
                    shape = RoundedCornerShape(4.dp),
                    color = if (state.isExecuting) RubyRed else TermuxPromptColor,
                    modifier = Modifier.size(28.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (state.isExecuting) {
                            Icon(
                                imageVector = Icons.Default.Stop,
                                contentDescription = "Zatrzymaj (^C)",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Wykonaj",
                                tint = Color.Black,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            // Pasek podręcznych poleceń Linuksa (Quick Command Chips)
            QuickCommandChips(
                onSelectCommand = { cmd -> onAction(TerminalAction.SendCommand(cmd)) }
            )

            // Panel pomocniczy w przypadku aktywnego klawisza CTRL
            if (state.isCtrlActive) {
                CtrlActiveBanner(onAction = onAction)
            }

            // Dedykowany pasek klawiszy funkcyjnych Termux (Extra Keys Bar nad klawiaturą)
            TermuxExtraKeysBar(
                isCtrlActive = state.isCtrlActive,
                isAltActive = state.isAltActive,
                onAction = onAction
            )
        }
    }
}

/**
 * Pasek klawiszy funkcyjnych Termux (Extra Keys Bar).
 * Rozwiązuje problem braku klawiszy ESC, TAB, CTRL, ALT, | czy strzałek historii na klawiaturach mobilnych.
 */
@Composable
private fun TermuxExtraKeysBar(
    isCtrlActive: Boolean,
    isAltActive: Boolean,
    onAction: (TerminalAction) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(TermuxHeaderDark)
            .padding(horizontal = 4.dp, vertical = 3.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ExtraKeyButton(label = "ESC", modifier = Modifier.weight(1f)) {
            onAction(TerminalAction.Escape)
        }
        ExtraKeyButton(label = "TAB", modifier = Modifier.weight(1f)) {
            onAction(TerminalAction.TabComplete)
        }
        ExtraKeyButton(label = "CTRL", isActive = isCtrlActive, modifier = Modifier.weight(1f)) {
            onAction(TerminalAction.ToggleCtrl)
        }
        ExtraKeyButton(label = "ALT", isActive = isAltActive, modifier = Modifier.weight(1f)) {
            onAction(TerminalAction.ToggleAlt)
        }
        ExtraKeyButton(label = "-", modifier = Modifier.weight(1f)) {
            onAction(TerminalAction.InsertKey("-"))
        }
        ExtraKeyButton(label = "~", modifier = Modifier.weight(1f)) {
            onAction(TerminalAction.InsertKey("~"))
        }
        ExtraKeyButton(label = "/", modifier = Modifier.weight(1f)) {
            onAction(TerminalAction.InsertKey("/"))
        }
        ExtraKeyButton(label = "|", modifier = Modifier.weight(1f)) {
            onAction(TerminalAction.InsertKey("|"))
        }
        ExtraKeyButton(label = "▲", modifier = Modifier.weight(1f)) {
            onAction(TerminalAction.HistoryUp)
        }
        ExtraKeyButton(label = "▼", modifier = Modifier.weight(1f)) {
            onAction(TerminalAction.HistoryDown)
        }
    }
}

@Composable
private fun ExtraKeyButton(
    label: String,
    modifier: Modifier = Modifier,
    isActive: Boolean = false,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(34.dp),
        shape = RoundedCornerShape(4.dp),
        contentPadding = PaddingValues(0.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isActive) TermuxKeyActive else TermuxKeySurface,
            contentColor = if (isActive) Color.Black else TermuxTextPrimary
        ),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = if (isActive) TermuxKeyActive else TermuxKeyBorder
        )
    ) {
        Text(
            text = label,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = 10.sp
        )
    }
}

/**
 * Podręczny pasek akcji skrótów klawiaturowych, gdy modyfikator CTRL jest włączony.
 */
@Composable
private fun CtrlActiveBanner(
    onAction: (TerminalAction) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF161B22))
            .padding(horizontal = 8.dp, vertical = 3.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "CTRL ON:",
            fontFamily = FontFamily.Monospace,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = TermuxPromptColor
        )

        ShortcutPill(label = "^C Kill") {
            onAction(TerminalAction.CancelCommand)
        }

        ShortcutPill(label = "^L Clear") {
            onAction(TerminalAction.ClearScreen)
        }

        ShortcutPill(label = "^D EOF") {
            onAction(TerminalAction.ToggleCtrl)
        }
    }
}

@Composable
private fun ShortcutPill(
    label: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(3.dp),
        color = Color(0xFF232936),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF3B4455))
    ) {
        Text(
            text = label,
            fontFamily = FontFamily.Monospace,
            fontSize = 10.sp,
            color = Color.White,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

@Composable
private fun QuickCommandChips(onSelectCommand: (String) -> Unit) {
    val quickCmds = listOf(
        "ls -la",
        "uptime",
        "df -h /",
        "free -h",
        "ps aux",
        "htop",
        "uname -a",
        "git status",
        "clear"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(TermuxPitchBlack)
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 6.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        quickCmds.forEach { cmd ->
            Surface(
                onClick = { onSelectCommand(cmd) },
                shape = RoundedCornerShape(4.dp),
                color = Color(0xFF161B22),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2A313C))
            ) {
                Text(
                    text = cmd,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = Color(0xFF7EE787),
                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                )
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun TerminalContentPreview() {
    SmartSSHTheme(darkTheme = true) {
        TerminalContent(
            state = TerminalUiState(
                isConnected = true,
                prompt = "minow@raspberrypi:~$",
                lines = listOf(
                    TerminalLine(text = "Linux raspberrypi 6.1.0-rpi7-rpi-v8 #1 SMP PREEMPT aarch64"),
                    TerminalLine(text = "minow@raspberrypi:~$ uname -a", isCommand = true),
                    TerminalLine(text = "Linux raspberrypi 6.1.0-rpi7-rpi-v8 #1 SMP PREEMPT Debian 1:6.1.63-1+rpt1 aarch64"),
                    TerminalLine(text = "minow@raspberrypi:~$ uptime", isCommand = true),
                    TerminalLine(text = " 16:30:12 up 14 days,  3:45,  2 users,  load average: 0.12, 0.08, 0.05")
                ),
                commandHistory = listOf("uname -a", "uptime"),
                isCtrlActive = false
            ),
            onAction = {},
            onNavigateBack = {}
        )
    }
}
