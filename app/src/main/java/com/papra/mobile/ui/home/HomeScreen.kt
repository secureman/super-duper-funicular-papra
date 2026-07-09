package com.papra.mobile.ui.home

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.papra.mobile.data.remote.dto.DocumentDto
import com.papra.mobile.ui.components.DocumentGridItem
import com.papra.mobile.ui.components.DocumentListItem
import com.papra.mobile.ui.components.OrgSwitcher
import com.papra.mobile.util.createCameraCaptureFile
import com.papra.mobile.util.resolveContentUriToFile

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onOpenDocument: (DocumentDto) -> Unit,
    onOpenSearch: () -> Unit,
) {
    val state by viewModel.uiState.collectAsState()
    var fabExpanded by remember { mutableStateOf(false) }
    val context = LocalContext.current

    // Holds the temp file the camera intent is asked to write into; needed
    // because TakePicture() only returns success/failure, not the Uri.
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
        if (success && file != null) {
            viewModel.uploadFile(file, "image/jpeg")
        }
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

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    OrgSwitcher(
                        organizations = state.organizations,
                        active = state.activeOrganization,
                        onSelect = viewModel::switchOrganization,
                    )
                },
                actions = {
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
                }
            }
        },
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            when {
                state.isLoading && state.documents.isEmpty() -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                state.error != null && state.documents.isEmpty() -> {
                    Text(
                        text = state.error ?: "Something went wrong",
                        modifier = Modifier.align(Alignment.Center).padding(24.dp),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                state.documents.isEmpty() -> {
                    Column(modifier = Modifier.align(Alignment.Center)) {
                        Text("No documents yet", style = MaterialTheme.typography.bodyLarge)
                    }
                }
                state.viewMode == ViewMode.GRID -> {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        contentPadding = PaddingValues(16.dp),
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        items(state.documents, key = { it.id }) { doc ->
                            Box(modifier = Modifier.padding(6.dp)) {
                                DocumentGridItem(
                                    document = doc,
                                    onClick = { onOpenDocument(doc) },
                                    onMoreClick = { /* TODO: context menu */ },
                                )
                            }
                        }
                    }
                }
                else -> {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(state.documents, key = { it.id }) { doc ->
                            DocumentListItem(
                                document = doc,
                                onClick = { onOpenDocument(doc) },
                                onMoreClick = { /* TODO: context menu */ },
                            )
                        }
                    }
                }
            }
        }
    }
}
