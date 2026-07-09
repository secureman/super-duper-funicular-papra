package com.papra.mobile.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.papra.mobile.data.local.SessionStore
import com.papra.mobile.data.remote.dto.DocumentDto
import com.papra.mobile.data.remote.dto.OrganizationDto
import com.papra.mobile.data.repository.DocumentRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

enum class ViewMode { LIST, GRID }

data class HomeUiState(
    val organizations: List<OrganizationDto> = emptyList(),
    val activeOrganization: OrganizationDto? = null,
    val documents: List<DocumentDto> = emptyList(),
    val viewMode: ViewMode = ViewMode.GRID,
    val isLoading: Boolean = false,
    val error: String? = null,
    val searchQuery: String = "",
)

class HomeViewModel(
    private val documentRepository: DocumentRepository,
    private val sessionStore: SessionStore,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadOrganizationsThenDocuments()
    }

    private fun loadOrganizationsThenDocuments() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val orgs = documentRepository.listOrganizations()
                val savedOrgId = sessionStore.activeOrganizationId.first()
                val active = orgs.firstOrNull { it.id == savedOrgId } ?: orgs.firstOrNull()
                _uiState.value = _uiState.value.copy(organizations = orgs, activeOrganization = active)
                if (active != null) loadDocuments(active.id) else {
                    _uiState.value = _uiState.value.copy(isLoading = false)
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message ?: "Failed to load organizations")
            }
        }
    }

    fun switchOrganization(org: OrganizationDto) {
        viewModelScope.launch {
            sessionStore.setActiveOrganization(org.id)
            _uiState.value = _uiState.value.copy(activeOrganization = org)
            loadDocuments(org.id)
        }
    }

    fun toggleViewMode() {
        _uiState.value = _uiState.value.copy(
            viewMode = if (_uiState.value.viewMode == ViewMode.GRID) ViewMode.LIST else ViewMode.GRID,
        )
    }

    fun onSearchQueryChange(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
        _uiState.value.activeOrganization?.let { loadDocuments(it.id, query) }
    }

    fun refresh() {
        _uiState.value.activeOrganization?.let { loadDocuments(it.id, _uiState.value.searchQuery) }
    }

    private fun loadDocuments(organizationId: String, query: String = "") {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val docs = documentRepository.listDocuments(
                    organizationId = organizationId,
                    searchQuery = query.ifBlank { null },
                )
                _uiState.value = _uiState.value.copy(documents = docs, isLoading = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message ?: "Failed to load documents")
            }
        }
    }
}
