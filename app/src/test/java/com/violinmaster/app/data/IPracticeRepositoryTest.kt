package com.violinmaster.app.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
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
 * Verifies that PracticeRepository correctly implements the IPracticeRepository
 * interface contract. All tests use the interface type to prove dependency
 * inversion works.
 *
 * REQ-ARCH-002-S1, REQ-ARCH-002-S3: Repository interfaces for clean architecture.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class IPracticeRepositoryTest {

    private lateinit var database: PracticeDatabase
    private lateinit var repository: IPracticeRepository

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, PracticeDatabase::class.java).build()
        repository = PracticeRepository(database.sessionDao(), database.lessonDao(), database.userDao(), database.assignmentDao())
    }

    @After
    fun tearDown() {
        database.close()
    }

    // --- Session CRUD ---

    @Test
    fun `insertSession and getAllSessions via interface`() = runTest {
        val session = PracticeSession(
            dateString = "2026-06-01",
            durationSeconds = 300,
            category = "Scales"
        )
        repository.insertSession(session)

        val sessions = repository.allSessions.first()
        assertEquals(1, sessions.size)
        assertEquals("Scales", sessions[0].category)
    }

    @Test
    fun `deleteSessionById via interface`() = runTest {
        repository.insertSession(PracticeSession(dateString = "2026-06-01", durationSeconds = 120, category = "Test"))
        val sessions = repository.allSessions.first()
        val id = sessions[0].id

        repository.deleteSession(id)
        val afterDelete = repository.allSessions.first()
        assertTrue(afterDelete.isEmpty())
    }

    @Test
    fun `clearSessions via interface`() = runTest {
        repository.insertSession(PracticeSession(dateString = "2026-06-01", durationSeconds = 60, category = "A"))
        repository.insertSession(PracticeSession(dateString = "2026-06-01", durationSeconds = 90, category = "B"))

        repository.clearSessions()
        val sessions = repository.allSessions.first()
        assertTrue(sessions.isEmpty())
    }

    // --- Lesson CRUD ---

    @Test
    fun `insertLessonProgress and getAllLessonProgress via interface`() = runTest {
        val lesson = LessonProgress("test_lesson", "Test Lesson", "Beginner", false, 0)
        repository.insertLessonProgress(lesson)

        val lessons = repository.allLevelProgress.first()
        assertEquals(1, lessons.size)
        assertEquals("Test Lesson", lessons[0].lessonTitle)
    }

    @Test
    fun `updateLessonCompletion via interface`() = runTest {
        val lesson = LessonProgress("test_update", "Update Lesson", "Beginner", false, 0)
        repository.insertLessonProgress(lesson)

        repository.updateLessonCompletion("test_update", true)
        val updated = repository.getLessonProgressById("test_update")
        assertNotNull(updated)
        assertTrue(updated!!.completed)
    }

    @Test
    fun `getLessonProgressById returns null for missing`() = runTest {
        val result = repository.getLessonProgressById("nonexistent")
        assertNull(result)
    }

    // --- User CRUD ---

    @Test
    fun `insertUser and getUserByUsername via interface`() = runTest {
        val user = UserAccount(
            username = "test_user",
            role = "STUDENT",
            hashedPassword = "hash",
            salt = "salt"
        )
        repository.insertUser(user)

        val found = repository.getUserByUsername("test_user")
        assertNotNull(found)
        assertEquals("test_user", found!!.username)
    }

    @Test
    fun `getAllUsers via interface`() = runTest {
        repository.insertUser(UserAccount(username = "u1", role = "STUDENT", hashedPassword = "h1", salt = "s1"))
        repository.insertUser(UserAccount(username = "u2", role = "TEACHER", hashedPassword = "h2", salt = "s2"))

        val users = repository.allUsers.first()
        assertEquals(2, users.size)
    }

    // --- Assignment CRUD ---

    @Test
    fun `insertAssignment and getAllAssignments via interface`() = runTest {
        val assignment = Assignment(
            title = "Test Assignment",
            description = "Test Desc",
            teacherUsername = "teacher1",
            studentUsername = "student1"
        )
        repository.insertAssignment(assignment)

        val assignments = repository.allAssignments.first()
        assertEquals(1, assignments.size)
        assertEquals("Test Assignment", assignments[0].title)
    }

    @Test
    fun `getAssignmentsForStudent via interface`() = runTest {
        repository.insertAssignment(Assignment(title = "For Alice", description = "", teacherUsername = "T1", studentUsername = "alice"))
        repository.insertAssignment(Assignment(title = "For Bob", description = "", teacherUsername = "T1", studentUsername = "bob"))

        val aliceAssignments = repository.getAssignmentsForStudent("alice").first()
        assertEquals(1, aliceAssignments.size)
        assertEquals("For Alice", aliceAssignments[0].title)
    }

    @Test
    fun `getAssignmentsByTeacher via interface`() = runTest {
        repository.insertAssignment(Assignment(title = "T1 Task", description = "", teacherUsername = "TEACH-001", studentUsername = "ALL"))
        repository.insertAssignment(Assignment(title = "T2 Task", description = "", teacherUsername = "TEACH-002", studentUsername = "ALL"))

        val teacher1Assignments = repository.getAssignmentsByTeacher("TEACH-001").first()
        assertEquals(1, teacher1Assignments.size)
        assertEquals("T1 Task", teacher1Assignments[0].title)
    }

    @Test
    fun `updateAssignmentCompletion via interface`() = runTest {
        repository.insertAssignment(Assignment(title = "Complete Me", description = "", teacherUsername = "T", studentUsername = "S"))
        val all = repository.allAssignments.first()
        val id = all[0].id

        repository.updateAssignmentCompletion(id, true)
        val updated = repository.allAssignments.first()
        assertTrue(updated[0].completed)
    }

    @Test
    fun `deleteAssignmentById via interface`() = runTest {
        repository.insertAssignment(Assignment(title = "Delete Me", description = "", teacherUsername = "T", studentUsername = "S"))
        val all = repository.allAssignments.first()
        val id = all[0].id

        repository.deleteAssignmentById(id)
        val after = repository.allAssignments.first()
        assertTrue(after.isEmpty())
    }
}
