package com.papra.mobile.data.remote

import com.papra.mobile.data.remote.dto.DocumentEnvelope
import com.papra.mobile.data.remote.dto.DocumentStatisticsResponse
import com.papra.mobile.data.remote.dto.DocumentsListResponse
import com.papra.mobile.data.remote.dto.EmailSignInRequest
import com.papra.mobile.data.remote.dto.EmailSignInResponse
import com.papra.mobile.data.remote.dto.OrganizationsResponse
import com.papra.mobile.data.remote.dto.TagsResponse
import com.papra.mobile.data.remote.dto.UpdateDocumentRequest
import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Streaming

/**
 * Mirrors the endpoints documented at https://docs.papra.app/resources/api-endpoints/
 * Base URL is the user's self-hosted (or fork-hosted) Papra server, set at
 * runtime via SessionStore rather than baked into the client.
 */
interface PapraApiService {

    // --- Auth (Better Auth) ---
    @POST("api/auth/sign-in/email")
    suspend fun signInWithEmail(@Body body: EmailSignInRequest): Response<EmailSignInResponse>

    // --- Organizations ---
    @GET("api/organizations")
    suspend fun listOrganizations(): OrganizationsResponse

    // --- Documents ---
    @GET("api/organizations/{organizationId}/documents")
    suspend fun listDocuments(
        @Path("organizationId") organizationId: String,
        @Query("searchQuery") searchQuery: String? = null,
        @Query("pageIndex") pageIndex: Int = 0,
        @Query("pageSize") pageSize: Int = 50,
        @Query("sortField") sortField: String = "createdAt",
        @Query("sortOrder") sortOrder: String = "desc",
    ): DocumentsListResponse

    @GET("api/organizations/{organizationId}/documents/deleted")
    suspend fun listTrashedDocuments(
        @Path("organizationId") organizationId: String,
        @Query("pageIndex") pageIndex: Int = 0,
        @Query("pageSize") pageSize: Int = 50,
    ): DocumentsListResponse

    @GET("api/organizations/{organizationId}/documents/{documentId}")
    suspend fun getDocument(
        @Path("organizationId") organizationId: String,
        @Path("documentId") documentId: String,
    ): DocumentEnvelope

    @Streaming
    @GET("api/organizations/{organizationId}/documents/{documentId}/file")
    suspend fun getDocumentFile(
        @Path("organizationId") organizationId: String,
        @Path("documentId") documentId: String,
    ): Response<ResponseBody>

    @Multipart
    @POST("api/organizations/{organizationId}/documents")
    suspend fun uploadDocument(
        @Path("organizationId") organizationId: String,
        @Part file: MultipartBody.Part,
    ): DocumentEnvelope

    @PATCH("api/organizations/{organizationId}/documents/{documentId}")
    suspend fun updateDocument(
        @Path("organizationId") organizationId: String,
        @Path("documentId") documentId: String,
        @Body body: UpdateDocumentRequest,
    ): DocumentEnvelope

    @DELETE("api/organizations/{organizationId}/documents/{documentId}")
    suspend fun trashDocument(
        @Path("organizationId") organizationId: String,
        @Path("documentId") documentId: String,
    ): Response<Unit>

    @GET("api/organizations/{organizationId}/documents/statistics")
    suspend fun getDocumentStatistics(
        @Path("organizationId") organizationId: String,
    ): DocumentStatisticsResponse

    // --- Tags ---
    @GET("api/organizations/{organizationId}/tags")
    suspend fun listTags(
        @Path("organizationId") organizationId: String,
    ): TagsResponse
}
