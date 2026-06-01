package com.violinmaster.app.domain.usecase

import com.violinmaster.app.data.IChatRepository
import com.violinmaster.app.data.firebase.Message
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Retrieves chat messages for an assignment.
 *
 * No Android imports — delegating to [IChatRepository].
 */
class GetMessagesUseCase @Inject constructor(
  private val chatRepository: IChatRepository
) {
  /**
   * Returns a Flow of messages for the given assignment.
   *
   * @param assignmentId The assignment chat room identifier.
   */
  operator fun invoke(assignmentId: String): Flow<List<Message>> {
    return chatRepository.loadMessages(assignmentId)
  }
}
