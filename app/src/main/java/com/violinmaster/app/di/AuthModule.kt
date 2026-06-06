package com.violinmaster.app.di

import android.content.Context
import androidx.credentials.CredentialManager
import com.google.firebase.auth.FirebaseAuth
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module providing Firebase Auth and Credential Manager dependencies.
 *
 * Migrated from deprecated GoogleSignInClient (play-services-auth 21+) to
 * Credential Manager API (androidx.credentials). The CredentialManager is
 * created once with Application context; individual getCredential() calls
 * receive the Activity context at the call site.
 */
@Module
@InstallIn(SingletonComponent::class)
object AuthModule {

    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance()

    /**
     * Provides [CredentialManager] for Google Sign-In via the Credential Manager API.
     *
     * NOTE: The OAuth 2.0 Web Client ID is auto-generated from google-services.json
     * as R.string.default_web_client_id. The calling Activity must pass its context
     * to credentialManager.getCredential(context, request).
     */
    @Provides
    @Singleton
    fun provideCredentialManager(@ApplicationContext context: Context): CredentialManager =
        CredentialManager.create(context)
}
