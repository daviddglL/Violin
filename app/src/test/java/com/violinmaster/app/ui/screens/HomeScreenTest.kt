package com.violinmaster.app.ui.screens

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.violinmaster.app.audio.ViolinAudioEngine
import com.violinmaster.app.data.CloudConfig
import com.violinmaster.app.data.PracticeDatabase
import com.violinmaster.app.data.PracticeRepository
import com.violinmaster.app.data.firebase.AssignmentDoc
import com.violinmaster.app.data.firebase.AssignmentSyncRepository
import com.violinmaster.app.data.firebase.FakeFirestoreCollection
import com.violinmaster.app.data.firebase.LessonDoc
import com.violinmaster.app.data.firebase.LessonSyncRepository
import com.violinmaster.app.data.firebase.SessionDoc
import com.violinmaster.app.data.firebase.SessionSyncRepository
import com.violinmaster.app.data.firebase.UserDoc
import com.violinmaster.app.data.firebase.UserSyncRepository
import com.violinmaster.app.di.AuthManager
import com.violinmaster.app.di.NavigationManager
import com.violinmaster.app.di.UserPreferencesManager
import com.violinmaster.app.domain.usecase.DeletePracticeSessionUseCase
import com.violinmaster.app.domain.usecase.EarnPointsUseCase
import com.violinmaster.app.domain.usecase.GenerateDemoHistoryUseCase
import com.violinmaster.app.domain.usecase.GetPracticeSessionsUseCase
import com.violinmaster.app.domain.usecase.SavePracticeSessionUseCase
import com.violinmaster.app.domain.usecase.SeedDefaultLessonsUseCase
import com.violinmaster.app.domain.usecase.ToggleLessonStatusUseCase
import com.violinmaster.app.domain.usecase.UpdateLessonProgressUseCase
import com.violinmaster.app.domain.usecase.UpdateSkillLevelUseCase
import com.violinmaster.app.ui.viewmodel.PracticeViewModel
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class HomeScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var database: PracticeDatabase
    private lateinit var repository: PracticeRepository
    private lateinit var authManager: AuthManager
    private lateinit var userPreferencesManager: UserPreferencesManager
    private lateinit var navigationManager: NavigationManager
    private lateinit var audioEngine: ViolinAudioEngine
    private lateinit var savePracticeSessionUseCase: SavePracticeSessionUseCase
    private lateinit var getPracticeSessionsUseCase: GetPracticeSessionsUseCase
    private lateinit var viewModel: PracticeViewModel
    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        val inlineExecutor = java.util.concurrent.Executor { r -> r.run() }
        database = Room.inMemoryDatabaseBuilder(context, PracticeDatabase::class.java)
            .setTransactionExecutor(inlineExecutor)
            .setQueryExecutor(inlineExecutor)
            .build()
        val sessionSync = SessionSyncRepository(FakeFirestoreCollection<SessionDoc>(), database.sessionDao())
        val lessonSync = LessonSyncRepository(FakeFirestoreCollection<LessonDoc>(), database.lessonDao())
        val userSync = UserSyncRepository(FakeFirestoreCollection<UserDoc>(), database.userDao())
        val assignmentSync = AssignmentSyncRepository(FakeFirestoreCollection<AssignmentDoc>(), database.assignmentDao())
        repository = PracticeRepository(sessionSync, lessonSync, userSync, assignmentSync,
            database.sessionDao(), database.lessonDao(), database.userDao(), database.assignmentDao(), CloudConfig())
        authManager = AuthManager(context)
        userPreferencesManager = UserPreferencesManager(context)
        navigationManager = NavigationManager()
        audioEngine = ViolinAudioEngine()
        savePracticeSessionUseCase = SavePracticeSessionUseCase(repository)
        getPracticeSessionsUseCase = GetPracticeSessionsUseCase(repository)
        val updateLessonProgressUseCase = UpdateLessonProgressUseCase(repository)
        val generateDemoHistoryUseCase = GenerateDemoHistoryUseCase(repository)
        val toggleLessonStatusUseCase = ToggleLessonStatusUseCase(repository, authManager)
        val deletePracticeSessionUseCase = DeletePracticeSessionUseCase(repository)
        val seedDefaultLessonsUseCase = SeedDefaultLessonsUseCase(repository)
        val earnPointsUseCase = EarnPointsUseCase(repository, authManager)
        val updateSkillLevelUseCase = UpdateSkillLevelUseCase(repository, authManager)
        viewModel = PracticeViewModel(
            repository, authManager, userPreferencesManager, audioEngine,
            savePracticeSessionUseCase, getPracticeSessionsUseCase,
            updateLessonProgressUseCase, generateDemoHistoryUseCase,
            toggleLessonStatusUseCase, deletePracticeSessionUseCase,
            seedDefaultLessonsUseCase, earnPointsUseCase, updateSkillLevelUseCase
        )
    }

    @After
    fun tearDown() {
        database.close()
        audioEngine.releaseAll()
    }

    @Test
    fun `home screen shows practice progress card`() {
        composeTestRule.setContent {
            HomeScreen(
                practiceVM = viewModel,
                authManager = authManager,
                userPreferencesManager = userPreferencesManager,
                navigationManager = navigationManager
            )
        }

        composeTestRule.onNodeWithTag("practice_progress_card").assertIsDisplayed()
    }

    @Test
    fun `home screen shows tuner tool button`() {
        composeTestRule.setContent {
            HomeScreen(
                practiceVM = viewModel,
                authManager = authManager,
                userPreferencesManager = userPreferencesManager,
                navigationManager = navigationManager
            )
        }

        composeTestRule.onNodeWithTag("tuner_tool_button").assertIsDisplayed()
    }

    @Test
    fun `home screen shows metronome tool button`() {
        composeTestRule.setContent {
            HomeScreen(
                practiceVM = viewModel,
                authManager = authManager,
                userPreferencesManager = userPreferencesManager,
                navigationManager = navigationManager
            )
        }

        composeTestRule.onNodeWithTag("metronome_tool_button").assertIsDisplayed()
    }

    @Test
    fun `home screen shows active lesson card with resume button`() {
        composeTestRule.setContent {
            HomeScreen(
                practiceVM = viewModel,
                authManager = authManager,
                userPreferencesManager = userPreferencesManager,
                navigationManager = navigationManager
            )
        }

        composeTestRule.onNodeWithTag("active_lesson_card").fetchSemanticsNode()
        composeTestRule.onNodeWithTag("resume_practice_button").fetchSemanticsNode()
    }

    @Test
    fun `home screen shows daily tasks list`() {
        composeTestRule.setContent {
            HomeScreen(
                practiceVM = viewModel,
                authManager = authManager,
                userPreferencesManager = userPreferencesManager,
                navigationManager = navigationManager
            )
        }

        // Beginner daily tasks should render (default skill level)
        composeTestRule.onNodeWithTag("daily_task_item_beg_dt1").fetchSemanticsNode()
        composeTestRule.onNodeWithTag("daily_task_item_beg_dt2").fetchSemanticsNode()
        composeTestRule.onNodeWithTag("daily_task_item_beg_dt3").fetchSemanticsNode()
    }

    // ═══════════════════════════════════════════════════════════════
    // QA-003: "Take Quiz" button navigates to quiz tab
    // ═══════════════════════════════════════════════════════════════

    @Test
    fun `QA-003 - take quiz button navigates to lessons tab`() {
        composeTestRule.setContent {
            HomeScreen(
                practiceVM = viewModel,
                authManager = authManager,
                userPreferencesManager = userPreferencesManager,
                navigationManager = navigationManager
            )
        }

        composeTestRule.onNodeWithTag("take_quiz_button").assertIsDisplayed()
        composeTestRule.onNodeWithTag("take_quiz_button").performClick()

        assertEquals("Should navigate to Lessons tab (index 1)", 1, navigationManager.currentTab.value)
    }
}
