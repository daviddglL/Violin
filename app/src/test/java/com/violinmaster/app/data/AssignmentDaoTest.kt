package com.violinmaster.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * DAO tests for AssignmentDao covering all CRUD operations.
 *
 * Uses a minimal in-memory database with only [Assignment] entity.
 * REQ-ARCH-004-S1: AssignmentDao manages student_assignments table.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AssignmentDaoTest {

    @Database(entities = [Assignment::class], version = 1)
    abstract class TestAssignmentDatabase : RoomDatabase() {
        abstract fun assignmentDao(): AssignmentDao
    }

    private lateinit var database: TestAssignmentDatabase
    private lateinit var dao: AssignmentDao

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, TestAssignmentDatabase::class.java)
            .build()
        dao = database.assignmentDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    // ── Insert + Query All ──────────────────────────────────────────────

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
        assertFalse(all[0].completed)
    }

    // ── Update ──────────────────────────────────────────────────────────

    @Test
    fun `updateAssignment modifies an existing assignment`() = runTest {
        val original = Assignment(
            title = "Old Title",
            description = "Original description.",
            teacherUsername = "teacher_jane",
            studentUsername = "student_bob"
        )
        dao.insertAssignment(original)
        advanceUntilIdle()

        val inserted = dao.getAllAssignments().first().first()
        val updated = inserted.copy(title = "New Title", completed = true)
        dao.updateAssignment(updated)
        advanceUntilIdle()

        val result = dao.getAllAssignments().first().first()
        assertEquals("New Title", result.title)
        assertTrue(result.completed)
    }

    // ── Mark Completed ──────────────────────────────────────────────────

    @Test
    fun `markAssignmentCompleted sets completed flag`() = runTest {
        val assignment = Assignment(
            title = "Scale Practice",
            description = "Play D major scale with metronome.",
            teacherUsername = "teacher_jane",
            studentUsername = "student_bob"
        )
        dao.insertAssignment(assignment)
        advanceUntilIdle()

        val inserted = dao.getAllAssignments().first().first()
        dao.markAssignmentCompleted(inserted.id, true)
        advanceUntilIdle()

        val afterUpdate = dao.getAllAssignments().first().first()
        assertTrue(afterUpdate.completed)
    }

    @Test
    fun `markAssignmentCompleted sets completed to false`() = runTest {
        val assignment = Assignment(
            title = "Unmark me",
            description = "Should be unmarked.",
            teacherUsername = "teacher_jane",
            studentUsername = "student_bob",
            completed = true
        )
        dao.insertAssignment(assignment)
        advanceUntilIdle()

        val inserted = dao.getAllAssignments().first().first()
        dao.markAssignmentCompleted(inserted.id, false)
        advanceUntilIdle()

        val afterUpdate = dao.getAllAssignments().first().first()
        assertFalse(afterUpdate.completed)
    }

    // ── Delete ──────────────────────────────────────────────────────────

    @Test
    fun `deleteAssignment removes the assignment`() = runTest {
        val assignment = Assignment(
            title = "Temp Task",
            description = "Delete me.",
            teacherUsername = "teacher_jane",
            studentUsername = "student_bob"
        )
        dao.insertAssignment(assignment)
        advanceUntilIdle()

        val inserted = dao.getAllAssignments().first().first()
        dao.deleteAssignment(inserted)
        advanceUntilIdle()

        assertTrue(dao.getAllAssignments().first().isEmpty())
    }

    // ── Student Filter ──────────────────────────────────────────────────

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
    fun `getAssignmentsForStudent returns empty for no matches`() = runTest {
        dao.insertAssignment(
            Assignment(
                title = "Alice Only",
                description = "For Alice.",
                teacherUsername = "teacher_jane",
                studentUsername = "student_alice"
            )
        )
        advanceUntilIdle()

        val result = dao.getAssignmentsForStudent("student_bob").first()
        assertTrue(result.isEmpty())
    }

    // ── Teacher Filter ──────────────────────────────────────────────────

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

    // ── Data Integrity ──────────────────────────────────────────────────

    @Test
    fun `assignment auto-generates unique IDs`() = runTest {
        dao.insertAssignment(
            Assignment(title = "Task A", description = "First", teacherUsername = "t", studentUsername = "s")
        )
        dao.insertAssignment(
            Assignment(title = "Task B", description = "Second", teacherUsername = "t", studentUsername = "s")
        )
        advanceUntilIdle()

        val assignments = dao.getAllAssignments().first()
        assertEquals(2, assignments.size)
        assertTrue(assignments[0].id != assignments[1].id)
    }

    @Test
    fun `getAllAssignments returns empty for empty table`() = runTest {
        val assignments = dao.getAllAssignments().first()
        assertTrue(assignments.isEmpty())
    }
}
