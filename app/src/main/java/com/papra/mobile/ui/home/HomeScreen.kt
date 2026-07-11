package com.papra.mobile.ui.home

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.papra.mobile.data.remote.dto.DocumentDto
import com.papra.mobile.ui.components.DocumentGridItem
import com.papra.mobile.ui.components.DocumentListItem
import com.papra.mobile.ui.components.FolderGridItem
import com.papra.mobile.ui.components.OrgSwitcher
import com.papra.mobile.util.createCameraCaptureFile
import com.papra.mobile.util.resolveContentUriToFile
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onOpenDocument: (DocumentDto) -> Unit,
    onOpenSearch: () -> Unit,
) {
    val state by viewModel.uiState.collectAsState()
    var fabExpanded by remember { mutableStateOf(false) }
    var showNewFolderDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) { viewModel.loadInitialData() }

    var pendingCaptureFile by remember { mutableStateOf<java.io.File?>(null) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            val resolved = resolveContentUriToFile(context, uri)
            viewModel.uploadFile(resolved.file, resolved.mimeType)
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture(),
    ) { success ->
        val file = pendingCaptureFile
        if (success && file != null) viewModel.uploadFile(file, "image/jpeg")
        pendingCaptureFile = null
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            val file = createCameraCaptureFile(context)
            pendingCaptureFile = file
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            cameraLauncher.launch(uri)
        }
    }

    fun launchScan() {
        val hasPermission = androidx.core.content.ContextCompat.checkSelfPermission(
            context, Manifest.permission.CAMERA,
        ) == PackageManager.PERMISSION_GRANTED
        if (hasPermission) {
            val file = createCameraCaptureFile(context)
            pendingCaptureFile = file
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            cameraLauncher.launch(uri)
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    fun shareDocument(doc: DocumentDto) {
        coroutineScope.launch {
            val dest = java.io.File(context.cacheDir, doc.name)
            val file = viewModel.downloadDocumentToFile(doc, dest)
            if (file != null) {
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = doc.mimeType ?: "*/*"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(intent, "Share ${doc.name}"))
            }
        }
    }

    fun thumbnailUrlFor(doc: DocumentDto): String? {
        val serverUrl = state.serverUrl ?: return null
        return "$serverUrl/api/organizations/${doc.organizationId}/documents/${doc.id}/file"
    }

    Scaffold(
        topBar = {
            Column {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            text = state.activeOrganization?.name ?: "Papra",
                            style = MaterialTheme.typography.titleMedium,
                        )
                    },
                    actions = {
                        OrgSwitcher(
                            organizations = state.organizations,
                            active = state.activeOrganization,
                            onSelect = viewModel::switchOrganization,
                        )
                        IconButton(onClick = onOpenSearch) {
                            Icon(Icons.Filled.Search, contentDescription = "Search")
                        }
                        IconButton(onClick = viewModel::toggleViewMode) {
                            Icon(
                                imageVector = if (state.viewMode == ViewMode.GRID) Icons.Filled.ViewList else Icons.Filled.GridView,
                                contentDescription = "Toggle view",
                            )
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                    ),
                )
                if (state.foldersSupported) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
                    ) {
                        TextButton(onClick = { viewModel.navigateToBreadcrumb(-1) }) { Text("Home") }
                        state.breadcrumb.forEachIndexed { index, folder ->
                            Icon(Icons.Filled.ChevronRight, contentDescription = null, modifier = Modifier.size(16.dp))
                            TextButton(onClick = { viewModel.navigateToBreadcrumb(index) }) { Text(folder.name) }
                        }
                    }
                }
            }
        },
        floatingActionButton = {
            Box {
                ExtendedFloatingActionButton(
                    onClick = { fabExpanded = true },
                    icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                    text = { Text("Add") },
                )
                DropdownMenu(expanded = fabExpanded, onDismissRequest = { fabExpanded = false }) {
                    DropdownMenuItem(
                        text = { Text("Upload file") },
                        leadingIcon = { Icon(Icons.Filled.CloudUpload, contentDescription = null) },
                        onClick = { fabExpanded = false; filePickerLauncher.launch(arrayOf("*/*")) },
                    )
                    DropdownMenuItem(
                        text = { Text("Scan document") },
                        leadingIcon = { Icon(Icons.Filled.CameraAlt, contentDescription = null) },
                        onClick = { fabExpanded = false; launchScan() },
                    )
                    if (state.foldersSupported) {
                        DropdownMenuItem(
                            text = { Text("New folder") },
                            leadingIcon = { Icon(Icons.Filled.CreateNewFolder, contentDescription = null) },
                            onClick = { fabExpanded = false; showNewFolderDialog = true },
                        )
                    }
                }
            }
        },
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            when {
                state.isLoading && state.documents.isEmpty() && state.folders.isEmpty() -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                state.error != null && state.documents.isEmpty() && state.folders.isEmpty() -> {
                    Text(
                        text = state.error ?: "Something went wrong",
                        modifier = Modifier.align(Alignment.Center).padding(24.dp),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                state.documents.isEmpty() && state.folders.isEmpty() -> {
                    Column(modifier = Modifier.align(Alignment.Center)) {
                        Text("Nothing here yet", style = MaterialTheme.typography.bodyLarge)
                    }
                }
                state.viewMode == ViewMode.GRID -> {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        contentPadding = PaddingValues(16.dp),
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        items(state.folders, key = { "folder-${it.id}" }) { folder ->
                            Box(modifier = Modifier.padding(6.dp)) {
                                FolderGridItem(
                                    name = folder.name,
                                    documentsCount = folder.documentsCount,
                                    onClick = { viewModel.openFolder(folder) },
                                    onDelete = { viewModel.deleteFolder(folder) },
                                )
                            }
                        }
                        items(state.documents, key = { it.id }) { doc ->
                            Box(modifier = Modifier.padding(6.dp)) {
                                DocumentGridItem(
                                    document = doc,
                                    thumbnailUrl = thumbnailUrlFor(doc),
                                    onClick = { onOpenDocument(doc) },
                                    onRename = { newName -> viewModel.renameDocument(doc, newName) },
                                    onDelete = { viewModel.trashDocument(doc) },
                                    onShare = { shareDocument(doc) },
                                )
                            }
                        }
                    }
                }
                else -> {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(state.folders, key = { "folder-${it.id}" }) { folder ->
                            Box(modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)) {
                                FolderGridItem(
                                    name = folder.name,
                                    documentsCount = folder.documentsCount,
                                    onClick = { viewModel.openFolder(folder) },
                                    onDelete = { viewModel.deleteFolder(folder) },
                                )
                            }
                        }
                        items(state.documents, key = { it.id }) { doc ->
                            DocumentListItem(
                                document = doc,
                                thumbnailUrl = thumbnailUrlFor(doc),
                                onClick = { onOpenDocument(doc) },
                                onRename = { newName -> viewModel.renameDocument(doc, newName) },
                                onDelete = { viewModel.trashDocument(doc) },
                                onShare = { shareDocument(doc) },
                            )
                        }
                    }
                }
            }
        }
    }

    if (showNewFolderDialog) {
        var name by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showNewFolderDialog = false },
            title = { Text("New folder") },
            text = { OutlinedTextField(value = name, onValueChange = { name = it }, singleLine = true, label = { Text("Folder name") }) },
            confirmButton = {
                TextButton(onClick = {
                    showNewFolderDialog = false
                    if (name.isNotBlank()) viewModel.createFolder(name)
                }) { Text("Create") }
            },
            dismissButton = {
                TextButton(onClick = { showNewFolderDialog = false }) { Text("Cancel") }
            },
        )
    }
}
