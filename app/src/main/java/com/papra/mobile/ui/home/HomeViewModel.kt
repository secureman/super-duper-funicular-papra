package com.papra.mobile.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.papra.mobile.data.local.SessionStore
import com.papra.mobile.data.remote.dto.DocumentDto
import com.papra.mobile.data.remote.dto.FolderDto
import com.papra.mobile.data.remote.dto.OrganizationDto
import com.papra.mobile.data.repository.DocumentRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import retrofit2.HttpException

enum class ViewMode { LIST, GRID }

data class HomeUiState(
    val organizations: List<OrganizationDto> = emptyList(),
    val activeOrganization: OrganizationDto? = null,
    val serverUrl: String? = null,
    // Folder navigation. null folderId means the organization root.
    val currentFolderId: String? = null,
    val breadcrumb: List<FolderDto> = emptyList(),
    val folders: List<FolderDto> = emptyList(),
    val foldersSupported: Boolean = true,
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

    private var hasLoadedInitialData = false

    fun loadInitialData() {
        if (hasLoadedInitialData) return
        hasLoadedInitialData = true
        loadOrganizationsThenDocuments()
    }

    private fun loadOrganizationsThenDocuments() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val serverUrl = sessionStore.serverUrl.first()
                val orgs = documentRepository.listOrganizations()
                val savedOrgId = sessionStore.activeOrganizationId.first()
                val active = orgs.firstOrNull { it.id == savedOrgId } ?: orgs.firstOrNull()
                _uiState.value = _uiState.value.copy(
                    organizations = orgs,
                    activeOrganization = active,
                    serverUrl = serverUrl,
                )
                if (active != null) {
                    loadFolder(active.id, folderId = null, breadcrumb = emptyList())
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = if (orgs.isEmpty()) {
                            "No organizations found for this account. Check that sign-in " +
                                "succeeded and the account has at least one organization."
                        } else null,
                    )
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
            loadFolder(org.id, folderId = null, breadcrumb = emptyList())
        }
    }

    fun toggleViewMode() {
        _uiState.value = _uiState.value.copy(
            viewMode = if (_uiState.value.viewMode == ViewMode.GRID) ViewMode.LIST else ViewMode.GRID,
        )
    }

    fun onSearchQueryChange(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
        val org = _uiState.value.activeOrganization ?: return
        viewModelScope.launch {
            try {
                val docs = documentRepository.listDocuments(org.id, searchQuery = query.ifBlank { null })
                _uiState.value = _uiState.value.copy(documents = docs)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message ?: "Search failed")
            }
        }
    }

    fun refresh() {
        val org = _uiState.value.activeOrganization ?: return
        loadFolder(org.id, _uiState.value.currentFolderId, _uiState.value.breadcrumb)
    }

    /** Navigate into a subfolder, pushing it onto the breadcrumb. */
    fun openFolder(folder: FolderDto) {
        val org = _uiState.value.activeOrganization ?: return
        loadFolder(org.id, folder.id, _uiState.value.breadcrumb + folder)
    }

    /** Jump to an arbitrary point in the breadcrumb (or root if index is -1). */
    fun navigateToBreadcrumb(index: Int) {
        val org = _uiState.value.activeOrganization ?: return
        if (index < 0) {
            loadFolder(org.id, null, emptyList())
        } else {
            val newPath = _uiState.value.breadcrumb.subList(0, index + 1)
            loadFolder(org.id, newPath.last().id, newPath)
        }
    }

    /** One level up from the current folder -- used to make the system back button
     *  step up through folders instead of exiting the app. */
    fun navigateUp() {
        val path = _uiState.value.breadcrumb
        if (path.isEmpty()) return
        navigateToBreadcrumb(path.size - 2)
    }

    fun createFolder(name: String) {
        val org = _uiState.value.activeOrganization ?: return
        viewModelScope.launch {
            try {
                documentRepository.createFolder(org.id, name, _uiState.value.currentFolderId)
                loadFolder(org.id, _uiState.value.currentFolderId, _uiState.value.breadcrumb)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message ?: "Failed to create folder")
            }
        }
    }

    fun deleteFolder(folder: FolderDto, force: Boolean = false) {
        val org = _uiState.value.activeOrganization ?: return
        viewModelScope.launch {
            try {
                documentRepository.deleteFolder(org.id, folder.id, force)
                loadFolder(org.id, _uiState.value.currentFolderId, _uiState.value.breadcrumb)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message ?: "Failed to delete folder")
            }
        }
    }

    fun renameDocument(document: DocumentDto, newName: String) {
        val org = _uiState.value.activeOrganization ?: return
        viewModelScope.launch {
            try {
                documentRepository.renameDocument(org.id, document.id, newName)
                loadFolder(org.id, _uiState.value.currentFolderId, _uiState.value.breadcrumb)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message ?: "Rename failed")
            }
        }
    }

    fun trashDocument(document: DocumentDto) {
        val org = _uiState.value.activeOrganization ?: return
        viewModelScope.launch {
            try {
                documentRepository.trashDocument(org.id, document.id)
                loadFolder(org.id, _uiState.value.currentFolderId, _uiState.value.breadcrumb)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message ?: "Delete failed")
            }
        }
    }

    /** Called after a file picker returns a resolved File + mimeType, or a camera capture completes. */
    fun uploadFile(file: java.io.File, mimeType: String) {
        val org = _uiState.value.activeOrganization
        if (org == null) {
            _uiState.value = _uiState.value.copy(error = "Select an organization first")
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                documentRepository.uploadDocument(org.id, file, mimeType, _uiState.value.currentFolderId)
                loadFolder(org.id, _uiState.value.currentFolderId, _uiState.value.breadcrumb)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message ?: "Upload failed")
            }
        }
    }

    suspend fun downloadDocumentToFile(document: DocumentDto, destination: java.io.File): java.io.File? {
        val org = _uiState.value.activeOrganization ?: return null
        return try {
            documentRepository.downloadDocumentFile(org.id, document.id, destination)
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(error = e.message ?: "Download failed")
            null
        }
    }

    private fun loadFolder(organizationId: String, folderId: String?, breadcrumb: List<FolderDto>) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null, currentFolderId = folderId, breadcrumb = breadcrumb)
            try {
                // Folder endpoints require the papra-folders-feature.patch to be applied
                // server-side. If it's not, this 404s and we fall back to the flat
                // document list so the app stays usable either way.
                val contents = documentRepository.getFolderContents(organizationId, folderId)
                _uiState.value = _uiState.value.copy(
                    folders = contents.folders,
                    documents = contents.documents,
                    foldersSupported = true,
                    isLoading = false,
                )
            } catch (e: HttpException) {
                if (e.code() == 404) {
                    loadFlatFallback(organizationId)
                } else {
                    _uiState.value = _uiState.value.copy(isLoading = false, error = e.message ?: "Failed to load folder")
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message ?: "Failed to load folder")
            }
        }
    }

    private suspend fun loadFlatFallback(organizationId: String) {
        try {
            val docs = documentRepository.listDocuments(organizationId)
            _uiState.value = _uiState.value.copy(
                folders = emptyList(),
                documents = docs,
                foldersSupported = false,
                currentFolderId = null,
                breadcrumb = emptyList(),
                isLoading = false,
            )
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(isLoading = false, error = e.message ?: "Failed to load documents")
        }
    }
}
