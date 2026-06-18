package com.violinmaster.app.data.firebase

/**
 * In-memory fake implementation of [IFirestoreCollection] for unit testing.
 *
 * Stores documents in a mutable list and notifies registered snapshot
 * listeners when documents are added or removed.
 *
 * REQ-CSYNC-001: Enables testing FirestoreSyncRepository without Firebase emulator.
 *
 * @param D The Firestore document type.
 */
class FakeFirestoreCollection<D> : IFirestoreCollection<D> {

    private val documents = mutableListOf<Pair<String, D>>()
    private val listeners = mutableListOf<Pair<(List<D>) -> Unit, (Exception) -> Unit>>()

    // ── IFirestoreCollection Implementation ──────────────────────────────

    override suspend fun setDocument(id: String, document: D) {
        val existingIndex = documents.indexOfFirst { it.first == id }
        if (existingIndex >= 0) {
            documents[existingIndex] = id to document
        } else {
            documents.add(id to document)
        }
        notifySnapshot()
    }

    override suspend fun deleteDocument(id: String) {
        documents.removeAll { it.first == id }
        notifySnapshot()
    }

    override fun addSnapshotListener(
        onSnapshot: (List<D>) -> Unit,
        onError: (Exception) -> Unit
    ): () -> Unit {
        listeners.add(onSnapshot to onError)
        // Fire immediately with current state (mimics Firestore behavior)
        onSnapshot(documents.map { it.second })
        return {
            listeners.removeAll { it.first == onSnapshot && it.second == onError }
        }
    }

    override suspend fun queryByField(field: String, value: Any): List<D> {
        return documents.filter { (_, doc) ->
            val docValue = doc?.let { d ->
                try {
                    val f = d!!::class.java.getDeclaredField(field)
                    f.isAccessible = true
                    f.get(d)
                } catch (e: Exception) {
                    null
                }
            }
            docValue == value
        }.map { it.second }
    }

    override suspend fun deleteSubcollection(subcollectionPath: String) {
        documents.removeAll { it.first.startsWith("$subcollectionPath/") }
        notifySnapshot()
    }

    // ── Test Helpers ─────────────────────────────────────────────────────

    /**
     * Returns all stored document ID+data pairs for test assertions.
     */
    fun getAllDocuments(): List<Pair<String, D>> = documents.toList()

    /**
     * Simulates a Firestore listener error for testing error handling.
     */
    fun simulateError(error: Exception) {
        for ((_, onError) in listeners.toList()) {
            onError(error)
        }
    }

    // ── Private ──────────────────────────────────────────────────────────

    private fun notifySnapshot() {
        val currentDocs = documents.map { it.second }
        for ((onSnapshot, _) in listeners.toList()) {
            onSnapshot(currentDocs)
        }
    }
}
