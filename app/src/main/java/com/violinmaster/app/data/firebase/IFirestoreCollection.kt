package com.violinmaster.app.data.firebase

/**
 * Interface wrapping Firestore collection operations for test substitution.
 *
 * FirebaseFirestore is final and cannot run in Robolectric unit tests.
 * This interface enables replacing Firestore with an in-memory fake
 * so that [FirestoreSyncRepository] and concrete sync repositories
 * can be tested without the Firebase emulator.
 *
 * REQ-CSYNC-001: Firestore collection abstraction for test substitution.
 *
 * @param D The Firestore document type (e.g., [UserDoc], [SessionDoc]).
 */
interface IFirestoreCollection<D> {

    /**
     * Sets (creates or overwrites) a document in the collection.
     *
     * @param id The document ID within the collection.
     * @param document The document data to store.
     */
    suspend fun setDocument(id: String, document: D)

    /**
     * Deletes a document from the collection.
     *
     * @param id The document ID to remove.
     */
    suspend fun deleteDocument(id: String)

    /**
     * Registers a real-time snapshot listener on the collection.
     *
     * The listener fires immediately with the current state and
     * subsequently whenever any document in the collection changes.
     *
     * @param onSnapshot Callback receiving the current list of documents.
     * @param onError Callback receiving any listener error (e.g., permission denied).
     * @return A cancel function. Call it to stop receiving updates.
     */
    fun addSnapshotListener(
        onSnapshot: (List<D>) -> Unit,
        onError: (Exception) -> Unit
    ): () -> Unit
}
