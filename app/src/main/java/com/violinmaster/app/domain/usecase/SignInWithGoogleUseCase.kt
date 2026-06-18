package com.violinmaster.app.domain.usecase

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.violinmaster.app.data.IPracticeRepository
import com.violinmaster.app.data.UserAccount
import com.violinmaster.app.di.AuthManager
import kotlinx.coroutines.tasks.await

/**
 * Sign-in result from Google Sign-In flow.
 *
 * @param user The authenticated [UserAccount] on success, null on failure.
 * @param error Human-readable error message on failure, null on success.
 */
data class SignInResult(val user: UserAccount?, val error: String?)

/**
 * Executes full Google Sign-In via Credential Manager → FirebaseAuth → account lookup.
 *
 * REQ-GS-001, GS-002, GS-003: Google Sign-In button → Credential Manager → FirebaseAuth.
 *
 * @param firebaseAuth Firebase Auth instance for signInWithCredential.
 * @param repository Repository for user account lookup and creation.
 * @param authManager Session manager for persisting authenticated user.
 */
class SignInWithGoogleUseCase(
    private val firebaseAuth: FirebaseAuth,
    private val repository: IPracticeRepository,
    private val authManager: AuthManager
) {
    /**
     * Initiates Google Sign-In via Credential Manager and exchanges the
     * resulting ID token for Firebase credentials.
     *
     * @param context Activity context for Credential Manager.
     * @param credentialManager Credential Manager instance.
     * @param googleSignInClient Legacy GoogleSignInClient (fallback).
     * @return [SignInResult] with user on success, error on failure.
     */
    suspend operator fun invoke(
        context: Context,
        credentialManager: CredentialManager,
        googleSignInClient: GoogleSignInClient
    ): SignInResult {
        return try {
            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId("")
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val result = credentialManager.getCredential(context, request)
            val credential = result.credential

            if (credential.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                return SignInResult(null, "Invalid credential type")
            }

            val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
            val idToken = googleIdTokenCredential.idToken

            val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)
            val authResult = firebaseAuth.signInWithCredential(firebaseCredential).await()
            val firebaseUser = authResult.user
                ?: return SignInResult(null, "Firebase sign-in returned null user")

            val existingUser = repository.getUserByFirebaseUid(firebaseUser.uid)
            if (existingUser != null) {
                authManager.saveCurrentUser(existingUser)
                return SignInResult(existingUser, null)
            }

            val newUser = UserAccount(
                username = firebaseUser.email ?: firebaseUser.uid.take(20),
                role = "FREELANCER",
                hashedPassword = "",
                salt = "",
                firebaseUid = firebaseUser.uid
            )
            repository.insertUser(newUser)
            authManager.saveCurrentUser(newUser)
            SignInResult(newUser, null)
        } catch (e: Exception) {
            SignInResult(null, e.message ?: "Google Sign-In failed")
        }
    }
}
