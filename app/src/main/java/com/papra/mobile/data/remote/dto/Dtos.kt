package com.papra.mobile.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class OrganizationDto(
    val id: String,
    val name: String,
)

@Serializable
data class OrganizationsResponse(
    val organizations: List<OrganizationDto> = emptyList(),
)

@Serializable
data class TagDto(
    val id: String,
    val name: String,
    val color: String? = null,
)

@Serializable
data class TagsResponse(
    val tags: List<TagDto> = emptyList(),
)

@Serializable
data class DocumentDto(
    val id: String,
    val name: String,
    @SerialName("organizationId") val organizationId: String,
    @SerialName("originalName") val originalName: String? = null,
    @SerialName("mimeType") val mimeType: String? = null,
    @SerialName("originalSize") val size: Long? = null,
    @SerialName("createdAt") val createdAt: String? = null,
    @SerialName("updatedAt") val updatedAt: String? = null,
    @SerialName("documentDate") val documentDate: String? = null,
    val tags: List<TagDto> = emptyList(),
    val notes: String? = null,
)

@Serializable
data class DocumentsListResponse(
    val documents: List<DocumentDto> = emptyList(),
    @SerialName("documentsCount") val documentsCount: Int = 0,
)

// GET/PATCH/POST document endpoints wrap the payload in a "document" key.
@Serializable
data class DocumentEnvelope(val document: DocumentDto)

@Serializable
data class DocumentStatisticsResponse(
    @SerialName("organizationStats") val organizationStats: OrganizationStats = OrganizationStats(),
)

@Serializable
data class OrganizationStats(
    @SerialName("documentsCount") val documentsCount: Int = 0,
    @SerialName("documentsSize") val documentsSize: Long = 0,
)

@Serializable
data class UpdateDocumentRequest(
    val name: String? = null,
    val content: String? = null,
    @SerialName("folderId") val folderId: String? = null,
)

// --- Folders (requires papra-folders-feature.patch applied server-side) ---

@Serializable
data class FolderDto(
    val id: String,
    @SerialName("organizationId") val organizationId: String,
    @SerialName("parentId") val parentId: String? = null,
    val name: String,
    @SerialName("documentsCount") val documentsCount: Int = 0,
    @SerialName("createdAt") val createdAt: String? = null,
    @SerialName("updatedAt") val updatedAt: String? = null,
)

@Serializable
data class FoldersResponse(val folders: List<FolderDto> = emptyList())

@Serializable
data class FolderContentsResponse(
    val folders: List<FolderDto> = emptyList(),
    val documents: List<DocumentDto> = emptyList(),
)

@Serializable
data class FolderEnvelope(val folder: FolderDto)

@Serializable
data class CreateFolderRequest(val name: String, val parentId: String? = null)

@Serializable
data class UpdateFolderRequest(val name: String? = null, val parentId: String? = null)

// --- Better Auth email/password sign-in ---

@Serializable
data class EmailSignInRequest(
    val email: String,
    val password: String,
)

@Serializable
data class EmailSignInResponse(
    val token: String? = null,
    val user: AuthUserDto? = null,
)

@Serializable
data class AuthUserDto(
    val id: String,
    val email: String,
    val name: String? = null,
)
