package com.papra.mobile.ui.document

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.papra.mobile.data.local.SessionStore
import com.papra.mobile.data.remote.dto.DocumentDto
import com.papra.mobile.data.repository.DocumentRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class DocumentDetailUiState(
    val document: DocumentDto? = null,
    val serverUrl: String? = null,
    val availableTags: List<com.papra.mobile.data.remote.dto.TagDto> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
)

class DocumentDetailViewModel(
    private val documentRepository: DocumentRepository,
    private val sessionStore: SessionStore,
    private val organizationId: String,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DocumentDetailUiState())
    val uiState: StateFlow<DocumentDetailUiState> = _uiState.asStateFlow()

    fun load(documentId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val serverUrl = sessionStore.serverUrl.first()
                val doc = documentRepository.getDocument(organizationId, documentId)
                _uiState.value = _uiState.value.copy(document = doc, serverUrl = serverUrl, isLoading = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message ?: "Failed to load document")
            }
        }
    }

    fun loadAvailableTags() {
        viewModelScope.launch {
            try {
                val tags = documentRepository.listTags(organizationId)
                _uiState.value = _uiState.value.copy(availableTags = tags)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message ?: "Failed to load tags")
            }
        }
    }

    fun addTag(tagId: String) {
        val doc = _uiState.value.document ?: return
        viewModelScope.launch {
            try {
                documentRepository.addTagToDocument(organizationId, doc.id, tagId)
                load(doc.id)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message ?: "Failed to add tag")
            }
        }
    }

    fun removeTag(tagId: String) {
        val doc = _uiState.value.document ?: return
        viewModelScope.launch {
            try {
                documentRepository.removeTagFromDocument(organizationId, doc.id, tagId)
                load(doc.id)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message ?: "Failed to remove tag")
            }
        }
    }

    /** Creates a brand-new org tag and immediately attaches it to the current document. */
    fun createAndAddTag(name: String, colorHex: String) {
        val doc = _uiState.value.document ?: return
        viewModelScope.launch {
            try {
                val tag = documentRepository.createTag(organizationId, name, colorHex)
                documentRepository.addTagToDocument(organizationId, doc.id, tag.id)
                load(doc.id)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message ?: "Failed to create tag")
            }
        }
    }

    fun trash(documentId: String, onDeleted: () -> Unit) {
        viewModelScope.launch {
            try {
                documentRepository.trashDocument(organizationId, documentId)
                onDeleted()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message ?: "Failed to delete document")
            }
        }
    }

    suspend fun downloadToFile(document: DocumentDto, destination: java.io.File): java.io.File? = try {
        documentRepository.downloadDocumentFile(organizationId, document.id, destination)
    } catch (e: Exception) {
        _uiState.value = _uiState.value.copy(error = e.message ?: "Download failed")
        null
    }
}
