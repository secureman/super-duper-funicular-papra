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
import okhttp3.RequestBody.Companion.toRequestBody
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
        api().trashDocument(organizationId, documentId).throwIfError("Delete document")
    }

    suspend fun renameDocument(organizationId: String, documentId: String, newName: String): DocumentDto =
        api().updateDocument(
            organizationId,
            documentId,
            com.papra.mobile.data.remote.dto.UpdateDocumentRequest(name = newName),
        ).document

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
        api().deleteFolder(organizationId, folderId, force).throwIfError("Delete folder")
    }

    // --- Tags ---

    suspend fun listTags(organizationId: String): List<TagDto> =
        api().listTags(organizationId).tags

    suspend fun createTag(organizationId: String, name: String, color: String): TagDto {
        val plainText = "text/plain".toMediaType()
        return try {
            api().createTag(
                organizationId,
                name = name.toRequestBody(plainText),
                color = color.toRequestBody(plainText),
            ).tag
        } catch (e: retrofit2.HttpException) {
            // Retrofit auto-throws HttpException for calls typed to return the body
            // directly, but its .message is just the generic status phrase ("HTTP 400
            // Bad Request") -- it drops the response body, which is where the server
            // actually explains what was wrong with the request. Unwrap it explicitly.
            val detail = e.response()?.errorBody()?.string()?.take(300)
            throw IllegalStateException("Create tag failed (${e.code()})${if (detail != null) ": $detail" else ""}", e)
        }
    }

    suspend fun deleteTag(organizationId: String, tagId: String) {
        api().deleteTag(organizationId, tagId).throwIfError("Delete tag")
    }

    suspend fun addTagToDocument(organizationId: String, documentId: String, tagId: String) {
        api().addTagToDocument(organizationId, documentId, com.papra.mobile.data.remote.dto.AddTagToDocumentRequest(tagId))
            .throwIfError("Add tag")
    }

    suspend fun removeTagFromDocument(organizationId: String, documentId: String, tagId: String) {
        api().removeTagFromDocument(organizationId, documentId, tagId).throwIfError("Remove tag")
    }

    /** Retrofit only auto-throws on failure for calls whose return type IS the body;
     *  calls typed to return a raw Response<T> (used here for empty-body endpoints)
     *  treat 4xx/5xx as a normal, non-exceptional result unless checked manually.
     *  Without this, a rejected request (missing permission, conflict, etc.) looks
     *  identical to success from the caller's point of view. */
    private fun retrofit2.Response<Unit>.throwIfError(action: String) {
        if (!isSuccessful) {
            val detail = errorBody()?.string()?.take(300)
            error("$action failed (${code()})${if (detail != null) ": $detail" else ""}")
        }
    }
}
