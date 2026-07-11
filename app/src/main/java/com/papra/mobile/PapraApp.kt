package com.papra.mobile

import android.app.Application
import coil.Coil
import coil.ImageLoader
import com.papra.mobile.data.local.SessionStore
import com.papra.mobile.data.remote.AuthInterceptor
import com.papra.mobile.data.repository.AuthRepository
import com.papra.mobile.data.repository.DocumentRepository
import okhttp3.OkHttpClient

/**
 * Simple manual DI container (no Hilt) to keep the scaffold dependency-light.
 * Swap for Hilt/Koin later if the app grows.
 */
class PapraApp : Application() {

    lateinit var sessionStore: SessionStore
        private set
    lateinit var authRepository: AuthRepository
        private set
    lateinit var documentRepository: DocumentRepository
        private set

    override fun onCreate() {
        super.onCreate()
        sessionStore = SessionStore(this)
        authRepository = AuthRepository(sessionStore)
        documentRepository = DocumentRepository(sessionStore)

        // Thumbnails load from the same auth-gated /documents/{id}/file endpoint as
        // everything else, so Coil needs the same AuthInterceptor Retrofit uses.
        val imageLoader = ImageLoader.Builder(this)
            .okHttpClient {
                OkHttpClient.Builder()
                    .addInterceptor(AuthInterceptor(sessionStore))
                    .build()
            }
            .build()
        Coil.setImageLoader(imageLoader)
    }
}
