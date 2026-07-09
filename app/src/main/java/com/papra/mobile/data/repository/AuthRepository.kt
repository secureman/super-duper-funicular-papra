package com.papra.mobile.data.repository

import com.papra.mobile.data.local.SessionStore
import com.papra.mobile.data.remote.ApiClientFactory
import com.papra.mobile.data.remote.dto.EmailSignInRequest

sealed class AuthResult {
    data object Success : AuthResult()
    data class Error(val message: String) : AuthResult()
}

class AuthRepository(private val sessionStore: SessionStore) {

    /** Step 1 for both auth modes: point the app at the user's Papra server. */
    suspend fun setServerUrl(url: String) {
        sessionStore.saveServerUrl(url)
    }

    /** Auth mode A: paste a Papra API key (Settings > API Keys on the server). */
    suspend fun signInWithApiKey(apiKey: String): AuthResult {
        val serverUrl = sessionStore.currentServerUrl()
            ?: return AuthResult.Error("Set a server URL first")
        sessionStore.saveApiKeySession(apiKey)
        return try {
            // Validate the key by hitting a cheap authenticated endpoint.
            ApiClientFactory.create(serverUrl, sessionStore).listOrganizations()
            AuthResult.Success
        } catch (e: Exception) {
            AuthResult.Error(e.message ?: "Could not verify API key")
        }
    }

    /** Auth mode B: email/password against Better Auth, session cookie stored. */
    suspend fun signInWithEmail(email: String, password: String): AuthResult {
        val serverUrl = sessionStore.currentServerUrl()
            ?: return AuthResult.Error("Set a server URL first")
        return try {
            val api = ApiClientFactory.create(serverUrl, sessionStore)
            val response = api.signInWithEmail(EmailSignInRequest(email, password))
            if (!response.isSuccessful) {
                return AuthResult.Error("Sign-in failed (${response.code()})")
            }
            // Set-Cookie headers look like "name=value; Path=/; HttpOnly; SameSite=Lax".
            // The request Cookie header must only contain "name=value" pairs -- replaying
            // the full Set-Cookie string back gets silently rejected by the server, which
            // breaks every authenticated call after login. Strip to just the pairs.
            val setCookieHeaders = response.headers().values("set-cookie")
            if (setCookieHeaders.isEmpty()) {
                return AuthResult.Error("No session returned by server")
            }
            val cookie = setCookieHeaders.joinToString("; ") { it.substringBefore(";").trim() }
            sessionStore.saveEmailSession(cookie)
            AuthResult.Success
        } catch (e: Exception) {
            AuthResult.Error(e.message ?: "Could not sign in")
        }
    }

    suspend fun signOut() {
        sessionStore.clear()
    }
}
