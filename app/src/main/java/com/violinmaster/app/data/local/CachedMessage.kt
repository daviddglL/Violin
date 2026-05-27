package com.violinmaster.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.violinmaster.app.data.firebase.Message

/**
 * Room entity mirroring Firestore message fields for offline cache.
 *
 * REQ-CHAT-004: CachedMessage mirrors Message fields plus assignmentId
 * for scoped queries. The [assignmentId] field enables querying messages
 * by assignment without crossing Firestore collection boundaries.
 *
 * Differences from [Message]:
 * - [attachmentUrl] and [attachmentType] are non-null with empty defaults
 *   (SQLite NOT NULL constraint).
 * - [assignmentId] is added for Room query filtering.
 */
@Entity(tableName = "cached_messages")
data class CachedMessage(
    @PrimaryKey val id: String,             // Matches Firestore document ID
    val assignmentId: String,               // For scoped Room queries
    val senderUsername: String,
    val senderRole: String,                 // "TEACHER" or "STUDENT"
    val text: String,
    val attachmentUrl: String = "",
    val attachmentType: String = "",
    val timestamp: Long,
    val read: Boolean = false
)

/** Converts a Firestore [Message] to a Room [CachedMessage] for local storage. */
fun Message.toCachedMessage(assignmentId: String): CachedMessage = CachedMessage(
    id = id,
    assignmentId = assignmentId,
    senderUsername = senderUsername,
    senderRole = senderRole,
    text = text,
    attachmentUrl = attachmentUrl ?: "",
    attachmentType = attachmentType ?: "",
    timestamp = timestamp,
    read = read
)

/** Converts a Room [CachedMessage] back to a Firestore-compatible [Message]. */
fun CachedMessage.toMessage(): Message = Message(
    id = id,
    senderUsername = senderUsername,
    senderRole = senderRole,
    text = text,
    attachmentUrl = attachmentUrl.ifEmpty { null },
    attachmentType = attachmentType.ifEmpty { null },
    timestamp = timestamp,
    read = read
)
