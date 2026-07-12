package com.papra.mobile.ui.document

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.papra.mobile.data.remote.dto.DocumentDto
import com.papra.mobile.ui.components.fileVisualFor
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentDetailScreen(
    documentId: String,
    viewModel: DocumentDetailViewModel,
    onBack: () -> Unit,
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var showTagDialog by remember { mutableStateOf(false) }

    LaunchedEffect(documentId) { viewModel.load(documentId) }

    fun fileUrl(doc: DocumentDto): String? {
        val serverUrl = state.serverUrl ?: return null
        return "$serverUrl/api/organizations/${doc.organizationId}/documents/${doc.id}/file"
    }

    fun openExternally(doc: DocumentDto) {
        coroutineScope.launch {
            val dest = java.io.File(context.cacheDir, doc.name)
            val file = viewModel.downloadToFile(doc, dest)
            if (file != null) {
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, doc.mimeType ?: "*/*")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(intent, "Open ${doc.name}"))
            }
        }
    }

    fun shareExternally(doc: DocumentDto) {
        coroutineScope.launch {
            val dest = java.io.File(context.cacheDir, doc.name)
            val file = viewModel.downloadToFile(doc, dest)
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.document?.name ?: "Document", maxLines = 1) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    state.document?.let { doc ->
                        IconButton(onClick = { shareExternally(doc) }) {
                            Icon(Icons.Filled.Share, contentDescription = "Share")
                        }
                    }
                    IconButton(onClick = { viewModel.trash(documentId, onDeleted = onBack) }) {
                        Icon(Icons.Filled.Delete, contentDescription = "Move to trash")
                    }
                },
            )
        },
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            when {
                state.isLoading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                state.document != null -> {
                    val doc = state.document!!
                    val visual = fileVisualFor(doc.mimeType)
                    val isImage = doc.mimeType?.startsWith("image/") == true
                    val url = fileUrl(doc)

                    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(4f / 3f),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (isImage && url != null) {
                                AsyncImage(
                                    model = url,
                                    contentDescription = doc.name,
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier.fillMaxSize(),
                                )
                            } else {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = visual.icon,
                                        contentDescription = null,
                                        tint = visual.color,
                                        modifier = Modifier.fillMaxWidth(0.3f).aspectRatio(1f),
                                    )
                                    androidx.compose.foundation.layout.Spacer(Modifier.padding(top = 12.dp))
                                    Button(onClick = { openExternally(doc) }) {
                                        Icon(Icons.Filled.OpenInNew, contentDescription = null)
                                        androidx.compose.foundation.layout.Spacer(Modifier.padding(start = 4.dp))
                                        Text("Open")
                                    }
                                }
                            }
                        }

                        Text(doc.name, style = MaterialTheme.typography.titleLarge)

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(top = 12.dp),
                        ) {
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(doc.tags) { tag ->
                                    InputChip(
                                        selected = false,
                                        onClick = {},
                                        label = { Text(tag.name) },
                                        trailingIcon = {
                                            IconButton(
                                                onClick = { viewModel.removeTag(tag.id) },
                                                modifier = Modifier.size(18.dp),
                                            ) {
                                                Icon(Icons.Filled.Close, contentDescription = "Remove tag")
                                            }
                                        },
                                    )
                                }
                            }
                            Spacer(Modifier.width(8.dp))
                            AssistChip(
                                onClick = {
                                    viewModel.loadAvailableTags()
                                    showTagDialog = true
                                },
                                label = { Text("Add tag") },
                                leadingIcon = { Icon(Icons.Filled.Add, contentDescription = null) },
                            )
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

                        DetailRow("Type", doc.mimeType ?: "Unknown")
                        DetailRow("Size", formatFileSize(doc.size))
                        doc.createdAt?.let { DetailRow("Created", it) }
                        doc.updatedAt?.let { DetailRow("Updated", it) }

                        doc.notes?.let {
                            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
                            Text("Notes", style = MaterialTheme.typography.titleMedium)
                            Text(it, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 4.dp))
                        }
                    }
                }
                state.error != null -> {
                    Text(
                        state.error ?: "Failed to load document",
                        modifier = Modifier.align(Alignment.Center).padding(24.dp),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
    }

    if (showTagDialog) {
        val doc = state.document
        val currentTagIds = doc?.tags?.map { it.id }?.toSet() ?: emptySet()
        val pickableTags = state.availableTags.filter { it.id !in currentTagIds }
        var newTagName by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showTagDialog = false },
            title = { Text("Add tag") },
            text = {
                Column {
                    if (pickableTags.isNotEmpty()) {
                        Text("Existing tags", style = MaterialTheme.typography.labelLarge)
                        Row(modifier = Modifier.padding(top = 8.dp, bottom = 16.dp)) {
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(pickableTags) { tag ->
                                    AssistChip(
                                        onClick = {
                                            viewModel.addTag(tag.id)
                                            showTagDialog = false
                                        },
                                        label = { Text(tag.name) },
                                    )
                                }
                            }
                        }
                    }
                    Text("Create new tag", style = MaterialTheme.typography.labelLarge)
                    OutlinedTextField(
                        value = newTagName,
                        onValueChange = { newTagName = it },
                        singleLine = true,
                        label = { Text("Tag name") },
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            },
            confirmButton = {
                TextButton(
                    enabled = newTagName.isNotBlank(),
                    onClick = {
                        // Deterministic color per name keeps it simple -- no color
                        // picker UI, but still gives tags visually distinct colors.
                        val palette = listOf("#EA4335", "#4285F4", "#34A853", "#FBBC05", "#9C27B0", "#00ACC1")
                        val color = palette[Math.floorMod(newTagName.hashCode(), palette.size)]
                        viewModel.createAndAddTag(newTagName, color)
                        showTagDialog = false
                    },
                ) { Text("Create & add") }
            },
            dismissButton = {
                TextButton(onClick = { showTagDialog = false }) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    androidx.compose.foundation.layout.Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}

private fun formatFileSize(bytes: Long?): String {
    if (bytes == null) return "Unknown"
    val kb = 1024.0
    return when {
        bytes < kb -> "$bytes B"
        bytes < kb * kb -> String.format("%.1f KB", bytes / kb)
        bytes < kb * kb * kb -> String.format("%.1f MB", bytes / (kb * kb))
        else -> String.format("%.1f GB", bytes / (kb * kb * kb))
    }
}
