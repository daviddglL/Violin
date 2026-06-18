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
import kotlinx.coroutines.tasks.await

/**
 * Links a Google credential to an existing PIN-authenticated [UserAccount]
 * via its firebaseUid field.
 *
 * Uniqueness guard: rejects the link if another account already has this
 * firebaseUid, preventing account takeover.
 *
 * REQ-GS-004, GS-005, GS-006: Google account linking with duplicate rejection.
 *
 * @param firebaseAuth Firebase Auth instance for signInWithCredential.
 * @param repository Repository for user account lookup and firebaseUid updates.
 */
class LinkGoogleAccountUseCase(
    private val firebaseAuth: FirebaseAuth,
    private val repository: IPracticeRepository
) {
    /**
     * Initiates Google Sign-In via Credential Manager and links the resulting
     * Firebase Auth UID to the provided [currentUser].
     *
     * @param context Activity context for Credential Manager.
     * @param currentUser The currently authenticated PIN user (firebaseUid must be null).
     * @param credentialManager Credential Manager instance.
     * @param googleSignInClient Legacy GoogleSignInClient (fallback).
     * @return [Result] containing the updated [UserAccount] on success,
     *         or failure with "account_already_linked" message on duplicate.
     */
    suspend operator fun invoke(
        context: Context,
        currentUser: UserAccount,
        credentialManager: CredentialManager,
        googleSignInClient: GoogleSignInClient
    ): Result<UserAccount> {
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
                return Result.failure(Exception("Invalid credential type"))
            }

            val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
            val idToken = googleIdTokenCredential.idToken

            val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)
            val authResult = firebaseAuth.signInWithCredential(firebaseCredential).await()
            val firebaseUser = authResult.user
                ?: return Result.failure(Exception("Firebase sign-in returned null user"))

            val firebaseUid = firebaseUser.uid

            // Uniqueness guard: reject if another account already has this firebaseUid
            val existingWithUid = repository.getUserByFirebaseUid(firebaseUid)
            if (existingWithUid != null && existingWithUid.username != currentUser.username) {
                return Result.failure(Exception("account_already_linked"))
            }

            // Link: update firebaseUid on the current user
            repository.updateFirebaseUid(currentUser.username, firebaseUid)
            val updated = currentUser.copy(firebaseUid = firebaseUid)
            Result.success(updated)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Checks whether a firebaseUid is already linked to another account.
     * Exposed for testing the uniqueness guard independently.
     */
    suspend fun checkUniqueness(firebaseUid: String): Boolean {
        val existing = repository.getUserByFirebaseUid(firebaseUid)
        return existing == null
    }
}
