package com.violinmaster.app.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.violinmaster.app.data.firebase.AssignmentDoc
import com.violinmaster.app.data.firebase.AssignmentSyncRepository
import com.violinmaster.app.data.firebase.FakeFirestoreCollection
import com.violinmaster.app.data.firebase.LessonDoc
import com.violinmaster.app.data.firebase.LessonSyncRepository
import com.violinmaster.app.data.firebase.SessionDoc
import com.violinmaster.app.data.firebase.SessionSyncRepository
import com.violinmaster.app.data.firebase.UserDoc
import com.violinmaster.app.data.firebase.UserSyncRepository
import kotlinx.coroutines.flow.first
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
 * Verifies GDPR deletion infrastructure and firebaseUid lookup methods
 * on the [IPracticeRepository] interface.
 *
 * REQ-GD-003: Cascading deletion support.
 * REQ-GS-004: Google account link via firebaseUid.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class IPracticeRepositoryDeletionTest {

    private lateinit var database: PracticeDatabase
    private lateinit var repository: IPracticeRepository
    private lateinit var lessonDao: LessonDao
    private lateinit var assignmentDao: AssignmentDao
    private lateinit var chatDao: ChatDao
    private lateinit var sessionDao: SessionDao
    private lateinit var userDao: UserDao

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, PracticeDatabase::class.java).build()
        val sessionSync = SessionSyncRepository(FakeFirestoreCollection<SessionDoc>(), database.sessionDao())
        val lessonSync = LessonSyncRepository(FakeFirestoreCollection<LessonDoc>(), database.lessonDao())
        val userSync = UserSyncRepository(FakeFirestoreCollection<UserDoc>(), database.userDao())
        val assignmentSync = AssignmentSyncRepository(FakeFirestoreCollection<AssignmentDoc>(), database.assignmentDao())
        repository = PracticeRepository(
            sessionSync = sessionSync,
            lessonSync = lessonSync,
            userSync = userSync,
            assignmentSync = assignmentSync,
            sessionDao = database.sessionDao(),
            lessonDao = database.lessonDao(),
            userDao = database.userDao(),
            assignmentDao = database.assignmentDao(),
            cloudConfig = CloudConfig()
        )
        lessonDao = database.lessonDao()
        assignmentDao = database.assignmentDao()
        chatDao = database.chatDao()
        sessionDao = database.sessionDao()
        userDao = database.userDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    // ── firebaseUid lookup ─────────────────────────────────────────────────

    @Test
    fun `getUserByFirebaseUid finds user by firebaseUid`() = runTest {
        val user = UserAccount(
            username = "google_user",
            role = "FREELANCER",
            hashedPassword = "hash",
            salt = "salt",
            firebaseUid = "abc123"
        )
        repository.insertUser(user)

        val found = repository.getUserByFirebaseUid("abc123")
        assertNotNull(found)
        assertEquals("google_user", found!!.username)
    }

    @Test
    fun `getUserByFirebaseUid returns null for unknown uid`() = runTest {
        val result = repository.getUserByFirebaseUid("nonexistent")
        assertNull(result)
    }

    // ── updateFirebaseUid ──────────────────────────────────────────────────

    @Test
    fun `updateFirebaseUid links Google account to existing user`() = runTest {
        val user = UserAccount(
            username = "pin_user",
            role = "STUDENT",
            hashedPassword = "hash",
            salt = "salt",
            firebaseUid = null
        )
        repository.insertUser(user)

        repository.updateFirebaseUid("pin_user", "new_firebase_uid")

        val updated = repository.getUserByUsername("pin_user")
        assertNotNull(updated)
        assertEquals("new_firebase_uid", updated!!.firebaseUid)
    }

    // ── deleteAllUserData ──────────────────────────────────────────────────

    @Test
    fun `deleteAllUserData clears all Room tables`() = runTest {
        // Populate data
        val user = UserAccount(username = "todelete", role = "STUDENT", hashedPassword = "h", salt = "s")
        repository.insertUser(user)
        sessionDao.insertSession(PracticeSession(dateString = "2026-06-01", durationSeconds = 60, category = "Test"))
        lessonDao.insertLessonProgress(LessonProgress("L1", "Lesson 1", "Beginner", false, 0))
        assignmentDao.insertAssignment(Assignment(title = "A1", description = "", teacherUsername = "T", studentUsername = "todelete"))

        // Delete user first, then wipe remaining tables
        repository.deleteAllUserData(user)

        val usersAfter = userDao.getAllUsers().first()
        val sessionsAfter = sessionDao.getAllSessions().first()
        val lessonsAfter = lessonDao.getAllLessons().first()
        val assignmentsAfter = assignmentDao.getAllAssignments().first()

        assertTrue("Users should be cleared", usersAfter.isEmpty())
        assertTrue("Sessions should be cleared", sessionsAfter.isEmpty())
        assertTrue("Lessons should be cleared", lessonsAfter.isEmpty())
        assertTrue("Assignments should be cleared", assignmentsAfter.isEmpty())
    }

    @Test
    fun `wipeAllTables clears all non-user tables`() = runTest {
        sessionDao.insertSession(PracticeSession(dateString = "2026-06-01", durationSeconds = 60, category = "Test"))
        lessonDao.insertLessonProgress(LessonProgress("L1", "Lesson 1", "Beginner", false, 0))
        assignmentDao.insertAssignment(Assignment(title = "A1", description = "", teacherUsername = "T", studentUsername = "S"))
        val msg = com.violinmaster.app.data.local.CachedMessage(
            id = "m1", assignmentId = "A1", senderUsername = "T", senderRole = "TEACHER",
            text = "Hello", timestamp = System.currentTimeMillis()
        )
        chatDao.insertCachedMessages(listOf(msg))

        database.wipeAllTables()

        val sessionsAfter = sessionDao.getAllSessions().first()
        val lessonsAfter = lessonDao.getAllLessons().first()
        val assignmentsAfter = assignmentDao.getAllAssignments().first()
        val messagesAfter = chatDao.getCachedMessagesByAssignment("A1").first()

        assertTrue("Sessions should be cleared", sessionsAfter.isEmpty())
        assertTrue("Lessons should be cleared", lessonsAfter.isEmpty())
        assertTrue("Assignments should be cleared", assignmentsAfter.isEmpty())
        assertTrue("Chat messages should be cleared", messagesAfter.isEmpty())
    }

    @Test
    fun `clearAllLessons DAO method removes all lesson_progress rows`() = runTest {
        lessonDao.insertLessonProgress(LessonProgress("L1", "Lesson 1", "Beginner", false, 0))
        lessonDao.insertLessonProgress(LessonProgress("L2", "Lesson 2", "Advanced", false, 0))

        lessonDao.clearAllLessons()
        val lessons = lessonDao.getAllLessons().first()
        assertTrue(lessons.isEmpty())
    }

    @Test
    fun `clearAllAssignments DAO method removes all student_assignments rows`() = runTest {
        assignmentDao.insertAssignment(Assignment(title = "A1", description = "", teacherUsername = "T", studentUsername = "S"))
        assignmentDao.insertAssignment(Assignment(title = "A2", description = "", teacherUsername = "T", studentUsername = "S"))

        assignmentDao.clearAllAssignments()
        val assignments = assignmentDao.getAllAssignments().first()
        assertTrue(assignments.isEmpty())
    }

    @Test
    fun `clearAllCachedMessages DAO method removes all cached_messages rows`() = runTest {
        val msg1 = com.violinmaster.app.data.local.CachedMessage(
            id = "msg1", assignmentId = "A", senderUsername = "T", senderRole = "TEACHER",
            text = "Hello", timestamp = System.currentTimeMillis()
        )
        val msg2 = com.violinmaster.app.data.local.CachedMessage(
            id = "msg2", assignmentId = "A", senderUsername = "S", senderRole = "STUDENT",
            text = "Hi", timestamp = System.currentTimeMillis()
        )
        chatDao.insertCachedMessages(listOf(msg1, msg2))

        chatDao.clearAllCachedMessages()
        val messages = chatDao.getCachedMessagesByAssignment("A").first()
        assertTrue(messages.isEmpty())
    }
}
