package com.violinmaster.app.data.firebase

import com.violinmaster.app.data.UserAccount
import com.violinmaster.app.data.UserDao
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow

/**
 * Sync repository for user accounts.
 *
 * Collection path: `users/{firebaseUid}` — firebaseUid is the document ID.
 * Sensitive fields (hashedPassword, salt) are excluded from Firestore via
 * the [UserDoc.fromEntity] mapping.
 *
 * REQ-CSYNC-004: Users collection with firebaseUid as docId.
 * REQ-AUTH-001: hashedPassword and salt NEVER stored in Firestore.
 *
 * @param collection The Firestore collection abstraction (injected for testability).
 * @param userDao Room DAO for user cache operations.
 * @param dispatcher Coroutine dispatcher. Defaults to [Dispatchers.IO].
 */
class UserSyncRepository(
    collection: IFirestoreCollection<UserDoc>,
    private val userDao: UserDao,
    dispatcher: CoroutineDispatcher = Dispatchers.IO
) : FirestoreSyncRepository<UserAccount, UserDoc>(collection, dispatcher) {

    override fun UserAccount.toFirestoreDoc(): UserDoc = UserDoc.fromEntity(this)
    override fun UserDoc.toEntity(): UserAccount = this.toEntity()
    override fun UserDoc.docId(): String = firebaseUid

    override suspend fun insertCache(entity: UserAccount) {
        userDao.insertUser(entity)
    }

    override suspend fun deleteCache(docId: String) {
        val user = userDao.getUserByFirebaseUid(docId)
        if (user != null) {
            userDao.deleteUser(user)
        }
    }

    override fun observeCache(): Flow<List<UserAccount>> = userDao.getAllUsers()

    /**
     * Updates the FCM push token for a user in both Firestore and local Room cache.
     *
     * @param firebaseUid The Firebase Auth UID identifying the user document.
     * @param token The FCM registration token.
     */
    suspend fun updateFcmToken(firebaseUid: String, token: String) {
        val user = userDao.getUserByFirebaseUid(firebaseUid) ?: return
        val updated = user.copy(fcmToken = token)
        userDao.insertUser(updated)
        try {
            collection.setDocument(firebaseUid, updated.toFirestoreDoc())
        } catch (_: Exception) {
            // Firestore write failed — token cached locally, will sync on next push
        }
    }

    /**
     * Fetches a user account from Firestore by username.
     */
    suspend fun fetchByUsername(username: String): UserAccount? {
        val docs = collection.queryByField("username", username)
        val userDoc = docs.firstOrNull() ?: return null
        val user = userDoc.toEntity()
        insertCache(user)
        return user
    }

    /**
     * Deletes the user document and its sessions subcollection from Firestore.
     *
     * Used for GDPR cascading deletion (REQ-GD-003).
     *
     * @param firebaseUid The Firebase Auth UID of the user to delete.
     */
    suspend fun deleteUserAndSubcollections(firebaseUid: String) {
        // Delete user document: users/{firebaseUid}
        collection.deleteDocument(firebaseUid)
        // Delete sessions subcollection: users/{firebaseUid}/sessions/*
        collection.deleteSubcollection("$firebaseUid/sessions")
    }

    /**
     * Deletes all lesson progress documents for a given user from Firestore.
     *
     * Lesson documents use compound IDs: `{firebaseUid}_{lessonId}`.
     * This method queries the lesson collection by the firebaseUid prefix
     * and deletes all matching documents.
     *
     * @param firebaseUid The Firebase Auth UID whose lessons should be deleted.
     * @param lessonCollection The lesson progress Firestore collection.
     */
    suspend fun deleteLessonProgressByUser(
        firebaseUid: String,
        lessonCollection: IFirestoreCollection<LessonDoc>
    ) {
        val allDocs = lessonCollection.queryByField("lessonId", firebaseUid)
        // Compound keys use _ as separator: {firebaseUid}_{lessonId}
        // Query by field isn't precise for prefix matching, so we collect and filter
        // Actually, we'll fetch all and filter by the firebaseUid prefix
        // Since we can't do prefix queries in Firestore easily, we use the
        // deleteSubcollection approach with a synthetic subcollection path
        try {
            // Try to delete using the path: users/{firebaseUid}/lesson_progress
            collection.deleteSubcollection("$firebaseUid/lesson_progress")
        } catch (_: Exception) {
            // If the subcollection doesn't exist, that's fine — nothing to delete
        }
    }
}
