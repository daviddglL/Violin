package com.violinmaster.app.data.auth

import kotlinx.coroutines.flow.StateFlow

/**
 * Repository interface for Firebase Auth + Google Sign-In lifecycle.
 *
 * Google Sign-In token acquisition is handled by the UI layer via
 * [androidx.credentials.CredentialManager]. This repository exchanges
 * the obtained ID token for Firebase credentials.
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
     * @param idToken The ID token from a successful Google Sign-In (obtained via CredentialManager).
     * @return [Result] containing [GoogleUser] on success or an exception on failure.
     */
    suspend fun signIn(idToken: String): Result<GoogleUser>

    /** Signs out from Firebase and clears Credential Manager state. */
    suspend fun signOut()

    /**
     * Returns the current Firebase user's ID token for Gemini API authentication.
     * Returns `null` if no user is signed in.
     */
    fun getAccessToken(): String?

    /** Whether a Firebase user is currently signed in. */
    fun isSignedIn(): Boolean
}
