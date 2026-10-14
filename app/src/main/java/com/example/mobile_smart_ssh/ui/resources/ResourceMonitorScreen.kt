package com.example.mobile_smart_ssh.ui.resources

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.mobile_smart_ssh.core.model.ProcessInfo
import com.example.mobile_smart_ssh.ui.theme.AmberWarning
import com.example.mobile_smart_ssh.ui.theme.CyanAccent
import com.example.mobile_smart_ssh.ui.theme.EmeraldPrimary
import com.example.mobile_smart_ssh.ui.theme.IndigoSecondary
import com.example.mobile_smart_ssh.ui.theme.RubyRed
import com.example.mobile_smart_ssh.ui.theme.SlateBorder
import com.example.mobile_smart_ssh.ui.theme.SlateSurface
import com.example.mobile_smart_ssh.ui.theme.SlateSurfaceVariant
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResourceMonitorScreen(
    onNavigateBack: () -> Unit,
    viewModel: ResourceMonitorViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val res = state.resources

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Monitor Zasobów & Kwot", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text(
                            text = if (state.lastRefreshTime > 0) {
                                val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                                "Ostatnia aktualizacja: ${sdf.format(Date(state.lastRefreshTime))}"
                            } else "Ładowanie danych...",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Wróć")
                    }
                },
                actions = {
                    IconButton(
                        onClick = viewModel::refreshResources,
                        enabled = !state.isLoading
                    ) {
                        if (state.isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = EmeraldPrimary)
                        } else {
                            Icon(Icons.Default.Refresh, contentDescription = "Odśwież")
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
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            // Uptime Banner
            if (res.uptime.isNotBlank() && res.uptime != "Niedostępny") {
                UptimeBanner(uptime = res.uptime)
                Spacer(modifier = Modifier.height(14.dp))
            }

            // Karta 1: Obciążenie procesora (CPU)
            ResourceMetricCard(
                title = "Obciążenie procesora (CPU)",
                icon = Icons.Default.Speed,
                percentage = res.cpuPercentage,
                details = listOf(
                    "Aktywność procesora" to String.format(Locale.US, "%.1f %%", res.cpuPercentage),
                    "Architektura" to "ARM Cortex (Raspberry Pi)"
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Karta 2: Pamięć Operacyjna (RAM)
            ResourceMetricCard(
                title = "Pamięć operacyjna (RAM)",
                icon = Icons.Default.Memory,
                percentage = res.ramPercentage,
                details = listOf(
                    "Użycie" to "${res.ramUsed} / ${res.ramTotal}",
                    "Zajętość" to String.format(Locale.US, "%.1f %%", res.ramPercentage)
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Karta 3: Przestrzeń Dyskowa
            ResourceMetricCard(
                title = "Pamięć masowa (Dysk)",
                icon = Icons.Default.Storage,
                percentage = res.diskPercentage,
                details = listOf(
                    "Zajętość" to "${res.diskUsed} / ${res.diskTotal}",
                    "Wolne miejsce" to res.diskFree
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Karta 4: Kwota Dyskowa Użytkownika
            QuotaMetricCard(
                quotaUsed = res.userQuotaUsed,
                quotaLimit = res.userQuotaLimit,
                percentage = res.userQuotaPercentage
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Karta 5: Top Procesy
            TopProcessesCard(processes = res.topProcesses)

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun UptimeBanner(uptime: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SlateSurface),
        shape = RoundedCornerShape(10.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, SlateBorder)
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 14.dp, vertical = 10.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(EmeraldPrimary)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "Czas pracy: $uptime",
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                color = Color.White
            )
        }
    }
}

@Composable
private fun ResourceMetricCard(
    title: String,
    icon: ImageVector,
    percentage: Float,
    details: List<Pair<String, String>>
) {
    val progressColor = when {
        percentage >= 85f -> RubyRed
        percentage >= 65f -> AmberWarning
        else -> EmeraldPrimary
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SlateSurface),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, SlateBorder)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(icon, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White)
                }

                Text(
                    text = String.format(Locale.US, "%.1f%%", percentage),
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    fontFamily = FontFamily.Monospace,
                    color = progressColor
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            LinearProgressIndicator(
                progress = { (percentage / 100f).coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = progressColor,
                trackColor = SlateSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                details.forEach { (label, value) ->
                    Column {
                        Text(text = label, fontSize = 11.sp, color = Color.Gray)
                        Text(text = value, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
private fun QuotaMetricCard(
    quotaUsed: String,
    quotaLimit: String,
    percentage: Float
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SlateSurface),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, SlateBorder)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.PieChart, contentDescription = null, tint = IndigoSecondary, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Limit kwoty dyskowej",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (percentage > 0) {
                LinearProgressIndicator(
                    progress = { (percentage / 100f).coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = IndigoSecondary,
                    trackColor = SlateSurfaceVariant
                )
                Spacer(modifier = Modifier.height(10.dp))
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(text = "Użyte miejsce", fontSize = 11.sp, color = Color.Gray)
                    Text(text = quotaUsed, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(text = "Przyznany limit", fontSize = 11.sp, color = Color.Gray)
                    Text(text = quotaLimit, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = CyanAccent)
                }
            }
        }
    }
}

@Composable
private fun TopProcessesCard(processes: List<ProcessInfo>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SlateSurface),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, SlateBorder)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Aktywne procesy",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Nagłówek tabeli
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SlateSurfaceVariant, RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            ) {
                Text(text = "PID", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.Gray, modifier = Modifier.width(46.dp))
                Text(text = "%CPU", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.Gray, modifier = Modifier.width(46.dp))
                Text(text = "%RAM", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.Gray, modifier = Modifier.width(46.dp))
                Text(text = "POLECENIE", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.Gray, modifier = Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(4.dp))

            if (processes.isEmpty()) {
                Text(
                    text = "Brak danych o procesach.",
                    fontSize = 12.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            } else {
                processes.take(10).forEach { proc ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = proc.pid,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = CyanAccent,
                            modifier = Modifier.width(46.dp)
                        )
                        Text(
                            text = String.format(Locale.US, "%.1f", proc.cpuPercentage),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = if (proc.cpuPercentage > 10f) RubyRed else Color.White,
                            modifier = Modifier.width(46.dp)
                        )
                        Text(
                            text = String.format(Locale.US, "%.1f", proc.memPercentage),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = Color.White,
                            modifier = Modifier.width(46.dp)
                        )
                        Text(
                            text = proc.command,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = Color(0xFFCBD5E1),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}
