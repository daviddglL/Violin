package com.violinmaster.app.domain.usecase

import com.violinmaster.app.data.IChatRepository
import com.violinmaster.app.data.firebase.Message
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class GetMessagesUseCaseTest {

  private lateinit var chatRepo: FakeGetMessagesChatRepo
  private lateinit var useCase: GetMessagesUseCase

  @Before
  fun setup() {
    chatRepo = FakeGetMessagesChatRepo()
    useCase = GetMessagesUseCase(chatRepo)
  }

  @Test
  fun `returns messages flow from repository`() = runTest {
    val messages = listOf(
      Message(id = "1", senderUsername = "t", senderRole = "TEACHER", text = "Hi", timestamp = 100),
      Message(id = "2", senderUsername = "s", senderRole = "STUDENT", text = "Hello", timestamp = 200)
    )
    chatRepo.messagesFlow["A1"] = MutableStateFlow(messages)

    val result = useCase("A1").first()

    assertEquals(2, result.size)
    assertEquals("Hi", result[0].text)
    assertEquals("Hello", result[1].text)
  }

  @Test
  fun `empty assignment returns empty flow`() = runTest {
    chatRepo.messagesFlow["UNKNOWN"] = MutableStateFlow(emptyList())

    val result = useCase("UNKNOWN").first()

    assertTrue(result.isEmpty())
  }

  class FakeGetMessagesChatRepo : IChatRepository {
    val messagesFlow = mutableMapOf<String, MutableStateFlow<List<Message>>>()

    override suspend fun sendMessage(assignmentId: String, message: Message): Message = message
    override fun loadMessages(assignmentId: String): Flow<List<Message>> {
      return messagesFlow.getOrPut(assignmentId) { MutableStateFlow(emptyList()) }
    }
    override suspend fun clearMessagesForAssignment(assignmentId: String) {}
  }
}
