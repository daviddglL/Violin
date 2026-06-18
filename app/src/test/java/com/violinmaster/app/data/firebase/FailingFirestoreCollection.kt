package com.violinmaster.app.data.firebase

/**
 * Test fake that throws on Firestore operations to simulate write/delete failures.
 *
 * Used to verify that [FirestoreSyncRepository] properly propagates errors
 * and does not update the Room cache when the cloud operation fails.
 *
 * REQ-CSYNC-003: Write/delete failure propagates, Room cache NOT updated on failure.
 */
class FailingFirestoreCollection<D> : IFirestoreCollection<D> {

    private val documents = mutableListOf<Pair<String, D>>()
    private val listeners = mutableListOf<Pair<(List<D>) -> Unit, (Exception) -> Unit>>()

    /**
     * Set to true to make the NEXT operation succeed. Reset after one success.
     */
    var nextWriteSucceeds: Boolean = false

    /**
     * Set to true to make the NEXT deleteDocument call throw.
     */
    var nextDeleteFails: Boolean = false

    override suspend fun setDocument(id: String, document: D) {
        if (!nextWriteSucceeds) {
            throw RuntimeException("Simulated Firestore write failure")
        }
        nextWriteSucceeds = false
        val existingIndex = documents.indexOfFirst { it.first == id }
        if (existingIndex >= 0) {
            documents[existingIndex] = id to document
        } else {
            documents.add(id to document)
        }
        notifySnapshot()
    }

    override suspend fun deleteDocument(id: String) {
        if (nextDeleteFails) {
            nextDeleteFails = false
            throw RuntimeException("Simulated Firestore delete failure")
        }
        documents.removeAll { it.first == id }
        notifySnapshot()
    }

    override fun addSnapshotListener(
        onSnapshot: (List<D>) -> Unit,
        onError: (Exception) -> Unit
    ): () -> Unit {
        listeners.add(onSnapshot to onError)
        onSnapshot(documents.map { it.second })
        return {
            listeners.removeAll { it.first == onSnapshot && it.second == onError }
        }
    }

    override suspend fun queryByField(field: String, value: Any): List<D> {
        throw RuntimeException("Simulated Firestore query failure")
    }

    override suspend fun deleteSubcollection(subcollectionPath: String) {
        documents.removeAll { it.first.startsWith("$subcollectionPath/") }
        notifySnapshot()
    }

    fun getAllDocuments(): List<Pair<String, D>> = documents.toList()

    fun simulateError(error: Exception) {
        for ((_, onError) in listeners.toList()) {
            onError(error)
        }
    }

    private fun notifySnapshot() {
        val currentDocs = documents.map { it.second }
        for ((onSnapshot, _) in listeners.toList()) {
            onSnapshot(currentDocs)
        }
    }
}
