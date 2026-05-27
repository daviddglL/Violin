package com.violinmaster.app.data

import com.violinmaster.app.data.firebase.Message
import kotlinx.coroutines.flow.Flow

/**
 * Interface for chat message persistence, enabling test doubles.
 *
 * REQ-CHAT-001, REQ-CHAT-003, REQ-CHAT-004.
 */
interface IChatRepository {
    suspend fun sendMessage(assignmentId: String, message: Message): Message
    fun loadMessages(assignmentId: String): Flow<List<Message>>
    suspend fun clearMessagesForAssignment(assignmentId: String)
}
