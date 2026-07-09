package com.papra.mobile.ui.document

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.papra.mobile.data.remote.dto.DocumentDto
import com.papra.mobile.data.repository.DocumentRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class DocumentDetailUiState(
    val document: DocumentDto? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
)

class DocumentDetailViewModel(
    private val documentRepository: DocumentRepository,
    private val organizationId: String,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DocumentDetailUiState())
    val uiState: StateFlow<DocumentDetailUiState> = _uiState.asStateFlow()

    fun load(documentId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val doc = documentRepository.getDocument(organizationId, documentId)
                _uiState.value = _uiState.value.copy(document = doc, isLoading = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message ?: "Failed to load document")
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
}
