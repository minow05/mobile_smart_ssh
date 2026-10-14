package com.example.mobile_smart_ssh.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.mobile_smart_ssh.ui.theme.CyanAccent
import com.example.mobile_smart_ssh.ui.theme.EmeraldPrimary
import com.example.mobile_smart_ssh.ui.theme.IndigoSecondary
import com.example.mobile_smart_ssh.ui.theme.RubyRed
import com.example.mobile_smart_ssh.ui.theme.SlateBorder
import com.example.mobile_smart_ssh.ui.theme.SlateSurface
import com.example.mobile_smart_ssh.ui.theme.SlateSurfaceVariant

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onNavigateToTerminal: () -> Unit,
    onNavigateToFiles: () -> Unit,
    onNavigateToResources: () -> Unit,
    onNavigateToSettings: () -> Unit,
    viewModel: DashboardViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "SmartSSH",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = EmeraldPrimary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Mobile",
                            fontWeight = FontWeight.Normal,
                            fontSize = 20.sp,
                            color = Color.White
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = viewModel::checkHostStatus,
                        enabled = !state.isCheckingStatus
                    ) {
                        if (state.isCheckingStatus) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = EmeraldPrimary)
                        } else {
                            Icon(Icons.Default.Refresh, contentDescription = "Odśwież status", tint = Color.White)
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
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Nagłówek powiązanego hosta
            ServerHeaderCard(
                host = state.config?.host ?: "Brak hosta",
                user = state.config?.username ?: "minow",
                isOnline = state.isHostOnline,
                isChecking = state.isCheckingStatus
            )

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "OPERACJE HOSTA RASPBERRY PI",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp, bottom = 12.dp)
            )

            // Karta 1: Interaktywny Terminal SSH
            DashboardActionCard(
                title = "Terminal SSH",
                subtitle = "Bezpośrednia konsola z obsługą kolorów ANSI i szybkich poleceń.",
                icon = Icons.Default.Computer,
                iconTint = EmeraldPrimary,
                onClick = onNavigateToTerminal
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Karta 2: Remote File & Network Storage Manager
            DashboardActionCard(
                title = "Pliki i dyski sieciowe",
                subtitle = "Przeglądanie i zarządzanie plikami na dyskach domowym i współdzielonym.",
                icon = Icons.Default.Folder,
                iconTint = CyanAccent,
                onClick = onNavigateToFiles
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Karta 3: Resource & Storage Quota Monitor
            DashboardActionCard(
                title = "Monitor zasobów",
                subtitle = "Obciążenie CPU, zużycie pamięci RAM, dysk oraz limity kwotowe.",
                icon = Icons.Default.Insights,
                iconTint = IndigoSecondary,
                onClick = onNavigateToResources
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Karta 4: Settings & Reconfiguration
            DashboardActionCard(
                title = "Ustawienia",
                subtitle = "Parametry połączenia, klucz SSH oraz reset powiązania hosta.",
                icon = Icons.Default.Settings,
                iconTint = Color.Gray,
                onClick = onNavigateToSettings
            )
        }
    }
}

@Composable
private fun ServerHeaderCard(
    host: String,
    user: String,
    isOnline: Boolean?,
    isChecking: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SlateSurface),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, SlateBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "POWIĄZANY SERWER",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Gray,
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "$user@$host",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            // Badge statusu połączenia ONLINE / OFFLINE
            HostStatusBadge(isOnline = isOnline, isChecking = isChecking)
        }
    }
}

@Composable
private fun HostStatusBadge(isOnline: Boolean?, isChecking: Boolean) {
    val (bgColor, textColor, text) = when {
        isChecking -> Triple(SlateSurfaceVariant, Color.LightGray, "SPRAWDZANIE...")
        isOnline == true -> Triple(Color(0xFF064E3B), EmeraldPrimary, "ONLINE")
        isOnline == false -> Triple(Color(0xFF450A0A), RubyRed, "OFFLINE")
        else -> Triple(SlateSurfaceVariant, Color.Gray, "NIEZNANY")
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(bgColor)
            .border(1.dp, textColor.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(textColor)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = text,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                color = textColor,
                letterSpacing = 0.5.sp
            )
        }
    }
}

@Composable
private fun DashboardActionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconTint: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = SlateSurface),
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, SlateBorder)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(iconTint.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(24.dp))
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 16.sp
                )
            }

            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = Color.Gray,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
