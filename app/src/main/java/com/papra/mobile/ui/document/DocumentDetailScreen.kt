package com.papra.mobile.ui.document

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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

                        if (doc.tags.isNotEmpty()) {
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.padding(top = 12.dp),
                            ) {
                                items(doc.tags) { tag ->
                                    SuggestionChip(onClick = {}, label = { Text(tag.name) })
                                }
                            }
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
