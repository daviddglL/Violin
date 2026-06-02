package com.violinmaster.app.domain.usecase

import com.violinmaster.app.data.IChatRepository
import com.violinmaster.app.data.firebase.Message
import com.violinmaster.app.di.AuthManager

/**
 * Sends a chat message for an assignment.
 *
 * Reads current user from [AuthManager] for sender identity.
 * No Android imports — delegating to [IChatRepository].
 *
 * REQ-ARCH-001-S3: Pure-Kotlin use case testable with mocks.
 */
class SendMessageUseCase constructor(
  private val chatRepository: IChatRepository,
  private val authManager: AuthManager
) {
  /**
   * Sends a text message to the chat room for [assignmentId].
   *
   * @param assignmentId The assignment's chat room identifier.
   * @param text The message text (must be non-blank).
   * @return The created [Message] or null if validation fails.
   */
  suspend operator fun invoke(assignmentId: String, text: String): Message? {
    if (text.isBlank()) return null
    val currentUser = authManager.currentUser.value ?: return null

    val message = Message(
      senderUsername = currentUser.username,
      senderRole = currentUser.role,
      text = text.trim(),
      timestamp = System.currentTimeMillis()
    )

    return chatRepository.sendMessage(assignmentId, message)
  }
}
