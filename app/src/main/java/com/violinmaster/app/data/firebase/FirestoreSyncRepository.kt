package com.violinmaster.app.data.firebase

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch

/**
 * Generic base class for cloud-first entity sync (REQ-CSYNC-001, REQ-CSYNC-002).
 *
 * Provides the dual-write pattern observed in ChatRepository as a reusable
 * abstraction for all entity types:
 * 1. **Write**: Firestore `set()` → Room `insertCache()` (Firestore-first write-through)
 * 2. **Observe**: Room Flow (immediate cache) → Firestore snapshot listener
 *    updates Room → Room Flow re-emits (REQ-CSYNC-002)
 * 3. **Delete**: Firestore `delete()` → Room `deleteCache()`
 *
 * Subclasses implement entity-specific mappings and DAO operations.
 *
 * @param T The Room/domain entity type (e.g., [com.violinmaster.app.data.PracticeSession]).
 * @param D The Firestore document type (e.g., [SessionDoc]).
 * @param collection The Firestore collection abstraction for testability.
 */
abstract class FirestoreSyncRepository<T, D>(
    private val collection: IFirestoreCollection<D>
) {
    // ── Entity ↔ Document Mapping (implemented by subclasses) ────────────

    /** Converts a Room entity to its Firestore document representation. */
    protected abstract fun T.toFirestoreDoc(): D

    /** Converts a Firestore document back to a Room entity. */
    protected abstract fun D.toEntity(): T

    /** Returns the Firestore document ID for a given document. */
    protected abstract fun D.docId(): String

    // ── Cache Operations (implemented by subclasses with specific DAOs) ──

    /** Inserts or updates an entity in the Room cache. */
    protected abstract suspend fun insertCache(entity: T)

    /** Removes an entity from the Room cache by document ID. */
    protected abstract suspend fun deleteCache(docId: String)

    /** Returns a Flow of all cached entities from Room. */
    protected abstract fun observeCache(): Flow<List<T>>

    // ── Public API ───────────────────────────────────────────────────────

    /**
     * Writes an entity to Firestore first, then caches it locally.
     *
     * Firestore-first write-through: the cloud write is the authoritative
     * operation. If Firestore write fails, the exception propagates to the
     * caller (ViewModel handles UI feedback like Snackbar). Room cache is
     * only updated after a successful Firestore write.
     *
     * @param entity The entity to sync to Firestore and cache.
     */
    suspend fun write(entity: T) {
        val doc = entity.toFirestoreDoc()
        collection.setDocument(doc.docId(), doc)
        insertCache(entity)
    }

    /**
     * Observes entities via Room Flow with Firestore real-time sync.
     *
     * The returned Flow emits:
     * 1. Current Room cache state (immediate, from Room)
     * 2. Re-emits when Firestore snapshot listener detects changes
     *    and updates the Room cache
     *
     * If the Firestore listener encounters an error, the error is silently
     * ignored — Room continues serving the last-known-good cache state.
     *
     * @return A cold Flow of entity lists. Collectors must be in an active
     *         coroutine scope (typically viewModelScope).
     */
    fun observe(): Flow<List<T>> = callbackFlow {
        // Firestore real-time listener → updates Room cache
        val cancelListener = collection.addSnapshotListener(
            onSnapshot = { docs ->
                // Update Room cache from Firestore snapshot
                CoroutineScope(Dispatchers.IO).launch {
                    for (doc in docs) {
                        try {
                            insertCache(doc.toEntity())
                        } catch (e: Exception) {
                            // Log and skip malformed documents — Room cache
                            // continues serving last-known-good data
                        }
                    }
                }
            },
            onError = { _ ->
                // Listener errors are silently ignored — Room serves
                // last-known-good cache state (REQ-CSYNC-003)
            }
        )

        // Room cache Flow → re-emit into this callbackFlow
        val job = CoroutineScope(Dispatchers.IO).launch {
            observeCache().collect { entities ->
                trySend(entities)
            }
        }

        awaitClose {
            cancelListener()
            job.cancel()
        }
    }

    /**
     * Deletes an entity from both Firestore and Room cache.
     *
     * Firestore deletion is attempted first. If it fails, the exception
     * propagates to the caller for UI feedback. Room cache is cleared
     * only after successful Firestore deletion.
     *
     * @param docId The Firestore document ID to delete.
     */
    suspend fun delete(docId: String) {
        collection.deleteDocument(docId)
        deleteCache(docId)
    }
}
