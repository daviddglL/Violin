package com.violinmaster.app.ui.screens

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.core.app.ApplicationProvider
import com.violinmaster.app.audio.TunerEngine
import com.violinmaster.app.audio.ViolinAudioEngine
import com.violinmaster.app.data.AnalyticsHelper
import com.violinmaster.app.data.IAnalyticsService
import com.violinmaster.app.data.ICrashReportingService
import com.violinmaster.app.di.UserPreferencesManager
import com.violinmaster.app.ui.theme.AppLanguage
import com.violinmaster.app.ui.viewmodel.TunerViewModel
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class TunerScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var context: Context
    private lateinit var audioEngine: ViolinAudioEngine
    private lateinit var tunerEngine: TunerEngine
    private lateinit var userPreferencesManager: UserPreferencesManager
    private lateinit var viewModel: TunerViewModel

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext<Context>()
        audioEngine = ViolinAudioEngine()
        tunerEngine = TunerEngine()
        userPreferencesManager = UserPreferencesManager(context)
        val analyticsHelper = AnalyticsHelper(
            object : IAnalyticsService {
                override fun logEvent(name: String, params: Map<String, Any>) {}
                override fun setUserProperty(key: String, value: String) {}
                override fun setUserId(id: String) {}
                override fun setCurrentScreen(screenName: String, screenClass: String) {}
            },
            object : ICrashReportingService {
                override fun log(message: String) {}
                override fun recordException(throwable: Throwable) {}
                override fun setCustomKey(key: String, value: String) {}
            }
        )
        viewModel = TunerViewModel(audioEngine, tunerEngine, userPreferencesManager, analyticsHelper)
    }

    @After
    fun tearDown() {
        audioEngine.releaseAll()
        tunerEngine.release()
    }

    @Test
    fun `tuner screen shows string selection buttons G D A E`() {
        composeTestRule.setContent {
            TunerScreen(
                viewModel = viewModel,
                appLanguage = AppLanguage.ENGLISH
            )
        }

        composeTestRule.onNodeWithTag("string_note_button_G").fetchSemanticsNode()
        composeTestRule.onNodeWithTag("string_note_button_D").fetchSemanticsNode()
        composeTestRule.onNodeWithTag("string_note_button_A").fetchSemanticsNode()
        composeTestRule.onNodeWithTag("string_note_button_E").fetchSemanticsNode()
    }

    @Test
    fun `tuner screen shows tuner gauge container`() {
        composeTestRule.setContent {
            TunerScreen(
                viewModel = viewModel,
                appLanguage = AppLanguage.ENGLISH
            )
        }

        composeTestRule.onNodeWithTag("tuner_gauge_container").assertIsDisplayed()
    }

    @Test
    fun `tuner screen shows auto detect switch`() {
        composeTestRule.setContent {
            TunerScreen(
                viewModel = viewModel,
                appLanguage = AppLanguage.ENGLISH
            )
        }

        composeTestRule.onNodeWithTag("auto_detect_switch").assertIsDisplayed()
    }

    @Test
    fun `tuner screen shows listening toggle button`() {
        composeTestRule.setContent {
            TunerScreen(
                viewModel = viewModel,
                appLanguage = AppLanguage.ENGLISH
            )
        }

        composeTestRule.onNodeWithTag("listen_pitch_button").assertIsDisplayed()
    }

    @Test
    fun `tuner screen in SPANISH renders without crash`() {
        composeTestRule.setContent {
            TunerScreen(
                viewModel = viewModel,
                appLanguage = AppLanguage.SPANISH
            )
        }

        composeTestRule.onNodeWithTag("string_note_button_A").fetchSemanticsNode()
        composeTestRule.onNodeWithTag("tuner_gauge_container").fetchSemanticsNode()
    }
}
