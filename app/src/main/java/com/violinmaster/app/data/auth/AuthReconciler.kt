package com.violinmaster.app.data.auth

import com.violinmaster.app.data.UserAccount
import com.violinmaster.app.data.UserDao
import com.violinmaster.app.data.firebase.IFirestoreUsers
import com.violinmaster.app.data.firebase.UserDoc
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Handles UserAccount reconciliation after Firebase Auth (Google Sign-In).
 *
 * Core logic:
 * 1. Query Firestore `users/{uid}` for existing cloud record
 * 2. Query Room `user_accounts` for local record by firebaseUid
 * 3. Merge/create: Firestore-first, backfill local, handle edge cases
 *
 * REQ-AUTH-001: Google Sign-In links Firebase UID.
 * REQ-AUTH-002: Migration of existing PIN-only users.
 * REQ-AUTH-003: New user Google flow auto-creates account.
 * REQ-AUTH-004: Edge cases (reinstall, username conflict, cross-state).
 */
@Singleton
open class AuthReconciler @Inject constructor(
    private val userDao: UserDao,
    private val firestoreUsers: IFirestoreUsers
) {
    /**
     * Main reconciliation entry point after Google Sign-In.
     *
     * @param firebaseUid The Firebase Auth UID from the signed-in user.
     * @param googleEmail The Google account email (used for username derivation).
     * @param googleDisplayName The Google account display name.
     * @return The reconciled [UserAccount] persisted to both Firestore and Room.
     */
    open suspend fun reconcileAfterGoogleSignIn(
        firebaseUid: String,
        googleEmail: String,
        googleDisplayName: String
    ): UserAccount? {
        // Step 1: Check Firestore for existing user document
        val firestoreDoc = firestoreUsers.getUserDoc(firebaseUid)

        // Step 2: Check Room for user with this firebaseUid
        val localByUid = userDao.getUserByFirebaseUid(firebaseUid)

        return when {
            // CASE A: Firestore has doc → source of truth
            firestoreDoc != null -> {
                val account = firestoreDoc.toEntity()
                if (localByUid != null) {
                    userDao.updateUser(account)
                } else {
                    userDao.insertUser(account)
                }
                account
            }

            // CASE B: Room has user with this UID, Firestore doesn't → upload
            localByUid != null -> {
                uploadToFirestore(localByUid)
                localByUid
            }

            // CASE C: Neither has it → create new UserAccount
            else -> {
                val username = deriveUsername(googleEmail)
                val account = UserAccount(
                    username = username,
                    role = "FREELANCER",
                    hashedPassword = "",
                    salt = "",
                    points = 0,
                    skillLevel = "Beginner",
                    firebaseUid = firebaseUid
                )
                userDao.insertUser(account)
                uploadToFirestore(account)
                account
            }
        }
    }

    /**
     * Links an existing Room user (typically PIN-only with firebaseUid=null)
     * to a Firebase Auth UID. Backfills firebaseUid locally and uploads to Firestore.
     *
     * REQ-AUTH-002: First Google link for existing PIN user.
     *
     * @param username The existing Room username to link.
     * @param firebaseUid The Firebase Auth UID to associate.
     * @param googleDisplayName Ignored — username is preserved from Room.
     * @return The updated [UserAccount] or null if user not found.
     */
    open suspend fun linkExistingUserToFirebaseUid(
        username: String,
        firebaseUid: String,
        googleDisplayName: String
    ): UserAccount? {
        val user = userDao.getUserByUsername(username) ?: return null

        // Update local firebaseUid
        userDao.updateFirebaseUid(username, firebaseUid)

        // Upload to Firestore
        val updated = user.copy(firebaseUid = firebaseUid)
        uploadToFirestore(updated)

        return updated
    }

    /**
     * Derives a unique username from a Google email address.
     * If the base username is taken, appends a numeric discriminator.
     *
     * REQ-AUTH-004: Username conflict with discriminator.
     */
    private suspend fun deriveUsername(email: String): String {
        val base = email.substringBefore("@").take(20)
        val existing = userDao.getUserByUsername(base)
        if (existing == null) return base

        // Append discriminator
        var discriminator = 1
        while (true) {
            val candidate = "${base}_$discriminator"
            if (userDao.getUserByUsername(candidate) == null) return candidate
            discriminator++
        }
    }

    /**
     * Uploads a [UserAccount] to Firestore.
     * Firestore write failures are silently caught — Room is the local cache,
     * and the next reconcile will re-upload if needed.
     */
    private suspend fun uploadToFirestore(account: UserAccount) {
        val uid = account.firebaseUid ?: return
        try {
            val doc = UserDoc.fromEntity(account)
            firestoreUsers.setUserDoc(uid, doc)
        } catch (_: Exception) {
            // Firestore write failed — Room has the data, will retry on next reconcile
        }
    }
}
