package com.papra.mobile.ui.search

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.papra.mobile.data.remote.dto.DocumentDto
import com.papra.mobile.ui.components.DocumentListItem
import com.papra.mobile.ui.home.HomeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    homeViewModel: HomeViewModel,
    onOpenDocument: (DocumentDto) -> Unit,
    onBack: () -> Unit,
) {
    val state by homeViewModel.uiState.collectAsState()

    fun thumbnailUrlFor(doc: DocumentDto): String? {
        val serverUrl = state.serverUrl ?: return null
        return "$serverUrl/api/organizations/${doc.organizationId}/documents/${doc.id}/file"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    TextField(
                        value = state.searchQuery,
                        onValueChange = homeViewModel::onSearchQueryChange,
                        placeholder = { Text("Search documents, tags, content...") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding).fillMaxSize()) {
            items(state.documents, key = { it.id }) { doc ->
                DocumentListItem(
                    document = doc,
                    thumbnailUrl = thumbnailUrlFor(doc),
                    onClick = { onOpenDocument(doc) },
                    onRename = { newName -> homeViewModel.renameDocument(doc, newName) },
                    onDelete = { homeViewModel.trashDocument(doc) },
                    onShare = { /* handled from Home; search results reuse the same repo state */ },
                    onMove = { /* move-to-folder picker lives on Home; not duplicated here */ },
                )
            }
        }
    }
}
