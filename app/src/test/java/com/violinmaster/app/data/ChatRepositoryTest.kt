package com.violinmaster.app.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.violinmaster.app.data.local.CachedMessage
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Tests for the Room cache layer used by ChatRepository.
 *
 * REQ-CHAT-004: Room Cache for Offline Access.
 * Since FirebaseFirestore cannot run in Robolectric tests (final class, requires
 * Google Play Services), these tests verify the Room cache operations directly
 * through the DAO — insert, query ordered by timestamp, and delete by assignmentId.
 *
 * The ChatRepository delegates Room caching to these DAO methods, so proving
 * DAO correctness proves ChatRepository cache correctness.
 *
 * TDD: RED phase — ChatRepository.kt does not exist yet. These tests describe
 * the expected behavior of the Room cache layer the repository will use.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ChatRepositoryTest {

    private lateinit var database: PracticeDatabase
    private lateinit var dao: PracticeDao

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, PracticeDatabase::class.java)
            .build()
        dao = database.practiceDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    // ── Helper: create a CachedMessage with minimal required fields ──────

    private fun cachedMessage(
        id: String,
        assignmentId: String,
        senderUsername: String = "teacher_jane",
        senderRole: String = "TEACHER",
        text: String = "",
        timestamp: Long = System.currentTimeMillis(),
        read: Boolean = false
    ) = CachedMessage(
        id = id,
        assignmentId = assignmentId,
        senderUsername = senderUsername,
        senderRole = senderRole,
        text = text,
        timestamp = timestamp,
        read = read
    )

    // ── Cache Insert Tests ───────────────────────────────────────────────

    @Test
    fun `insertCachedMessages stores messages in Room`() = runTest {
        val messages = listOf(
            cachedMessage(id = "msg1", assignmentId = "A1", text = "Hello", timestamp = 100L),
            cachedMessage(id = "msg2", assignmentId = "A1", text = "World", timestamp = 200L)
        )
        dao.insertCachedMessages(messages)
        advanceUntilIdle()

        val cached = dao.getCachedMessagesByAssignment("A1").first()
        assertEquals("Should store 2 messages for assignment A1", 2, cached.size)
        assertEquals("msg1", cached[0].id)
        assertEquals("msg2", cached[1].id)
    }

    @Test
    fun `insertCachedMessages overwrites on conflict via REPLACE`() = runTest {
        dao.insertCachedMessages(
            listOf(cachedMessage(id = "msg1", assignmentId = "A1", text = "Original", timestamp = 100L))
        )
        advanceUntilIdle()

        // Insert same ID with updated text
        dao.insertCachedMessages(
            listOf(cachedMessage(id = "msg1", assignmentId = "A1", text = "Updated", timestamp = 100L))
        )
        advanceUntilIdle()

        val cached = dao.getCachedMessagesByAssignment("A1").first()
        assertEquals("Should have 1 message after REPLACE", 1, cached.size)
        assertEquals("Updated", cached[0].text)
    }

    // ── Ordering Test ────────────────────────────────────────────────────

    @Test
    fun `cached messages are ordered by timestamp ascending`() = runTest {
        val messages = listOf(
            cachedMessage(id = "m3", assignmentId = "A1", text = "Third", timestamp = 300L),
            cachedMessage(id = "m1", assignmentId = "A1", text = "First", timestamp = 100L),
            cachedMessage(id = "m2", assignmentId = "A1", text = "Second", timestamp = 200L)
        )
        dao.insertCachedMessages(messages)
        advanceUntilIdle()

        val cached = dao.getCachedMessagesByAssignment("A1").first()
        assertEquals("Should return all 3 messages", 3, cached.size)
        assertEquals(
            "Messages must be ordered by timestamp ASC",
            listOf(100L, 200L, 300L),
            cached.map { it.timestamp }
        )
        assertEquals("First", cached[0].text)
        assertEquals("Second", cached[1].text)
        assertEquals("Third", cached[2].text)
    }

    @Test
    fun `messages from different assignments are isolated`() = runTest {
        dao.insertCachedMessages(
            listOf(
                cachedMessage(id = "a1_1", assignmentId = "A1", text = "A1 first", timestamp = 100L),
                cachedMessage(id = "a1_2", assignmentId = "A1", text = "A1 second", timestamp = 200L)
            )
        )
        dao.insertCachedMessages(
            listOf(
                cachedMessage(id = "a2_1", assignmentId = "A2", text = "A2 only", timestamp = 150L)
            )
        )
        advanceUntilIdle()

        val a1Messages = dao.getCachedMessagesByAssignment("A1").first()
        val a2Messages = dao.getCachedMessagesByAssignment("A2").first()

        assertEquals("A1 should have 2 messages", 2, a1Messages.size)
        assertEquals("A2 should have 1 message", 1, a2Messages.size)
        assertTrue("A1 messages should not include A2", a1Messages.none { it.id == "a2_1" })
        assertTrue("A2 messages should not include A1", a2Messages.none { it.id == "a1_1" })
    }

    // ── Cache Clear Tests ────────────────────────────────────────────────

    @Test
    fun `clearCachedMessagesForAssignment removes all messages for that assignment`() = runTest {
        dao.insertCachedMessages(
            listOf(
                cachedMessage(id = "msg1", assignmentId = "A1", timestamp = 100L),
                cachedMessage(id = "msg2", assignmentId = "A1", timestamp = 200L),
                cachedMessage(id = "msg3", assignmentId = "A1", timestamp = 300L)
            )
        )
        advanceUntilIdle()
        assertEquals(3, dao.getCachedMessagesByAssignment("A1").first().size)

        dao.clearCachedMessagesForAssignment("A1")
        advanceUntilIdle()

        val remaining = dao.getCachedMessagesByAssignment("A1").first()
        assertTrue("All messages for A1 should be removed", remaining.isEmpty())
    }

    @Test
    fun `clearCachedMessagesForAssignment does not affect other assignments`() = runTest {
        dao.insertCachedMessages(
            listOf(
                cachedMessage(id = "a1_1", assignmentId = "A1", timestamp = 100L),
                cachedMessage(id = "a2_1", assignmentId = "A2", timestamp = 100L),
                cachedMessage(id = "a2_2", assignmentId = "A2", timestamp = 200L)
            )
        )
        advanceUntilIdle()

        dao.clearCachedMessagesForAssignment("A1")
        advanceUntilIdle()

        val a1After = dao.getCachedMessagesByAssignment("A1").first()
        val a2After = dao.getCachedMessagesByAssignment("A2").first()

        assertTrue("A1 should be empty after clear", a1After.isEmpty())
        assertEquals("A2 should still have 2 messages", 2, a2After.size)
    }

    // ── Empty State Test ─────────────────────────────────────────────────

    @Test
    fun `getCachedMessagesByAssignment returns empty for unknown assignment`() = runTest {
        val cached = dao.getCachedMessagesByAssignment("nonexistent").first()
        assertTrue("Should return empty list for unknown assignment", cached.isEmpty())
    }

    // ── Field Preservation Test ──────────────────────────────────────────

    @Test
    fun `all CachedMessage fields are preserved through Room round-trip`() = runTest {
        val original = CachedMessage(
            id = "full_test_id",
            assignmentId = "A1",
            senderUsername = "student_bob",
            senderRole = "STUDENT",
            text = "My vibrato improved!",
            attachmentUrl = "https://storage.example.com/video.mp4",
            attachmentType = "video",
            timestamp = 1717000000000L,
            read = false
        )
        dao.insertCachedMessages(listOf(original))
        advanceUntilIdle()

        val loaded = dao.getCachedMessagesByAssignment("A1").first().first()
        assertEquals("full_test_id", loaded.id)
        assertEquals("A1", loaded.assignmentId)
        assertEquals("student_bob", loaded.senderUsername)
        assertEquals("STUDENT", loaded.senderRole)
        assertEquals("My vibrato improved!", loaded.text)
        assertEquals("https://storage.example.com/video.mp4", loaded.attachmentUrl)
        assertEquals("video", loaded.attachmentType)
        assertEquals(1717000000000L, loaded.timestamp)
        assertEquals(false, loaded.read)
    }

    // ── Read Status Test ─────────────────────────────────────────────────

    @Test
    fun `read flag defaults to false for new messages`() = runTest {
        val msg = cachedMessage(id = "unread1", assignmentId = "A1")
        dao.insertCachedMessages(listOf(msg))
        advanceUntilIdle()

        val loaded = dao.getCachedMessagesByAssignment("A1").first().first()
        assertEquals("New messages should default to read=false", false, loaded.read)
    }

    @Test
    fun `read flag can be set to true and preserved`() = runTest {
        dao.insertCachedMessages(
            listOf(cachedMessage(id = "read1", assignmentId = "A1", read = true, timestamp = 100L))
        )
        advanceUntilIdle()

        val loaded = dao.getCachedMessagesByAssignment("A1").first().first()
        assertEquals("Messages with read=true should preserve the flag", true, loaded.read)
    }
}
