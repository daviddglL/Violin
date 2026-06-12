package com.violinmaster.app.data.firebase

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * Concrete [IFirestoreCollection] implementation wrapping [FirebaseFirestore]
 * for production use.
 *
 * Uses a [pathProvider] lambda to enable dynamic collection paths
 * (e.g., subcollections under user-specific paths). The path is
 * resolved lazily on each operation to support user-scoped paths
 * that depend on the current authentication state.
 *
 * REQ-CSYNC-001: Enables FirestoreSyncRepository to operate against
 * real Firestore in production while preserving interface-based
 * test substitution.
 *
 * @param T The Firestore document type (e.g., [SessionDoc], [UserDoc]).
 * @param firestore The FirebaseFirestore instance.
 * @param pathProvider Lambda that returns the Firestore collection path.
 *                     Called on each operation for dynamic path resolution.
 * @param docClass The Java class of the document type for deserialization
 *                 in snapshot listeners.
 */
class FirestoreCollection<T>(
    private val firestore: FirebaseFirestore,
    private val pathProvider: () -> String,
    private val docClass: Class<T>
) : IFirestoreCollection<T> {

    // ── Public accessors (for testing/diagnostics) ─────────────────────────

    /**
     * Resolves the current collection path from the path provider.
     * Used in tests to verify path resolution logic.
     */
    fun collectionPath(): String = pathProvider()

    /**
     * Returns the document class used for Firestore deserialization.
     * Used in tests to verify the correct type is configured.
     */
    fun getDocClass(): Class<T> = docClass

    // ── IFirestoreCollection Implementation ────────────────────────────────

    /**
     * Sets (creates or overwrites) a document in the Firestore collection.
     *
     * Delegates to [FirebaseFirestore.collection] → [document] → [set].
     * The Firestore SDK serializes the document object automatically.
     *
     * @param id The document ID within the collection.
     * @param document The document data to store.
     */
    override suspend fun setDocument(id: String, document: T) {
        firestore.collection(pathProvider())
            .document(id)
            .set(document as Any)
            .await()
    }

    /**
     * Deletes a document from the Firestore collection.
     *
     * @param id The document ID to remove.
     */
    override suspend fun deleteDocument(id: String) {
        firestore.collection(pathProvider())
            .document(id)
            .delete()
            .await()
    }

    /**
     * Registers a real-time snapshot listener on the Firestore collection.
     *
     * The listener fires immediately with the current state and subsequently
     * whenever any document in the collection changes. Documents are
     * deserialized using [docClass].
     *
     * @param onSnapshot Callback receiving the current list of documents.
     * @param onError Callback receiving any listener error (e.g., permission denied).
     * @return A cancel function. Call it to stop receiving updates.
     */
    override fun addSnapshotListener(
        onSnapshot: (List<T>) -> Unit,
        onError: (Exception) -> Unit
    ): () -> Unit {
        val listener = firestore.collection(pathProvider())
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onError(error)
                    return@addSnapshotListener
                }
                val docs = snapshot?.documents?.mapNotNull { it.toObject(docClass) } ?: emptyList()
                onSnapshot(docs)
            }
        return { listener.remove() }
    }
}
