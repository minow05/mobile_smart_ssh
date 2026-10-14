package com.example.mobile_smart_ssh.ui.files

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.mobile_smart_ssh.core.model.RemoteFileItem
import com.example.mobile_smart_ssh.ui.theme.AmberWarning
import com.example.mobile_smart_ssh.ui.theme.CyanAccent
import com.example.mobile_smart_ssh.ui.theme.EmeraldPrimary
import com.example.mobile_smart_ssh.ui.theme.RubyRed
import com.example.mobile_smart_ssh.ui.theme.SlateBorder
import com.example.mobile_smart_ssh.ui.theme.SlateSurface
import com.example.mobile_smart_ssh.ui.theme.SlateSurfaceVariant

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileBrowserScreen(
    onNavigateBack: () -> Unit,
    viewModel: FileBrowserViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            viewModel.uploadFileFromDevice(uri, context)
        }
    }

    LaunchedEffect(state.statusMessage) {
        state.statusMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.clearStatusMessage()
        }
    }

    LaunchedEffect(state.error) {
        state.error?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.clearStatusMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Pliki i dyski sieciowe", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        val driveLabel = if (state.currentDrive == StorageDrive.HOME) "Dysk Domowy [minow]" else "Dysk Współdzielony [shared]"
                        Text(
                            text = "$driveLabel • ${state.activeProtocol}",
                            fontSize = 11.sp,
                            color = CyanAccent
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Wróć")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.loadDirectory() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Odśwież")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SlateSurface)
            )
        },
        floatingActionButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FloatingActionButton(
                    onClick = { viewModel.setCreateDirDialogVisible(true) },
                    containerColor = SlateSurfaceVariant,
                    contentColor = Color.White
                ) {
                    Icon(Icons.Default.CreateNewFolder, contentDescription = "Nowy Folder")
                }

                FloatingActionButton(
                    onClick = { filePickerLauncher.launch("*/*") },
                    containerColor = EmeraldPrimary,
                    contentColor = Color.Black
                ) {
                    Icon(Icons.Default.UploadFile, contentDescription = "Wyślij Plik")
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Zakładki wyboru udziału (Dysk Domowy [minow] vs Dysk Współdzielony [shared])
            DriveTabs(
                selectedDrive = state.currentDrive,
                onDriveSelected = viewModel::selectDrive
            )

            // Pasek ścieżki i nawigacji w górę
            PathBreadcrumbsBar(
                currentPath = state.currentPath,
                onNavigateUp = viewModel::navigateUp
            )

            // Pasek wskaźnika ładowania
            AnimatedVisibility(visible = state.isLoading) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = EmeraldPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Pobieranie plików z serwera...", fontSize = 12.sp, color = Color.Gray)
                }
            }

            // Lista plików i folderów
            if (state.items.isEmpty() && !state.isLoading) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Katalog jest pusty", color = Color.Gray, fontSize = 14.sp)
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    items(state.items) { item ->
                        RemoteFileListItem(
                            item = item,
                            onClick = {
                                if (item.isDirectory) {
                                    viewModel.navigateIntoFolder(item)
                                } else {
                                    viewModel.selectItemForAction(item)
                                }
                            },
                            onActionClick = {
                                viewModel.selectItemForAction(item)
                            }
                        )
                    }
                }
            }
        }
    }

    // Modalne okno akcji na wybranym pliku (Pobierz / Usuń)
    state.selectedItemForAction?.let { item ->
        FileActionDialog(
            item = item,
            onDismiss = { viewModel.selectItemForAction(null) },
            onDownload = {
                viewModel.downloadFileToDevice(item, context)
            },
            onDelete = {
                viewModel.deleteItem(item)
            }
        )
    }

    // Dialog tworzenia nowego folderu
    if (state.isCreateDirDialogVisible) {
        CreateFolderDialog(
            folderName = state.newDirName,
            onFolderNameChange = viewModel::setNewDirName,
            onDismiss = { viewModel.setCreateDirDialogVisible(false) },
            onCreate = { viewModel.createDirectory(state.newDirName) }
        )
    }
}

@Composable
private fun DriveTabs(
    selectedDrive: StorageDrive,
    onDriveSelected: (StorageDrive) -> Unit
) {
    TabRow(
        selectedTabIndex = selectedDrive.ordinal,
        containerColor = SlateSurface,
        contentColor = EmeraldPrimary,
        indicator = { tabPositions ->
            TabRowDefaults.SecondaryIndicator(
                Modifier.tabIndicatorOffset(tabPositions[selectedDrive.ordinal]),
                color = EmeraldPrimary
            )
        }
    ) {
        Tab(
            selected = selectedDrive == StorageDrive.HOME,
            onClick = { onDriveSelected(StorageDrive.HOME) },
            text = {
                Text(
                    text = "Dysk Domowy [minow]",
                    fontWeight = if (selectedDrive == StorageDrive.HOME) FontWeight.Bold else FontWeight.Normal,
                    fontSize = 13.sp
                )
            }
        )
        Tab(
            selected = selectedDrive == StorageDrive.SHARED,
            onClick = { onDriveSelected(StorageDrive.SHARED) },
            text = {
                Text(
                    text = "Dysk Współdzielony [shared]",
                    fontWeight = if (selectedDrive == StorageDrive.SHARED) FontWeight.Bold else FontWeight.Normal,
                    fontSize = 13.sp
                )
            }
        )
    }
}

@Composable
private fun PathBreadcrumbsBar(currentPath: String, onNavigateUp: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(SlateSurface)
            .border(0.5.dp, SlateBorder)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onNavigateUp,
            enabled = currentPath.isNotEmpty(),
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                Icons.Default.ArrowUpward,
                contentDescription = "Katalog w górę",
                tint = if (currentPath.isNotEmpty()) EmeraldPrimary else Color.DarkGray
            )
        }

        Spacer(modifier = Modifier.width(6.dp))

        Text(
            text = "/${currentPath.ifEmpty { "" }}",
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
            color = Color.White,
            maxLines = 1
        )
    }
}

@Composable
private fun RemoteFileListItem(
    item: RemoteFileItem,
    onClick: () -> Unit,
    onActionClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 3.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = SlateSurface),
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, SlateBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (item.isDirectory) Icons.Default.Folder else Icons.AutoMirrored.Filled.InsertDriveFile,
                contentDescription = null,
                tint = if (item.isDirectory) AmberWarning else CyanAccent,
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.name,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    color = Color.White,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row {
                    Text(
                        text = item.formattedSize,
                        fontSize = 11.sp,
                        color = Color.Gray,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "•  ${item.formattedDate}",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                }
            }

            IconButton(onClick = onActionClick, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.MoreVert, contentDescription = "Opcje", tint = Color.Gray)
            }
        }
    }
}

@Composable
private fun FileActionDialog(
    item: RemoteFileItem,
    onDismiss: () -> Unit,
    onDownload: () -> Unit,
    onDelete: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = item.name,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        },
        text = {
            Column {
                Text("Rozmiar: ${item.formattedSize}", fontSize = 13.sp)
                Text("Ostatnia modyfikacja: ${item.formattedDate}", fontSize = 13.sp)
                Spacer(modifier = Modifier.height(16.dp))
                Text("Wybierz akcję do wykonania:", fontSize = 13.sp, color = Color.Gray)
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (!item.isDirectory) {
                    Button(
                        onClick = onDownload,
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary, contentColor = Color.Black)
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Pobierz")
                    }
                }

                Button(
                    onClick = onDelete,
                    colors = ButtonDefaults.buttonColors(containerColor = RubyRed, contentColor = Color.White)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Usuń")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Anuluj")
            }
        }
    )
}

@Composable
private fun CreateFolderDialog(
    folderName: String,
    onFolderNameChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onCreate: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Utwórz nowy katalog") },
        text = {
            Column {
                Text("Wprowadź nazwę nowego folderu na serwerze:", fontSize = 13.sp)
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = folderName,
                    onValueChange = onFolderNameChange,
                    label = { Text("Nazwa folderu") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onCreate,
                enabled = folderName.isNotBlank()
            ) {
                Text("Utwórz")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Anuluj")
            }
        }
    )
}
