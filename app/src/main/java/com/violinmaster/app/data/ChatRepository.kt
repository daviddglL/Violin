package com.violinmaster.app.data

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.violinmaster.app.data.firebase.FirebaseCollections
import com.violinmaster.app.data.firebase.Message
import com.violinmaster.app.data.local.toCachedMessage
import com.violinmaster.app.data.local.toMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for teacher-student chat messages.
 *
 * Coordinates between Firestore (real-time sync) and Room (offline cache).
 * REQ-CHAT-001–004, REQ-CHAT-008.
 *
 * @param firestore Firebase Firestore instance for real-time messaging.
 * @param dao Room DAO for offline message caching.
 */
@Singleton
class ChatRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val dao: PracticeDao
) : IChatRepository {

    /**
     * Sends a message to Firestore and caches it locally.
     *
     * REQ-CHAT-001: Creates a Firestore document at
     * assignments/{assignmentId}/messages/{autoId}.
     * On success, caches to Room for offline access.
     *
     * @param assignmentId The assignment to send the message to.
     * @param message The message to send (id is ignored, Firestore auto-generates).
     * @return The message with Firestore-generated document ID.
     */
    override suspend fun sendMessage(assignmentId: String, message: Message): Message {
        val collectionPath = FirebaseCollections.messagesPath(assignmentId)
        val docRef = firestore.collection(collectionPath).document()
        val msgWithId = message.copy(id = docRef.id)

        docRef.set(msgWithId).await()

        // Cache locally after successful Firestore write
        dao.insertCachedMessages(listOf(msgWithId.toCachedMessage(assignmentId)))

        return msgWithId
    }

    /**
     * Observes messages for an assignment via Firestore real-time listener
     * with Room cache fallback.
     *
     * REQ-CHAT-003: Firestore snapshot listener emits updates reactively.
     * REQ-CHAT-004: Room cache serves data when offline.
     *
     * The returned Flow first emits from Room cache (immediate), then
     * the Firestore listener updates Room on each snapshot change. Room's
     * Flow automatically emits the updated data.
     *
     * @param assignmentId The assignment to observe messages for.
     * @return Flow of messages ordered by timestamp ascending.
     */
    fun loadMessages(assignmentId: String): Flow<List<Message>> {
        return callbackFlow {
            val collectionPath = FirebaseCollections.messagesPath(assignmentId)

            // Set up Firestore real-time listener → updates Room cache
            val listener: ListenerRegistration = firestore.collection(collectionPath)
                .orderBy("timestamp")
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        // Offline or error: Room cache already serves stale data
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        val cachedMessages = snapshot.documents.mapNotNull { doc ->
                            try {
                                val msg = doc.toObject(Message::class.java)
                                msg?.copy(id = doc.id)?.toCachedMessage(assignmentId)
                            } catch (e: Exception) {
                                null // Skip malformed documents
                            }
                        }
                        if (cachedMessages.isNotEmpty()) {
                            // Fire-and-forget: cache update triggers Room Flow emissions
                            CoroutineScope(Dispatchers.IO).launch {
                                    dao.insertCachedMessages(cachedMessages)
                                }
                        }
                    }
                }

            // Room cache Flow → mapped to Message objects
            val roomFlow = dao.getCachedMessagesByAssignment(assignmentId)
                .map { cachedList -> cachedList.map { it.toMessage() } }

            // Re-emit Room Flow into this callbackFlow
            val job = CoroutineScope(Dispatchers.IO).launch {
                    roomFlow.collect { messages ->
                        trySend(messages)
                    }
                }

            awaitClose {
                listener.remove()
                job.cancel()
            }
        }
    }

    /**
     * Clears all messages for an assignment from both Firestore and Room cache.
     *
     * Note: Deleting subcollections in Firestore requires deleting each document
     * individually (Firestore does not support recursive delete via SDK).
     * This implementation deletes all known documents from the cached snapshot.
     *
     * @param assignmentId The assignment whose messages should be cleared.
     */
    suspend fun clearMessagesForAssignment(assignmentId: String) {
        val collectionPath = FirebaseCollections.messagesPath(assignmentId)

        // Delete from Firestore collection (batch delete all documents)
        val snapshot = firestore.collection(collectionPath).get().await()
        val batch = firestore.batch()
        for (doc in snapshot.documents) {
            batch.delete(doc.reference)
        }
        batch.commit().await()

        // Clear Room cache
        dao.clearCachedMessagesForAssignment(assignmentId)
    }
}
