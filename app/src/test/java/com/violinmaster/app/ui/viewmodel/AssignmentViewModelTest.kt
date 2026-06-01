package com.violinmaster.app.ui.viewmodel

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.violinmaster.app.data.Assignment
import com.violinmaster.app.data.PracticeDao
import com.violinmaster.app.data.PracticeDatabase
import com.violinmaster.app.data.IPracticeRepository
import com.violinmaster.app.data.PracticeRepository
import com.violinmaster.app.data.UserAccount
import com.violinmaster.app.di.AuthManager
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

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AssignmentViewModelTest {

    private lateinit var database: PracticeDatabase
    private lateinit var dao: PracticeDao
    private lateinit var repository: IPracticeRepository
    private lateinit var authManager: AuthManager
    private lateinit var viewModel: AssignmentViewModel

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, PracticeDatabase::class.java).build()
        dao = database.practiceDao()
        repository = PracticeRepository(database.sessionDao(), database.lessonDao(), database.userDao(), database.assignmentDao())
        authManager = AuthManager(context)
        viewModel = AssignmentViewModel(repository, authManager)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `publishAssignment creates assignment for teacher`() = runTest {
        // Arrange: login as teacher
        val teacher = UserAccount(
            username = "prof_mozart",
            role = "TEACHER",
            hashedPassword = "hash",
            salt = "salt",
            teacherCode = "TEACH-5678"
        )
        repository.insertUser(teacher)
        advanceUntilIdle()
        authManager.restoreCurrentUser(teacher)

        // Act
        viewModel.publishAssignment(
            title = "Vibrato Practice",
            description = "Slow wave vibrato on D string",
            targetStudent = "ALL",
            videoTitle = "Vibrato Demo",
            durationSeconds = 120
        )
        advanceUntilIdle()

        // Assert: an assignment was created
        val assignments = repository.allAssignments.first()
        assertEquals(1, assignments.size)
        assertEquals("Vibrato Practice", assignments[0].title)
        assertEquals("TEACH-5678", assignments[0].teacherUsername)
        assertEquals("ALL", assignments[0].studentUsername)
        assertEquals("Vibrato Demo", assignments[0].videoTitle)
        assertEquals(120, assignments[0].videoDurationSeconds)
    }

    @Test
    fun `markAssignmentComplete marks it done and awards 200 points`() = runTest {
        // Arrange: teacher + student + assignment
        val teacher = UserAccount(
            username = "prof_bach",
            role = "TEACHER",
            hashedPassword = "hash",
            salt = "salt",
            teacherCode = "TEACH-9999"
        )
        val student = UserAccount(
            username = "student_bach",
            role = "STUDENT",
            hashedPassword = "hash",
            salt = "salt",
            teacherCode = "TEACH-9999",
            points = 0
        )
        repository.insertUser(teacher)
        repository.insertUser(student)
        advanceUntilIdle()

        val assignment = Assignment(
            title = "Scale Run",
            description = "G major 2 octaves",
            teacherUsername = "TEACH-9999",
            studentUsername = "student_bach"
        )
        repository.insertAssignment(assignment)
        advanceUntilIdle()
        val createdAssignment = repository.allAssignments.first()[0]

        // Login as student to earn points
        authManager.restoreCurrentUser(student)

        // Act
        viewModel.markAssignmentComplete(createdAssignment.id, true)
        advanceUntilIdle()

        // Assert: assignment marked complete
        val updatedAssignments = repository.allAssignments.first()
        assertTrue(updatedAssignments[0].completed)

        // Assert: 200 points awarded
        val updatedStudent = repository.getUserByUsername("student_bach")
        assertNotNull(updatedStudent)
        assertEquals(200, updatedStudent!!.points)
    }

    @Test
    fun `deleteAssignment removes it`() = runTest {
        // Arrange
        val teacher = UserAccount(
            username = "prof_vivaldi",
            role = "TEACHER",
            hashedPassword = "hash",
            salt = "salt",
            teacherCode = "TEACH-1111"
        )
        repository.insertUser(teacher)
        advanceUntilIdle()
        authManager.restoreCurrentUser(teacher)

        val assignment = Assignment(
            title = "Delete Me",
            description = "Temporary",
            teacherUsername = "TEACH-1111",
            studentUsername = "ALL"
        )
        repository.insertAssignment(assignment)
        advanceUntilIdle()
        val created = repository.allAssignments.first()[0]

        // Act
        viewModel.deleteAssignment(created.id)
        advanceUntilIdle()

        // Assert
        val afterDelete = repository.allAssignments.first()
        assertTrue(afterDelete.isEmpty())
    }

    @Test
    fun `studentAssignments filters correctly by username`() = runTest {
        // Arrange: teacher and student
        val teacher = UserAccount(
            username = "prof_test",
            role = "TEACHER",
            hashedPassword = "hash",
            salt = "salt",
            teacherCode = "TEACH-2222"
        )
        val student = UserAccount(
            username = "alice",
            role = "STUDENT",
            hashedPassword = "hash",
            salt = "salt",
            teacherCode = "TEACH-2222"
        )
        repository.insertUser(teacher)
        repository.insertUser(student)
        advanceUntilIdle()
        authManager.restoreCurrentUser(student)

        // Create assignments: one for alice, one for ALL students, one for bob
        repository.insertAssignment(
            Assignment(title = "Alice Task", description = "For Alice", teacherUsername = "TEACH-2222", studentUsername = "alice")
        )
        repository.insertAssignment(
            Assignment(title = "All Task", description = "For all", teacherUsername = "TEACH-2222", studentUsername = "ALL")
        )
        repository.insertAssignment(
            Assignment(title = "Bob Task", description = "For Bob", teacherUsername = "TEACH-2222", studentUsername = "bob")
        )
        advanceUntilIdle()

        // Act: assignments should be filtered reactively
        val studentAssignments = viewModel.studentAssignments.value

        // Assert: only alice's assignments (including ALL)
        assertEquals(2, studentAssignments.size)
        val titles = studentAssignments.map { it.title }
        assertTrue(titles.contains("Alice Task"))
        assertTrue(titles.contains("All Task"))
        assertTrue(!titles.contains("Bob Task"))
    }

    @Test
    fun `teacherAssignments loads assignments for teacher`() = runTest {
        // Arrange
        val teacher = UserAccount(
            username = "prof_paganini",
            role = "TEACHER",
            hashedPassword = "hash",
            salt = "salt",
            teacherCode = "TEACH-3333"
        )
        repository.insertUser(teacher)
        advanceUntilIdle()
        authManager.restoreCurrentUser(teacher)

        // Act: create some assignments for this teacher
        viewModel.publishAssignment("Task 1", "Desc 1", "ALL", "", 0)
        viewModel.publishAssignment("Task 2", "Desc 2", "student_x", "", 0)
        advanceUntilIdle()

        // Assert
        val teacherAssignments = viewModel.teacherAssignments.value
        assertEquals(2, teacherAssignments.size)
        val titles = teacherAssignments.map { it.title }
        assertTrue(titles.contains("Task 1"))
        assertTrue(titles.contains("Task 2"))
    }
}
