package com.violinmaster.app.data.auth

import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository managing Firebase Auth + Google Sign-In lifecycle via Credential Manager.
 *
 * Migrated from deprecated GoogleSignInClient (play-services-auth 21+) to
 * Credential Manager API (androidx.credentials). Token acquisition is handled
 * by the UI layer; this repository exchanges ID tokens for Firebase credentials.
 *
 * Each user signs in with their own Google account → Gemini API calls use
 * that user's OAuth token, consuming their own Gemini quota.
 */
@Singleton
open class GoogleAuthRepository @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val credentialManager: CredentialManager
) : IGoogleAuthRepository {
    // Lazily initialized to avoid Firebase instantiation in unit tests
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }

    private val _signedInFlow = MutableStateFlow(false)
    override open val signedInFlow: StateFlow<Boolean> = _signedInFlow.asStateFlow()

    /**
     * Exchanges a Google Sign-In ID token (obtained via CredentialManager in the UI)
     * for Firebase Auth credentials and signs the user in to Firebase.
     *
     * @param idToken The ID token from a successful Google Sign-In.
     * @return [Result] containing [GoogleUser] on success or an exception on failure.
     */
    override open suspend fun signIn(idToken: String): Result<GoogleUser> {
        return try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val authResult = auth.signInWithCredential(credential).await()
            val firebaseUser = authResult.user
                ?: return Result.failure(Exception("Firebase sign-in succeeded but user is null"))

            _signedInFlow.value = true

            val googleUser = GoogleUser(
                userId = firebaseUser.uid,
                email = firebaseUser.email ?: "",
                displayName = firebaseUser.displayName ?: "Google User",
                photoUrl = firebaseUser.photoUrl?.toString()
            )
            Result.success(googleUser)
        } catch (e: Exception) {
            _signedInFlow.value = false
            Result.failure(e)
        }
    }

    /**
     * Signs out from Firebase and clears Credential Manager state.
     */
    override open suspend fun signOut() {
        try {
            credentialManager.clearCredentialState(ClearCredentialStateRequest())
        } catch (_: Exception) {
            // Ignore Credential Manager clear errors — proceed with Firebase sign-out
        }
        auth.signOut()
        _signedInFlow.value = false
    }

    /**
     * Returns the current Firebase user's ID token for Gemini API authentication.
     * Returns `null` if no user is signed in.
     *
     * Callers (e.g., [GeminiAuthInterceptor]) should handle null gracefully.
     */
    override open fun getAccessToken(): String? {
        val currentUser = auth.currentUser ?: return null
        // getIdToken(false) returns cached token if still valid, refreshes if needed
        return try {
            val task = currentUser.getIdToken(false)
            if (task.isSuccessful) {
                task.result?.token
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Whether a Firebase user is currently signed in.
     */
    override open fun isSignedIn(): Boolean = auth.currentUser != null
}

/**
 * Data class representing a signed-in Google user.
 */
data class GoogleUser(
    val userId: String,
    val email: String,
    val displayName: String,
    val photoUrl: String?
)
