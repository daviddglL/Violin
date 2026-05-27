package com.violinmaster.app.data.firebase

/**
 * Firestore-compatible message data class for teacher-student chat.
 *
 * REQ-CHAT-001: Each document in assignments/{assignmentId}/messages/{autoId}
 * contains senderUsername, senderRole, text, attachmentUrl, attachmentType,
 * timestamp (server Timestamp), and read.
 *
 * The assignmentId is NOT stored in the document itself — it lives in the
 * Firestore collection path. For Room caching, see [CachedMessage].
 */
data class Message(
    val id: String = "",               // Firestore auto-generated document ID
    val senderUsername: String,
    val senderRole: String,            // "TEACHER" or "STUDENT"
    val text: String = "",
    val attachmentUrl: String? = null,
    val attachmentType: String? = null, // "video" or null
    val timestamp: Long = System.currentTimeMillis(),
    val read: Boolean = false
)
