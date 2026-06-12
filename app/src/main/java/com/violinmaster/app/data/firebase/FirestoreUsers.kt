package com.violinmaster.app.data.firebase

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * FirebaseFirestore-backed implementation of [IFirestoreUsers].
 *
 * Wraps Firestore calls for the `users` collection so unit tests
 * can substitute [IFirestoreUsers] with an in-memory fake.
 */
@Singleton
class FirestoreUsers @Inject constructor(
    private val firestore: FirebaseFirestore
) : IFirestoreUsers {

    companion object {
        private const val USERS_COLLECTION = "users"
    }

    override suspend fun getUserDoc(uid: String): UserDoc? {
        return try {
            firestore.collection(USERS_COLLECTION)
                .document(uid)
                .get()
                .await()
                .toObject(UserDoc::class.java)
        } catch (e: Exception) {
            null // Firestore unavailable — caller handles gracefully
        }
    }

    override suspend fun setUserDoc(uid: String, doc: UserDoc) {
        firestore.collection(USERS_COLLECTION)
            .document(uid)
            .set(doc)
            .await()
    }
}
