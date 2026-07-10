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

    suspend fun uploadDocument(organizationId: String, file: File, mimeType: String): DocumentDto {
        val requestBody = file.asRequestBody(mimeType.toMediaType())
        val part = MultipartBody.Part.createFormData("file", file.name, requestBody)
        return api().uploadDocument(organizationId, part).document
    }
}
