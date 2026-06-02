package com.violinmaster.app.ui.viewmodel

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.violinmaster.app.audio.ViolinAudioEngine
import com.violinmaster.app.data.LessonProgress
import com.violinmaster.app.data.PracticeDatabase
import com.violinmaster.app.data.IPracticeRepository
import com.violinmaster.app.data.PracticeRepository
import com.violinmaster.app.data.UserAccount
import com.violinmaster.app.di.AuthManager
import com.violinmaster.app.di.UserPreferencesManager
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceTimeBy
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

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class PracticeViewModelTest {

    private lateinit var database: PracticeDatabase
    private lateinit var repository: IPracticeRepository
    private lateinit var authManager: AuthManager
    private lateinit var userPreferencesManager: UserPreferencesManager
    private lateinit var audioEngine: ViolinAudioEngine
    private lateinit var viewModel: PracticeViewModel
    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, PracticeDatabase::class.java).build()
        repository = PracticeRepository(database.sessionDao(), database.lessonDao(), database.userDao(), database.assignmentDao())
        authManager = AuthManager(context)
        userPreferencesManager = UserPreferencesManager(context)
        audioEngine = ViolinAudioEngine()
        viewModel = PracticeViewModel(repository, authManager, userPreferencesManager, audioEngine)
    }

    @After
    fun tearDown() {
        database.close()
    }

    // --- Timer tests ---

    @Test
    fun `startPracticeTimer sets isPracticing true and starts counting`() = runTest {
        viewModel.startPracticeTimer("Smart Tuner")

        assertTrue(viewModel.isPracticing.value)
        assertEquals("Smart Tuner", viewModel.practiceCategoryName.value)
        assertEquals(0, viewModel.practiceElapsedSeconds.value)

        advanceTimeBy(3500) // ~3.5 seconds

        assertEquals(3, viewModel.practiceElapsedSeconds.value)
    }

    @Test
    fun `pausePracticeTimer stops counting but preserves elapsed`() = runTest {
        viewModel.startPracticeTimer("Metronome")
        advanceTimeBy(2500) // 2 seconds elapsed
        assertEquals(2, viewModel.practiceElapsedSeconds.value)

        viewModel.pausePracticeTimer()

        assertFalse(viewModel.isPracticing.value)
        advanceTimeBy(3000) // more time passes while paused
        assertEquals(2, viewModel.practiceElapsedSeconds.value) // elapsed preserved
    }

    @Test
    fun `resumePracticeTimer continues from paused elapsed`() = runTest {
        viewModel.startPracticeTimer("Scales")
        advanceTimeBy(2000) // 2 seconds
        viewModel.pausePracticeTimer()
        advanceTimeBy(1000) // paused, no increment

        viewModel.resumePracticeTimer()
        assertTrue(viewModel.isPracticing.value)
        advanceTimeBy(3500) // 3 more seconds

        assertEquals(5, viewModel.practiceElapsedSeconds.value) // 2 + 3
    }

    @Test
    fun `stopAndSavePracticeSession saves session when duration ge 3 seconds`() = runTest {
        viewModel.startPracticeTimer("Smart Tuner")
        advanceTimeBy(3500) // 3 seconds elapsed
        assertEquals(3, viewModel.practiceElapsedSeconds.value)

        viewModel.stopAndSavePracticeSession()
        advanceUntilIdle()

        assertFalse(viewModel.isPracticing.value)
        assertEquals(0, viewModel.practiceElapsedSeconds.value)

        val sessions = repository.allSessions.first()
        assertEquals(1, sessions.size)
        assertEquals(3, sessions[0].durationSeconds)
        assertEquals("Smart Tuner", sessions[0].category)
    }

    @Test
    fun `stopAndSavePracticeSession does NOT save when duration lt 3 seconds`() = runTest {
        viewModel.startPracticeTimer("Quick Check")
        advanceTimeBy(1500) // only 1 second

        viewModel.stopAndSavePracticeSession()
        advanceUntilIdle()

        val sessions = repository.allSessions.first()
        assertTrue(sessions.isEmpty())
    }

    @Test
    fun `cancelPracticeTimer resets everything to zero`() = runTest {
        viewModel.startPracticeTimer("General")
        advanceTimeBy(5000) // 5 seconds

        viewModel.cancelPracticeTimer()

        assertFalse(viewModel.isPracticing.value)
        assertEquals(0, viewModel.practiceElapsedSeconds.value)
    }

    // --- Configuration tests ---

    @Test
    fun `updateDailyGoal changes the goal value`() = runTest {
        assertEquals(60, viewModel.dailyGoalMinutes.value) // default

        viewModel.updateDailyGoal(120)
        assertEquals(120, viewModel.dailyGoalMinutes.value)
    }

    // --- Daily task tests ---

    @Test
    fun `completeDailyTask awards 100 points on first attempt`() = runTest {
        // Setup a current user so earnPoints works
        val user = UserAccount(
            username = "student1",
            role = "STUDENT",
            hashedPassword = "hash",
            salt = "salt",
            points = 0
        )
        repository.insertUser(user)
        advanceUntilIdle()
        authManager.restoreCurrentUser(user)
        viewModel.loadDailyTasksCompleted()

        viewModel.completeDailyTask("task_scale_practice", 1)
        advanceUntilIdle()

        // Verify task marked completed
        assertTrue(viewModel.dailyTasksCompleted.value.contains("task_scale_practice"))

        // Verify points awarded (100 for attempt 1)
        val updatedUser = repository.getUserByUsername("student1")
        assertNotNull(updatedUser)
        assertEquals(100, updatedUser!!.points)
    }

    @Test
    fun `completeDailyTask awards scaled points based on attempts`() = runTest {
        val user = UserAccount(
            username = "student2",
            role = "STUDENT",
            hashedPassword = "hash",
            salt = "salt",
            points = 50
        )
        repository.insertUser(user)
        advanceUntilIdle()
        authManager.restoreCurrentUser(user)
        viewModel.loadDailyTasksCompleted()

        // 4 attempts = 25 points
        viewModel.completeDailyTask("task_bow_hold", 4)
        advanceUntilIdle()

        val updatedUser = repository.getUserByUsername("student2")
        assertNotNull(updatedUser)
        assertEquals(75, updatedUser!!.points) // 50 + 25
    }

    @Test
    fun `completeDailyTask marks task as completed and does not award points twice`() = runTest {
        val user = UserAccount(
            username = "student3",
            role = "STUDENT",
            hashedPassword = "hash",
            salt = "salt",
            points = 0
        )
        repository.insertUser(user)
        advanceUntilIdle()
        authManager.restoreCurrentUser(user)
        viewModel.loadDailyTasksCompleted()

        // First completion = 100 points
        viewModel.completeDailyTask("task_vibrato", 1)
        advanceUntilIdle()
        assertEquals(100, repository.getUserByUsername("student3")?.points)

        // Second completion of same task = no extra points (already in completed set)
        viewModel.completeDailyTask("task_vibrato", 1)
        advanceUntilIdle()
        assertEquals(100, repository.getUserByUsername("student3")?.points)
    }

    // --- Points and skill tests ---

    @Test
    fun `earnPoints adds points to currentUser`() = runTest {
        val user = UserAccount(
            username = "learner",
            role = "FREELANCER",
            hashedPassword = "hash",
            salt = "salt",
            points = 200
        )
        repository.insertUser(user)
        advanceUntilIdle()
        authManager.restoreCurrentUser(user)

        viewModel.earnPoints(150)
        advanceUntilIdle()

        val updatedUser = repository.getUserByUsername("learner")
        assertNotNull(updatedUser)
        assertEquals(350, updatedUser!!.points)
    }

    @Test
    fun `earnPoints does nothing when no currentUser`() = runTest {
        // No user logged in
        viewModel.earnPoints(500)
        advanceUntilIdle()
        // Should not crash — no assertion needed, just verifying no exception
    }

    // --- Lesson tests ---

    @Test
    fun `toggleLessonStatus marks lesson complete and awards 150 points`() = runTest {
        // Seed a lesson
        val lesson = LessonProgress("test_lesson_1", "Test Lesson", "Beginner", false, 0)
        repository.insertLessonProgress(lesson)
        advanceUntilIdle()

        // Setup a user for points
        val user = UserAccount(
            username = "student_lesson",
            role = "STUDENT",
            hashedPassword = "hash",
            salt = "salt",
            points = 0
        )
        repository.insertUser(user)
        advanceUntilIdle()
        authManager.restoreCurrentUser(user)

        viewModel.toggleLessonStatus("test_lesson_1", true)
        advanceUntilIdle()

        // Verify lesson is now completed
        val updatedLesson = repository.getLessonProgressById("test_lesson_1")
        assertNotNull(updatedLesson)
        assertTrue(updatedLesson!!.completed)

        // Verify points awarded
        val updatedUser = repository.getUserByUsername("student_lesson")
        assertNotNull(updatedUser)
        assertEquals(150, updatedUser!!.points)
    }

    @Test
    fun `toggleLessonStatus does not award points if already completed`() = runTest {
        // Seed a lesson already completed
        val lesson = LessonProgress("test_lesson_2", "Done Lesson", "Beginner", true, 300)
        repository.insertLessonProgress(lesson)
        advanceUntilIdle()

        val user = UserAccount(
            username = "student_done",
            role = "STUDENT",
            hashedPassword = "hash",
            salt = "salt",
            points = 100
        )
        repository.insertUser(user)
        advanceUntilIdle()
        authManager.restoreCurrentUser(user)

        viewModel.toggleLessonStatus("test_lesson_2", true)
        advanceUntilIdle()

        // Points unchanged because lesson was already completed
        val updatedUser = repository.getUserByUsername("student_done")
        assertNotNull(updatedUser)
        assertEquals(100, updatedUser!!.points) // no extra 150
    }

    // --- Session deletion test ---

    @Test
    fun `deleteSession removes session from repository`() = runTest {
        // Insert a session via repository
        val session = com.violinmaster.app.data.PracticeSession(
            dateString = "2026-05-27",
            durationSeconds = 120,
            category = "Scales"
        )
        repository.insertSession(session)
        advanceUntilIdle()

        val sessions = repository.allSessions.first()
        assertEquals(1, sessions.size)
        val sessionId = sessions[0].id

        viewModel.deleteSession(sessionId)
        advanceUntilIdle()

        val afterDelete = repository.allSessions.first()
        assertTrue(afterDelete.isEmpty())
    }

    // --- Demo history test ---

    @Test
    fun `generateDemoHistory populates sessions and updates lessons`() = runTest {
        // Seed lessons so demo history can update them
        val defaults = listOf(
            LessonProgress("beg_1", "Posture & Open Strings Bowing", "Beginner", false, 0),
            LessonProgress("adv_1", "Bowing Styles: Martelé, Spiccato", "Advanced", false, 0)
        )
        for (lesson in defaults) {
            repository.insertLessonProgress(lesson)
        }
        advanceUntilIdle()

        viewModel.generateDemoHistory()
        advanceUntilIdle()

        val sessions = repository.allSessions.first()
        assertTrue(sessions.isNotEmpty()) // demo history creates sessions

        val beg1 = repository.getLessonProgressById("beg_1")
        assertNotNull(beg1)
        assertTrue(beg1!!.completed)
    }

    // --- updateSkillLevel test ---

    @Test
    fun `updateSkillLevel changes user skill level`() = runTest {
        val user = UserAccount(
            username = "advanced_user",
            role = "FREELANCER",
            hashedPassword = "hash",
            salt = "salt",
            points = 500,
            skillLevel = "Beginner"
        )
        repository.insertUser(user)
        advanceUntilIdle()
        authManager.restoreCurrentUser(user)

        viewModel.updateSkillLevel("Advanced")
        advanceUntilIdle()

        val updatedUser = repository.getUserByUsername("advanced_user")
        assertNotNull(updatedUser)
        assertEquals("Advanced", updatedUser!!.skillLevel)
    }
}
