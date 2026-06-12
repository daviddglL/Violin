package com.violinmaster.app.data.firebase

/**
 * Interface for Firestore user collection operations.
 *
 * Enables substituting a fake implementation in unit tests without
 * needing the Firebase emulator (FirebaseFirestore is final).
 */
interface IFirestoreUsers {
    suspend fun getUserDoc(uid: String): UserDoc?
    suspend fun setUserDoc(uid: String, doc: UserDoc)
}
