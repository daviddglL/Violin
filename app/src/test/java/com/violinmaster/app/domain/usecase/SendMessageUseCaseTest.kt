package com.violinmaster.app.domain.usecase

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.violinmaster.app.data.IChatRepository
import com.violinmaster.app.data.UserAccount
import com.violinmaster.app.data.firebase.Message
import com.violinmaster.app.di.AuthManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class SendMessageUseCaseTest {

  private lateinit var context: Context
  private lateinit var chatRepo: FakeSendMessageChatRepo
  private lateinit var authManager: AuthManager
  private lateinit var useCase: SendMessageUseCase

  @Before
  fun setup() {
    context = ApplicationProvider.getApplicationContext()
    chatRepo = FakeSendMessageChatRepo()
    authManager = AuthManager(context)
    useCase = SendMessageUseCase(chatRepo, authManager)
  }

  @After
  fun tearDown() {
    authManager.clearSession()
  }

  @Test
  fun `sends message via repository`() = runTest {
    val user = UserAccount(username = "teacher1", role = "TEACHER", hashedPassword = "h", salt = "s")
    authManager.restoreCurrentUser(user)

    val result = useCase("A1", "Great job!")

    assertNotNull(result)
    assertEquals("Great job!", result!!.text)
    assertEquals("teacher1", result.senderUsername)
    assertEquals("A1", chatRepo.lastAssignmentId)
    assertEquals("Great job!", chatRepo.lastMessage?.text)
  }

  @Test
  fun `blank text returns null`() = runTest {
    val user = UserAccount(username = "teacher1", role = "TEACHER", hashedPassword = "h", salt = "s")
    authManager.restoreCurrentUser(user)

    val result = useCase("A1", "")
    assertNull(result)
    assertNull(chatRepo.lastMessage)
  }

  @Test
  fun `no current user returns null`() = runTest {
    val result = useCase("A1", "Hello")
    assertNull(result)
    assertNull(chatRepo.lastMessage)
  }

  class FakeSendMessageChatRepo : IChatRepository {
    var lastAssignmentId: String? = null
    var lastMessage: Message? = null
    override suspend fun sendMessage(assignmentId: String, message: Message): Message {
      lastAssignmentId = assignmentId; lastMessage = message; return message
    }
    override fun loadMessages(assignmentId: String): Flow<List<Message>> = MutableStateFlow(emptyList())
    override suspend fun clearMessagesForAssignment(assignmentId: String) {}
  }
}
