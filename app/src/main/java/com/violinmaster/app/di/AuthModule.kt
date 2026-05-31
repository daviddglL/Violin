@file:Suppress("DEPRECATION") // GoogleSignIn deprecated in play-services-auth 21+

package com.violinmaster.app.di

import android.content.Context
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module providing Firebase Auth and Google Sign-In dependencies.
 *
 * All singletons scoped to [SingletonComponent] (application lifetime).
 *
 * TODO(auth): Migrate from deprecated GoogleSignIn to Credential Manager API
 * (androidx.credentials). GoogleSignIn is deprecated in play-services-auth 21+.
 */
@Module
@InstallIn(SingletonComponent::class)
object AuthModule {

    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance()

    @Provides
    @Singleton
    fun provideGoogleSignInClient(@ApplicationContext context: Context): GoogleSignInClient {
        // NOTE: For Firebase Auth with Google Sign-In to work, you MUST:
        // 1. Enable Google Sign-In in the Firebase Console (Authentication → Sign-in method)
        // 2. Add your SHA-1 fingerprint in Firebase Console → Project Settings
        // 3. Re-download google-services.json (it must include oauth_client entries)
        //
        // The OAuth 2.0 Web Client ID is auto-generated from google-services.json
        // as R.string.default_web_client_id. If this resource is missing,
        // google-services.json was downloaded before enabling Google Sign-In.
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(context.getString(com.violinmaster.app.R.string.default_web_client_id))
            .requestEmail()
            .build()
        return GoogleSignIn.getClient(context, gso)
    }
}
