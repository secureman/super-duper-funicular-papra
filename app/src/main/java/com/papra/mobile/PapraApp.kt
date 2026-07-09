package com.papra.mobile

import android.app.Application
import com.papra.mobile.data.local.SessionStore
import com.papra.mobile.data.repository.AuthRepository
import com.papra.mobile.data.repository.DocumentRepository

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
    }
}
