package com.papra.mobile.data.remote

import com.papra.mobile.data.local.AuthMode
import com.papra.mobile.data.local.SessionStore
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Adds the right auth header depending on which method the user chose at
 * login: a Papra API key (Authorization: Bearer ...) or a Better Auth
 * session cookie from email/password sign-in.
 */
class AuthInterceptor(private val sessionStore: SessionStore) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val builder = request.newBuilder()

        runBlocking {
            when (sessionStore.currentAuthMode()) {
                AuthMode.API_KEY -> {
                    sessionStore.currentApiKey()?.let { key ->
                        builder.addHeader("Authorization", "Bearer $key")
                    }
                }
                AuthMode.SESSION -> {
                    sessionStore.currentSessionCookie()?.let { cookie ->
                        builder.addHeader("Cookie", cookie)
                    }
                }
                AuthMode.NONE -> Unit
            }
        }

        return chain.proceed(builder.build())
    }
}
