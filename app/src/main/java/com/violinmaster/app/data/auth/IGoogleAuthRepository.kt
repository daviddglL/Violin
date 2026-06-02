package com.violinmaster.app.data.auth

import android.content.Intent
import kotlinx.coroutines.flow.StateFlow

/**
 * Repository interface for Firebase Auth + Google Sign-In lifecycle.
 *
 * REQ-ARCH-008: Repository interfaces enable dependency inversion,
 * allowing consumers to depend on abstractions instead of concrete classes.
 */
interface IGoogleAuthRepository {

    /** Whether a Firebase user is currently signed in (emitted as StateFlow). */
    val signedInFlow: StateFlow<Boolean>

    /**
     * Exchanges a Google Sign-In ID token for Firebase Auth credentials
     * and signs the user in to Firebase.
     *
     * @param idToken The ID token from a successful Google Sign-In.
     * @return [Result] containing [GoogleUser] on success or an exception on failure.
     */
    suspend fun signIn(idToken: String): Result<GoogleUser>

    /** Signs out from Firebase and Google. */
    suspend fun signOut()

    /**
     * Returns the current Firebase user's ID token for Gemini API authentication.
     * Returns `null` if no user is signed in.
     */
    fun getAccessToken(): String?

    /** Whether a Firebase user is currently signed in. */
    fun isSignedIn(): Boolean

    /**
     * Returns the [Intent] to launch the Google Sign-In UI.
     * Call this from the Activity/Composable to start the sign-in flow.
     */
    fun getSignInIntent(): Intent
}
