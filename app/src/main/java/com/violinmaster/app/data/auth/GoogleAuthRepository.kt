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
 * by the UI layer; this repository exchanges ID tokens for Firebase credentials
 * and reconciles the user's local [com.violinmaster.app.data.UserAccount] with
 * Firestore via [AuthReconciler].
 *
 * Each user signs in with their own Google account → Gemini API calls use
 * that user's OAuth token, consuming their own Gemini quota.
 */
@Singleton
open class GoogleAuthRepository @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val credentialManager: CredentialManager,
    private val authReconciler: AuthReconciler
) : IGoogleAuthRepository {
    // Lazily initialized to avoid Firebase instantiation in unit tests
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }

    private val _signedInFlow = MutableStateFlow(false)
    override open val signedInFlow: StateFlow<Boolean> = _signedInFlow.asStateFlow()

    /**
     * Exchanges a Google Sign-In ID token (obtained via CredentialManager in the UI)
     * for Firebase Auth credentials, signs the user in to Firebase, and reconciles
     * the local [com.violinmaster.app.data.UserAccount] with Firestore.
     *
     * REQ-AUTH-001: Google Sign-In links Firebase UID.
     * REQ-AUTH-002: Migration of existing PIN-only users (when usernameToLink is provided).
     * REQ-AUTH-003: New user Google flow auto-creates account.
     *
     * @param idToken The ID token from a successful Google Sign-In.
     * @param usernameToLink Optional existing Room username to link to this Google account.
     * @return [Result] containing [GoogleSignInResult] on success or an exception on failure.
     */
    override open suspend fun signIn(
        idToken: String,
        usernameToLink: String?
    ): Result<GoogleSignInResult> {
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

            // Reconcile UserAccount with Firestore
            val userAccount = if (usernameToLink != null) {
                // REQ-AUTH-002: Existing PIN user linking Google for the first time
                authReconciler.linkExistingUserToFirebaseUid(
                    username = usernameToLink,
                    firebaseUid = firebaseUser.uid,
                    googleDisplayName = googleUser.displayName
                )
            } else {
                // REQ-AUTH-001 / REQ-AUTH-003: New or returning Google user
                authReconciler.reconcileAfterGoogleSignIn(
                    firebaseUid = firebaseUser.uid,
                    googleEmail = googleUser.email,
                    googleDisplayName = googleUser.displayName
                )
            }

            Result.success(GoogleSignInResult(googleUser, userAccount))
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
     * Returns `null` if no user is signed in or if token acquisition fails.
     *
     * Uses a [java.util.concurrent.CountDownLatch] to safely await the async
     * [com.google.firebase.auth.FirebaseUser.getIdToken] task with a 5-second
     * timeout. This is called from [GeminiAuthInterceptor] on OkHttp's thread pool,
     * where brief synchronous waiting is acceptable.
     *
     * Callers should handle null gracefully — [GeminiRepository] treats a missing
     * token as an auth failure (HTTP 401).
     */
    override open fun getAccessToken(): String? {
        val currentUser = auth.currentUser ?: return null
        return try {
            val latch = java.util.concurrent.CountDownLatch(1)
            var token: String? = null
            currentUser.getIdToken(false).addOnCompleteListener { task ->
                token = if (task.isSuccessful) task.result?.token else null
                latch.countDown()
            }
            // Block up to 5 s for token acquisition; proceed without token on timeout
            latch.await(5, java.util.concurrent.TimeUnit.SECONDS)
            token
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
