package com.papra.mobile.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "papra_session")

enum class AuthMode { API_KEY, SESSION, NONE }

/**
 * Persists whichever auth method the user picked (API key or session
 * cookie from email/password sign-in), plus the self-hosted server URL
 * and the currently selected organization.
 */
class SessionStore(private val context: Context) {

    private object Keys {
        val SERVER_URL = stringPreferencesKey("server_url")
        val AUTH_MODE = stringPreferencesKey("auth_mode")
        val API_KEY = stringPreferencesKey("api_key")
        val SESSION_COOKIE = stringPreferencesKey("session_cookie")
        val ACTIVE_ORG_ID = stringPreferencesKey("active_org_id")
    }

    val serverUrl: Flow<String?> = context.dataStore.data.map { it[Keys.SERVER_URL] }
    val authMode: Flow<AuthMode> = context.dataStore.data.map {
        when (it[Keys.AUTH_MODE]) {
            "API_KEY" -> AuthMode.API_KEY
            "SESSION" -> AuthMode.SESSION
            else -> AuthMode.NONE
        }
    }
    val apiKey: Flow<String?> = context.dataStore.data.map { it[Keys.API_KEY] }
    val sessionCookie: Flow<String?> = context.dataStore.data.map { it[Keys.SESSION_COOKIE] }
    val activeOrganizationId: Flow<String?> = context.dataStore.data.map { it[Keys.ACTIVE_ORG_ID] }

    suspend fun currentServerUrl(): String? = serverUrl.first()
    suspend fun currentAuthMode(): AuthMode = authMode.first()
    suspend fun currentApiKey(): String? = apiKey.first()
    suspend fun currentSessionCookie(): String? = sessionCookie.first()

    suspend fun saveServerUrl(url: String) {
        context.dataStore.edit { it[Keys.SERVER_URL] = url.trimEnd('/') }
    }

    suspend fun saveApiKeySession(apiKey: String) {
        context.dataStore.edit {
            it[Keys.AUTH_MODE] = "API_KEY"
            it[Keys.API_KEY] = apiKey
        }
    }

    suspend fun saveEmailSession(cookie: String) {
        context.dataStore.edit {
            it[Keys.AUTH_MODE] = "SESSION"
            it[Keys.SESSION_COOKIE] = cookie
        }
    }

    suspend fun setActiveOrganization(orgId: String) {
        context.dataStore.edit { it[Keys.ACTIVE_ORG_ID] = orgId }
    }

    suspend fun clear() {
        context.dataStore.edit { it.clear() }
    }
}
