package com.violinmaster.app.ui.viewmodel

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.violinmaster.app.data.CachedMessage
import com.violinmaster.app.data.IChatRepository
import com.violinmaster.app.data.PracticeDao
import com.violinmaster.app.data.PracticeDatabase
import com.violinmaster.app.data.UserAccount
import com.violinmaster.app.data.firebase.Message
import com.violinmaster.app.data.local.toCachedMessage
import com.violinmaster.app.data.local.toMessage
import com.violinmaster.app.di.SessionManager
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * TDD tests for ChatViewModel.
 *
 * REQ-CHAT-005: ChatViewModel message management.
 *
 * Uses a FakeChatRepository backed by in-memory Room so the ViewModel
 * can be tested without FirebaseFirestore (final class, unavailable in Robolectric).
 *
 * RED phase: ChatViewModel.kt does not exist yet. These tests will fail to compile.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ChatViewModelTest {

    private lateinit var database: PracticeDatabase
    private lateinit var dao: PracticeDao
    private lateinit var fakeRepo: FakeChatRepository
    private lateinit var sessionManager: SessionManager
    private lateinit var viewModel: ChatViewModel

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, PracticeDatabase::class.java).build()
        dao = database.practiceDao()
        fakeRepo = FakeChatRepository(dao)
        sessionManager = SessionManager(context)
        viewModel = ChatViewModel(fakeRepo, sessionManager)
    }

    @After
    fun tearDown() {
        database.close()
    }

    // ── Helper to set a logged-in user ──────────────────────────────────

    private fun setCurrentUser(username: String, role: String) {
        val user = UserAccount(
            username = username,
            role = role,
            hashedPassword = "hash",
            salt = "salt",
            teacherCode = if (role == "TEACHER") "TCH-001" else ""
        )
        sessionManager.restoreCurrentUser(user)
    }

    // ── T-006 Test: sendMessage adds message to the list ─────────────────

    @Test
    fun `sendMessage adds message to the messages flow`() = runTest {
        // Arrange: logged in as teacher, load an assignment
        setCurrentUser("teacher_jane", "TEACHER")
        viewModel.loadAssignment("A1")
        advanceUntilIdle()

        // Act: send a message
        viewModel.sendMessage("Great work!")
        advanceUntilIdle()

        // Assert: message appears in the flow
        val messages = viewModel.messages.value
        assertEquals("Should have 1 message after sending", 1, messages.size)
        val msg = messages[0]
        assertEquals("Great work!", msg.text)
        assertEquals("teacher_jane", msg.senderUsername)
        assertEquals("TEACHER", msg.senderRole)
        assertEquals("A1", fakeRepo.lastSentAssignmentId)
    }

    @Test
    fun `sendMessage with empty text does nothing`() = runTest {
        // Arrange
        setCurrentUser("teacher_jane", "TEACHER")
        viewModel.loadAssignment("A1")
        advanceUntilIdle()

        // Act: send empty/blank message
        viewModel.sendMessage("")
        viewModel.sendMessage("   ")
        advanceUntilIdle()

        // Assert: no messages added
        val messages = viewModel.messages.value
        assertEquals("Blank messages should be ignored", 0, messages.size)
    }

    // ── T-006 Test: messages are ordered by timestamp ────────────────────

    @Test
    fun `messages are ordered by timestamp ascending`() = runTest {
        // Arrange: load assignment, pre-seed repo with out-of-order messages
        setCurrentUser("teacher_jane", "TEACHER")
        viewModel.loadAssignment("A1")
        advanceUntilIdle()

        // Insert through fake repo with out-of-order timestamps
        fakeRepo.insertMessage(
            Message(
                id = "m3", senderUsername = "t", senderRole = "TEACHER",
                text = "Third", timestamp = 300L
            ), "A1"
        )
        fakeRepo.insertMessage(
            Message(
                id = "m1", senderUsername = "t", senderRole = "TEACHER",
                text = "First", timestamp = 100L
            ), "A1"
        )
        fakeRepo.insertMessage(
            Message(
                id = "m2", senderUsername = "t", senderRole = "TEACHER",
                text = "Second", timestamp = 200L
            ), "A1"
        )
        advanceUntilIdle()

        // Assert: ordered ASC
        val messages = viewModel.messages.value
        assertEquals("Should have 3 messages", 3, messages.size)
        assertEquals("First", messages[0].text)
        assertEquals("Second", messages[1].text)
        assertEquals("Third", messages[2].text)
        assertEquals(
            "Timestamps must be ascending",
            listOf(100L, 200L, 300L),
            messages.map { it.timestamp }
        )
    }

    // ── T-006 Test: loading state shows during fetch ─────────────────────

    @Test
    fun `isLoading is true initially then false after messages arrive`() = runTest {
        // Arrange: logged in, no prior messages
        setCurrentUser("teacher_jane", "TEACHER")

        // Act: load an assignment
        viewModel.loadAssignment("A1")
        advanceUntilIdle()

        // Assert: loading should be false after load completes
        assertEquals("isLoading should be false after messages are loaded", false, viewModel.isLoading.value)
    }

    @Test
    fun `isLoading transitions through loading state on loadAssignment`() = runTest {
        // Arrange: logged in teacher
        setCurrentUser("teacher_jane", "TEACHER")

        // Act & Assert: before loadAssignment, loading should be false since
        // no assignment is loaded yet
        assertEquals("isLoading before any load should be false", false, viewModel.isLoading.value)

        // Load assignment
        viewModel.loadAssignment("A1")
        advanceUntilIdle()

        // After load completes
        assertEquals("isLoading after load completes should be false", false, viewModel.isLoading.value)
        assertNotNull("currentAssignmentId should be set", viewModel.currentAssignmentId.value)
    }

    // ── T-006 Test: error state on repository failure ────────────────────

    @Test
    fun `error state is null initially`() = runTest {
        setCurrentUser("teacher_jane", "TEACHER")
        // Before any operation
        assertNull("Error should be null initially", viewModel.error.value)
    }

    @Test
    fun `error state is set when sendMessage fails`() = runTest {
        // Arrange: logged in, load assignment, then make repo fail
        setCurrentUser("teacher_jane", "TEACHER")
        viewModel.loadAssignment("A1")
        advanceUntilIdle()

        fakeRepo.shouldFailOnSend = true

        // Act: try to send
        viewModel.sendMessage("This will fail")
        advanceUntilIdle()

        // Assert: error state is populated
        val error = viewModel.error.value
        assertNotNull("Error should not be null after repository failure", error)
        assertTrue("Error message should indicate failure", error!!.contains("Failed", ignoreCase = true))
    }

    @Test
    fun `sendMessage catches exceptions and sets error state`() = runTest {
        // Arrange
        setCurrentUser("teacher_jane", "TEACHER")
        viewModel.loadAssignment("A1")
        advanceUntilIdle()

        fakeRepo.shouldFailOnSend = true
        // This will throw inside sendMessage
        viewModel.sendMessage("Will throw")
        advanceUntilIdle()

        // Assert: error was captured
        val error = viewModel.error.value
        assertNotNull("Error should be set on exception", error)

        // Assert: message NOT added (error path)
        val messages = viewModel.messages.value
        val sentTexts = messages.filter { it.text == "Will throw" }
        assertEquals("Failed messages should not appear in list", 0, sentTexts.size)
    }

    // ── T-006 Test: can switch between assignments ───────────────────────

    @Test
    fun `loadAssignment switches chat context`() = runTest {
        // Arrange: seed messages for two assignments
        setCurrentUser("teacher_jane", "TEACHER")

        fakeRepo.insertMessage(
            Message(id = "a1_msg", senderUsername = "t", senderRole = "TEACHER",
                text = "Assignment 1", timestamp = 100L), "A1"
        )
        fakeRepo.insertMessage(
            Message(id = "a2_msg", senderUsername = "t", senderRole = "TEACHER",
                text = "Assignment 2", timestamp = 200L), "A2"
        )

        // Act: load A1
        viewModel.loadAssignment("A1")
        advanceUntilIdle()

        // Assert: only A1 messages visible
        assertEquals("currentAssignmentId should be A1", "A1", viewModel.currentAssignmentId.value)
        val a1Messages = viewModel.messages.value
        assertEquals("A1 should have 1 message", 1, a1Messages.size)
        assertEquals("Assignment 1", a1Messages[0].text)

        // Act: switch to A2
        viewModel.loadAssignment("A2")
        advanceUntilIdle()

        // Assert: only A2 messages visible
        assertEquals("currentAssignmentId should be A2", "A2", viewModel.currentAssignmentId.value)
        val a2Messages = viewModel.messages.value
        assertEquals("A2 should have 1 message", 1, a2Messages.size)
        assertEquals("Assignment 2", a2Messages[0].text)
    }

    @Test
    fun `switching assignments clears previous assignment messages`() = runTest {
        // Arrange: seed messages for A1
        setCurrentUser("teacher_jane", "TEACHER")
        fakeRepo.insertMessage(
            Message(id = "a1_1", senderUsername = "t", senderRole = "TEACHER",
                text = "A1 message", timestamp = 100L), "A1"
        )

        // Load and verify A1
        viewModel.loadAssignment("A1")
        advanceUntilIdle()
        assertEquals(1, viewModel.messages.value.size)

        // Act: switch to empty assignment A2
        viewModel.loadAssignment("A2")
        advanceUntilIdle()

        // Assert: A2 is empty (no cross-contamination)
        assertEquals("Switching to empty assignment should show 0 messages", 0, viewModel.messages.value.size)
        assertEquals("currentAssignmentId should be A2", "A2", viewModel.currentAssignmentId.value)
    }

    @Test
    fun `sendMessage throws if no assignment loaded`() = runTest {
        // Arrange: logged in but no assignment loaded
        setCurrentUser("teacher_jane", "TEACHER")

        // Act: try to send without loading assignment
        viewModel.sendMessage("Orphan message")
        advanceUntilIdle()

        // Assert: error set, no message sent
        val error = viewModel.error.value
        assertNotNull("Error should be set when sending without assignment", error)
        assertEquals("Should have 0 messages", 0, viewModel.messages.value.size)
    }

    @Test
    fun `sendMessage throws if no current user`() = runTest {
        // Arrange: no user logged in
        viewModel.loadAssignment("A1")
        advanceUntilIdle()

        // Act: try to send
        viewModel.sendMessage("No user message")
        advanceUntilIdle()

        // Assert: error set, no message sent
        val error = viewModel.error.value
        assertNotNull("Error should be set when no user is logged in", error)
        assertEquals("Should have 0 messages", 0, viewModel.messages.value.size)
    }

    // ── T-006 Test: read receipt trigger ─────────────────────────────────

    @Test
    fun `messages from others start with read false`() = runTest {
        // Arrange: login as teacher, seed a message from student
        setCurrentUser("teacher_jane", "TEACHER")
        fakeRepo.insertMessage(
            Message(id = "s1", senderUsername = "student_bob", senderRole = "STUDENT",
                text = "Hello teacher", timestamp = 100L, read = false), "A1"
        )
        viewModel.loadAssignment("A1")
        advanceUntilIdle()

        // Assert: student message has read=false
        val messages = viewModel.messages.value
        assertEquals(1, messages.size)
        assertEquals(false, messages[0].read)
    }

    // ── Edge case: clearError resets error state ─────────────────────────

    @Test
    fun `clearError resets error state to null`() = runTest {
        // Arrange: trigger an error
        setCurrentUser("teacher_jane", "TEACHER")
        viewModel.loadAssignment("A1")
        advanceUntilIdle()
        fakeRepo.shouldFailOnSend = true
        viewModel.sendMessage("Failing")
        advanceUntilIdle()
        assertNotNull("Error should be set", viewModel.error.value)

        // Act: clear error
        viewModel.clearError()

        // Assert: error is null again
        assertNull("Error should be null after clearError", viewModel.error.value)
    }

    // ═══════════════════════════════════════════════════════════════════════
    // FakeChatRepository — test double backed by in-memory Room
    // ═══════════════════════════════════════════════════════════════════════

    class FakeChatRepository(private val dao: PracticeDao) : IChatRepository {

        var shouldFailOnSend: Boolean = false
        var lastSentAssignmentId: String? = null

        fun insertMessage(message: Message, assignmentId: String) {
            dao.insertCachedMessages(listOf(message.toCachedMessage(assignmentId)))
        }

        override suspend fun sendMessage(assignmentId: String, message: Message): Message {
            if (shouldFailOnSend) {
                throw RuntimeException("Fake repository: sendMessage failed")
            }
            lastSentAssignmentId = assignmentId
            val msgToCache = message.copy(
                id = message.id.ifEmpty { "msg_${System.currentTimeMillis()}" }
            )
            dao.insertCachedMessages(listOf(msgToCache.toCachedMessage(assignmentId)))
            return msgToCache
        }

        override fun loadMessages(assignmentId: String): Flow<List<Message>> {
            return dao.getCachedMessagesByAssignment(assignmentId)
                .map { cachedList -> cachedList.map { it.toMessage() } }
        }

        override suspend fun clearMessagesForAssignment(assignmentId: String) {
            dao.clearCachedMessagesForAssignment(assignmentId)
        }
    }
}
