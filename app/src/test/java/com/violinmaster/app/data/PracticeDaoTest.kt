package com.violinmaster.app.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.advanceUntilIdle
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
 * DAO tests for PracticeDao covering all CRUD operations.
 *
 * REQ-TST-002: All DAO CRUD operations tested with in-memory Room database.
 * Uses the pattern established by existing ViewModel tests (AuthViewModelTest, etc.).
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class PracticeDaoTest {

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

    // ── Practice Sessions ──────────────────────────────────────────────

    @Test
    fun `insertSession then getAllSessions returns the session`() = runTest {
        val session = PracticeSession(
            dateString = "2026-05-27",
            durationSeconds = 1800,
            category = "Smart Tuner",
            timestamp = 1716840000000
        )
        dao.insertSession(session)
        advanceUntilIdle()

        val sessions = dao.getAllSessions().first()
        assertEquals(1, sessions.size)
        assertEquals("2026-05-27", sessions[0].dateString)
        assertEquals(1800, sessions[0].durationSeconds)
        assertEquals("Smart Tuner", sessions[0].category)
    }

    @Test
    fun `deleteSessionById removes the session`() = runTest {
        val session = PracticeSession(
            dateString = "2026-05-27",
            durationSeconds = 600,
            category = "Metronome",
            timestamp = 1716840000000
        )
        dao.insertSession(session)
        advanceUntilIdle()

        val inserted = dao.getAllSessions().first().first()

        dao.deleteSessionById(inserted.id)
        advanceUntilIdle()

        val afterDelete = dao.getAllSessions().first()
        assertTrue(afterDelete.isEmpty())
    }

    @Test
    fun `clearAllSessions empties the table`() = runTest {
        repeat(3) { i ->
            dao.insertSession(
                PracticeSession(
                    dateString = "2026-05-0${i + 1}",
                    durationSeconds = 100 * (i + 1),
                    category = "Test",
                    timestamp = 1716840000000 + i * 86400000
                )
            )
        }
        advanceUntilIdle()

        // Verify 3 inserted
        assertEquals(3, dao.getAllSessions().first().size)

        dao.clearAllSessions()
        advanceUntilIdle()

        assertTrue(dao.getAllSessions().first().isEmpty())
    }

    @Test
    fun `getSessionsByDate returns only matching sessions`() = runTest {
        dao.insertSession(
            PracticeSession(
                dateString = "2026-05-27",
                durationSeconds = 300,
                category = "Morning",
                timestamp = 1L
            )
        )
        dao.insertSession(
            PracticeSession(
                dateString = "2026-05-28",
                durationSeconds = 600,
                category = "Evening",
                timestamp = 2L
            )
        )
        advanceUntilIdle()

        val may27 = dao.getSessionsByDate("2026-05-27").first()
        assertEquals(1, may27.size)
        assertEquals("Morning", may27[0].category)

        val may28 = dao.getSessionsByDate("2026-05-28").first()
        assertEquals(1, may28.size)
        assertEquals("Evening", may28[0].category)
    }

    // ── Lesson Progress ────────────────────────────────────────────────

    @Test
    fun `insertLessonProgress then getAllLessonProgress returns it`() = runTest {
        val progress = LessonProgress(
            lessonId = "mod_1",
            lessonTitle = "Intro to Bowing",
            difficulty = "Beginner",
            completed = false,
            totalPracticedSeconds = 1200,
            lastPracticedTimestamp = 1716840000000
        )
        dao.insertLessonProgress(progress)
        advanceUntilIdle()

        val all = dao.getAllLessonProgress().first()
        assertEquals(1, all.size)
        assertEquals("mod_1", all[0].lessonId)
        assertEquals("Intro to Bowing", all[0].lessonTitle)
    }

    @Test
    fun `updateLessonCompletion marks lesson as complete`() = runTest {
        dao.insertLessonProgress(
            LessonProgress(
                lessonId = "mod_2",
                lessonTitle = "Vibrato",
                difficulty = "Intermediate",
                completed = false
            )
        )
        advanceUntilIdle()

        dao.updateLessonCompletion("mod_2", true)
        advanceUntilIdle()

        val updated = dao.getLessonProgressById("mod_2")
        assertNotNull(updated)
        assertTrue(updated!!.completed)
    }

    @Test
    fun `getLessonProgressById returns null for non-existent lesson`() = runTest {
        val result = dao.getLessonProgressById("nonexistent")
        assertNull(result)
    }

    // ── User Accounts ──────────────────────────────────────────────────

    @Test
    fun `insertUser then getUserByUsername returns the user`() = runTest {
        val user = UserAccount(
            username = "teacher_jane",
            role = "TEACHER",
            hashedPassword = "abc123hash",
            salt = "salt123",
            teacherCode = "TEACH-0001",
            points = 150,
            skillLevel = "Advanced"
        )
        dao.insertUser(user)
        advanceUntilIdle()

        val retrieved = dao.getUserByUsername("teacher_jane")
        assertNotNull(retrieved)
        assertEquals("TEACHER", retrieved!!.role)
        assertEquals("TEACH-0001", retrieved.teacherCode)
        assertEquals(150, retrieved.points)
        assertEquals("Advanced", retrieved.skillLevel)
    }

    @Test
    fun `getUserByUsername returns null for unknown user`() = runTest {
        val retrieved = dao.getUserByUsername("ghost_user")
        assertNull(retrieved)
    }

    @Test
    fun `insertUser overwrites on conflict`() = runTest {
        val original = UserAccount(
            username = "student_bob",
            role = "STUDENT",
            hashedPassword = "hash1",
            salt = "salt1",
            points = 50
        )
        dao.insertUser(original)
        advanceUntilIdle()

        val updated = UserAccount(
            username = "student_bob",
            role = "STUDENT",
            hashedPassword = "hash1",
            salt = "salt1",
            points = 100
        )
        dao.insertUser(updated)
        advanceUntilIdle()

        val retrieved = dao.getUserByUsername("student_bob")
        assertNotNull(retrieved)
        assertEquals(100, retrieved!!.points)
    }

    @Test
    fun `getAllUsers returns users ordered by username`() = runTest {
        dao.insertUser(
            UserAccount(username = "zoe", role = "STUDENT", hashedPassword = "h", salt = "s")
        )
        dao.insertUser(
            UserAccount(username = "alice", role = "STUDENT", hashedPassword = "h", salt = "s")
        )
        advanceUntilIdle()

        val users = dao.getAllUsers().first()
        assertEquals(2, users.size)
        assertEquals("alice", users[0].username)
        assertEquals("zoe", users[1].username)
    }

    // ── Assignments ────────────────────────────────────────────────────

    @Test
    fun `insertAssignment then getAllAssignments returns it`() = runTest {
        val assignment = Assignment(
            title = "Vibrato Drill",
            description = "Practice slow vibrato on G string for 5 minutes.",
            teacherUsername = "teacher_jane",
            studentUsername = "student_bob",
            videoTitle = "Vibrato Demo",
            videoDurationSeconds = 60,
            timestamp = 1716840000000
        )
        dao.insertAssignment(assignment)
        advanceUntilIdle()

        val all = dao.getAllAssignments().first()
        assertEquals(1, all.size)
        assertEquals("Vibrato Drill", all[0].title)
        assertEquals("teacher_jane", all[0].teacherUsername)
        assertTrue(!all[0].completed)
    }

    @Test
    fun `updateAssignmentCompletion marks as complete`() = runTest {
        val assignment = Assignment(
            title = "Scale Practice",
            description = "Play D major scale with metronome.",
            teacherUsername = "teacher_jane",
            studentUsername = "student_bob"
        )
        dao.insertAssignment(assignment)
        advanceUntilIdle()

        val inserted = dao.getAllAssignments().first().first()
        dao.updateAssignmentCompletion(inserted.id, true)
        advanceUntilIdle()

        val afterUpdate = dao.getAllAssignments().first().first()
        assertTrue(afterUpdate.completed)
    }

    @Test
    fun `deleteAssignmentById removes the assignment`() = runTest {
        val assignment = Assignment(
            title = "Temp Task",
            description = "Delete me.",
            teacherUsername = "teacher_jane",
            studentUsername = "student_bob"
        )
        dao.insertAssignment(assignment)
        advanceUntilIdle()

        val inserted = dao.getAllAssignments().first().first()
        dao.deleteAssignmentById(inserted.id)
        advanceUntilIdle()

        assertTrue(dao.getAllAssignments().first().isEmpty())
    }

    @Test
    fun `getAssignmentsForStudent returns only matching student`() = runTest {
        dao.insertAssignment(
            Assignment(
                title = "Bob's Task",
                description = "Only for Bob.",
                teacherUsername = "teacher_jane",
                studentUsername = "student_bob"
            )
        )
        dao.insertAssignment(
            Assignment(
                title = "Alice's Task",
                description = "Only for Alice.",
                teacherUsername = "teacher_jane",
                studentUsername = "student_alice"
            )
        )
        dao.insertAssignment(
            Assignment(
                title = "All Students Task",
                description = "For everyone.",
                teacherUsername = "teacher_jane",
                studentUsername = "ALL"
            )
        )
        advanceUntilIdle()

        val bobAssignments = dao.getAssignmentsForStudent("student_bob").first()
        assertEquals(2, bobAssignments.size)
        assertTrue(bobAssignments.any { it.studentUsername == "student_bob" })
        assertTrue(bobAssignments.any { it.studentUsername == "ALL" })
    }

    @Test
    fun `getAssignmentsByTeacher returns only matching teacher`() = runTest {
        dao.insertAssignment(
            Assignment(
                title = "Jane's Task",
                description = "From Jane.",
                teacherUsername = "teacher_jane",
                studentUsername = "student_bob"
            )
        )
        dao.insertAssignment(
            Assignment(
                title = "John's Task",
                description = "From John.",
                teacherUsername = "teacher_john",
                studentUsername = "student_bob"
            )
        )
        advanceUntilIdle()

        val janeAssignments = dao.getAssignmentsByTeacher("teacher_jane").first()
        assertEquals(1, janeAssignments.size)
        assertEquals("Jane's Task", janeAssignments[0].title)
    }

    // ── Data Integrity ─────────────────────────────────────────────────

    @Test
    fun `session auto-generates unique IDs`() = runTest {
        dao.insertSession(
            PracticeSession(dateString = "2026-05-27", durationSeconds = 100, category = "A")
        )
        dao.insertSession(
            PracticeSession(dateString = "2026-05-27", durationSeconds = 200, category = "B")
        )
        advanceUntilIdle()

        val sessions = dao.getAllSessions().first()
        assertEquals(2, sessions.size)
        // Auto-generated IDs should differ
        assertTrue(sessions[0].id != sessions[1].id)
    }
}
