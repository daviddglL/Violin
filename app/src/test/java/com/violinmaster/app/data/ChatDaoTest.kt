package com.violinmaster.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.test.core.app.ApplicationProvider
import com.violinmaster.app.data.local.CachedMessage
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * DAO tests for ChatDao covering cached message operations.
 *
 * Uses a minimal in-memory database with only [CachedMessage] entity.
 * REQ-ARCH-004-S4: ChatDao manages cached_messages table.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ChatDaoTest {

    @Database(entities = [CachedMessage::class], version = 1)
    abstract class TestChatDatabase : RoomDatabase() {
        abstract fun chatDao(): ChatDao
    }

    private lateinit var database: TestChatDatabase
    private lateinit var dao: ChatDao

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, TestChatDatabase::class.java)
            .build()
        dao = database.chatDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    // ── Helper ─────────────────────────────────────────────────────────

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

    // ── Insert + Query ──────────────────────────────────────────────────

    @Test
    fun `insertCachedMessages then getCachedMessagesByAssignment returns the messages`() = runTest {
        val messages = listOf(
            cachedMessage(id = "msg1", assignmentId = "A1", text = "Hello", timestamp = 100L),
            cachedMessage(id = "msg2", assignmentId = "A1", text = "World", timestamp = 200L)
        )
        dao.insertCachedMessages(messages)
        advanceUntilIdle()

        val cached = dao.getCachedMessagesByAssignment("A1").first()
        assertEquals(2, cached.size)
        assertEquals("msg1", cached[0].id)
        assertEquals("msg2", cached[1].id)
    }

    // ── Ordering ────────────────────────────────────────────────────────

    @Test
    fun `messages are ordered by timestamp ascending`() = runTest {
        dao.insertCachedMessages(
            listOf(
                cachedMessage(id = "m3", assignmentId = "A1", text = "Third", timestamp = 300L),
                cachedMessage(id = "m1", assignmentId = "A1", text = "First", timestamp = 100L),
                cachedMessage(id = "m2", assignmentId = "A1", text = "Second", timestamp = 200L)
            )
        )
        advanceUntilIdle()

        val cached = dao.getCachedMessagesByAssignment("A1").first()
        assertEquals(3, cached.size)
        assertEquals(listOf(100L, 200L, 300L), cached.map { it.timestamp })
    }

    // ── Assignment Isolation ────────────────────────────────────────────

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

        assertEquals(2, a1Messages.size)
        assertEquals(1, a2Messages.size)
        assertTrue(a1Messages.none { it.id == "a2_1" })
        assertTrue(a2Messages.none { it.id == "a1_1" })
    }

    // ── Clear ───────────────────────────────────────────────────────────

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
        assertTrue(remaining.isEmpty())
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

        assertTrue(a1After.isEmpty())
        assertEquals(2, a2After.size)
    }

    // ── Empty State ─────────────────────────────────────────────────────

    @Test
    fun `getCachedMessagesByAssignment returns empty for unknown assignment`() = runTest {
        val cached = dao.getCachedMessagesByAssignment("nonexistent").first()
        assertTrue(cached.isEmpty())
    }

    // ── Field Preservation ──────────────────────────────────────────────

    @Test
    fun `all CachedMessage fields are preserved through round-trip`() = runTest {
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

    // ── REPLACE on Conflict ─────────────────────────────────────────────

    @Test
    fun `insertCachedMessages replaces on conflict`() = runTest {
        dao.insertCachedMessages(
            listOf(cachedMessage(id = "msg1", assignmentId = "A1", text = "Original", timestamp = 100L))
        )
        advanceUntilIdle()

        dao.insertCachedMessages(
            listOf(cachedMessage(id = "msg1", assignmentId = "A1", text = "Updated", timestamp = 100L))
        )
        advanceUntilIdle()

        val cached = dao.getCachedMessagesByAssignment("A1").first()
        assertEquals(1, cached.size)
        assertEquals("Updated", cached[0].text)
    }

    // ── Read Flag ───────────────────────────────────────────────────────

    @Test
    fun `read flag defaults to false`() = runTest {
        dao.insertCachedMessages(
            listOf(cachedMessage(id = "unread1", assignmentId = "A1"))
        )
        advanceUntilIdle()

        val loaded = dao.getCachedMessagesByAssignment("A1").first().first()
        assertEquals(false, loaded.read)
    }
}
