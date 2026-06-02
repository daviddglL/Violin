package com.violinmaster.app.ui.screens

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.violinmaster.app.audio.ViolinAudioEngine
import com.violinmaster.app.data.PracticeDatabase
import com.violinmaster.app.data.PracticeRepository
import com.violinmaster.app.di.AuthManager
import com.violinmaster.app.di.NavigationManager
import com.violinmaster.app.di.UserPreferencesManager
import com.violinmaster.app.ui.viewmodel.PracticeViewModel
import org.junit.After
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
    private lateinit var viewModel: PracticeViewModel
    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, PracticeDatabase::class.java).build()
        repository = PracticeRepository(database.sessionDao(), database.lessonDao(), database.userDao(), database.assignmentDao())
        authManager = AuthManager(context)
        userPreferencesManager = UserPreferencesManager(context)
        navigationManager = NavigationManager()
        audioEngine = ViolinAudioEngine()
        viewModel = PracticeViewModel(repository, authManager, userPreferencesManager, audioEngine)
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

        composeTestRule.onNodeWithTag("active_lesson_card").assertIsDisplayed()
        composeTestRule.onNodeWithTag("resume_practice_button").assertIsDisplayed()
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
        composeTestRule.onNodeWithTag("daily_task_item_beg_dt1").assertIsDisplayed()
        composeTestRule.onNodeWithTag("daily_task_item_beg_dt2").assertIsDisplayed()
        composeTestRule.onNodeWithTag("daily_task_item_beg_dt3").assertIsDisplayed()
    }
}
