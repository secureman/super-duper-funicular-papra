package com.papra.mobile.data.remote

import com.papra.mobile.data.local.SessionStore
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Papra is self-hosted, so the API base URL is whatever server the user
 * points the app at, not a fixed constant. This factory (re)builds the
 * Retrofit client whenever the server URL is set/changed.
 */
object ApiClientFactory {

    private val json = Json { ignoreUnknownKeys = true }

    fun create(baseUrl: String, sessionStore: SessionStore): PapraApiService {
        val normalizedUrl = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"

        val logging = HttpLoggingInterceptor().apply {
            // BODY level shows the raw JSON responses (and confirms whether the
            // session cookie is actually being sent/accepted) -- essential for
            // diagnosing auth/shape mismatches against a custom fork. Only ever
            // active in debug builds since it does log the session cookie itself.
            level = if (com.papra.mobile.BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(sessionStore))
            .addInterceptor(logging)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()

        return Retrofit.Builder()
            .baseUrl(normalizedUrl)
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(PapraApiService::class.java)
    }
}
