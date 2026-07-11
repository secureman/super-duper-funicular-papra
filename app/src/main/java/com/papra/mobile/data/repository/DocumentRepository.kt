package com.papra.mobile.data.repository

import com.papra.mobile.data.local.SessionStore
import com.papra.mobile.data.remote.ApiClientFactory
import com.papra.mobile.data.remote.dto.DocumentDto
import com.papra.mobile.data.remote.dto.OrganizationDto
import com.papra.mobile.data.remote.dto.TagDto
import kotlinx.coroutines.flow.first
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File

class DocumentRepository(private val sessionStore: SessionStore) {

    private suspend fun api() = ApiClientFactory.create(
        baseUrl = sessionStore.serverUrl.first() ?: error("No server configured"),
        sessionStore = sessionStore,
    )

    suspend fun listOrganizations(): List<OrganizationDto> =
        api().listOrganizations().organizations

    suspend fun listDocuments(
        organizationId: String,
        searchQuery: String? = null,
        pageIndex: Int = 0,
        sortField: String = "createdAt",
        sortOrder: String = "desc",
    ): List<DocumentDto> = api().listDocuments(
        organizationId = organizationId,
        searchQuery = searchQuery,
        pageIndex = pageIndex,
        sortField = sortField,
        sortOrder = sortOrder,
    ).documents

    suspend fun listTrashed(organizationId: String): List<DocumentDto> =
        api().listTrashedDocuments(organizationId).documents

    suspend fun getDocument(organizationId: String, documentId: String): DocumentDto =
        api().getDocument(organizationId, documentId).document

    suspend fun trashDocument(organizationId: String, documentId: String) {
        api().trashDocument(organizationId, documentId)
    }

    suspend fun renameDocument(organizationId: String, documentId: String, newName: String): DocumentDto =
        api().updateDocument(
            organizationId,
            documentId,
            com.papra.mobile.data.remote.dto.UpdateDocumentRequest(name = newName),
        ).document

    suspend fun listTags(organizationId: String): List<TagDto> =
        api().listTags(organizationId).tags

    suspend fun uploadDocument(
        organizationId: String,
        file: File,
        mimeType: String,
        folderId: String? = null,
    ): DocumentDto {
        val requestBody = file.asRequestBody(mimeType.toMediaType())
        val part = MultipartBody.Part.createFormData("file", file.name, requestBody)
        return api().uploadDocument(organizationId, part, folderId).document
    }

    /** Downloads a document's bytes to a local cache file, for preview or sharing. */
    suspend fun downloadDocumentFile(organizationId: String, documentId: String, destination: File): File {
        val response = api().getDocumentFile(organizationId, documentId)
        if (!response.isSuccessful) error("Download failed (${response.code()})")
        val body = response.body() ?: error("Empty response body")
        body.byteStream().use { input ->
            destination.outputStream().use { output -> input.copyTo(output) }
        }
        return destination
    }

    // --- Folders (requires papra-folders-feature.patch applied server-side) ---

    suspend fun listFolders(organizationId: String): List<com.papra.mobile.data.remote.dto.FolderDto> =
        api().listFolders(organizationId).folders

    suspend fun getFolderContents(
        organizationId: String,
        folderId: String?,
    ): com.papra.mobile.data.remote.dto.FolderContentsResponse =
        api().getFolderContents(organizationId, folderId)

    suspend fun createFolder(organizationId: String, name: String, parentId: String?): com.papra.mobile.data.remote.dto.FolderDto =
        api().createFolder(organizationId, com.papra.mobile.data.remote.dto.CreateFolderRequest(name, parentId)).folder

    suspend fun renameFolder(organizationId: String, folderId: String, newName: String): com.papra.mobile.data.remote.dto.FolderDto =
        api().updateFolder(organizationId, folderId, com.papra.mobile.data.remote.dto.UpdateFolderRequest(name = newName)).folder

    suspend fun moveDocumentToFolder(organizationId: String, documentId: String, folderId: String?): DocumentDto =
        api().updateDocument(
            organizationId,
            documentId,
            com.papra.mobile.data.remote.dto.UpdateDocumentRequest(folderId = folderId),
        ).document

    suspend fun deleteFolder(organizationId: String, folderId: String, force: Boolean = false) {
        api().deleteFolder(organizationId, folderId, force)
    }
}
