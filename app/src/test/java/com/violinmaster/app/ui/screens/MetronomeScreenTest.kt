package com.violinmaster.app.ui.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import com.violinmaster.app.audio.ViolinAudioEngine
import com.violinmaster.app.data.AnalyticsHelper
import com.violinmaster.app.data.IAnalyticsService
import com.violinmaster.app.data.ICrashReportingService
import com.violinmaster.app.ui.theme.AppLanguage
import com.violinmaster.app.ui.viewmodel.MetronomeViewModel
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class MetronomeScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var audioEngine: ViolinAudioEngine
    private lateinit var viewModel: MetronomeViewModel

    @Before
    fun setup() {
        audioEngine = ViolinAudioEngine()
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
        viewModel = MetronomeViewModel(audioEngine, analyticsHelper)
    }

    @After
    fun tearDown() {
        audioEngine.releaseAll()
    }

    @Test
    fun `metronome shows pulse card`() {
        composeTestRule.setContent {
            MetronomeScreen(
                viewModel = viewModel,
                appLanguage = AppLanguage.ENGLISH
            )
        }

        composeTestRule.onNodeWithTag("metronome_pulse_card").assertIsDisplayed()
    }

    @Test
    fun `metronome shows play pause toggle button`() {
        composeTestRule.setContent {
            MetronomeScreen(
                viewModel = viewModel,
                appLanguage = AppLanguage.ENGLISH
            )
        }

        composeTestRule.onNodeWithTag("toggle_metronome_button").fetchSemanticsNode()
    }

    @Test
    fun `metronome shows BPM slider controls`() {
        composeTestRule.setContent {
            MetronomeScreen(
                viewModel = viewModel,
                appLanguage = AppLanguage.ENGLISH
            )
        }

        composeTestRule.onNodeWithTag("bpm_slider").assertIsDisplayed()
        composeTestRule.onNodeWithTag("bpm_decrement_5").assertIsDisplayed()
        composeTestRule.onNodeWithTag("bpm_increment_5").assertIsDisplayed()
    }

    @Test
    fun `metronome shows time signature selector`() {
        composeTestRule.setContent {
            MetronomeScreen(
                viewModel = viewModel,
                appLanguage = AppLanguage.ENGLISH
            )
        }

        composeTestRule.onNodeWithTag("beat_selector_card").fetchSemanticsNode()
        composeTestRule.onNodeWithTag("time_sign_button_2").fetchSemanticsNode()
        composeTestRule.onNodeWithTag("time_sign_button_4").fetchSemanticsNode()
    }

    @Test
    fun `metronome shows tap tempo button`() {
        composeTestRule.setContent {
            MetronomeScreen(
                viewModel = viewModel,
                appLanguage = AppLanguage.ENGLISH
            )
        }

        composeTestRule.onNodeWithTag("tap_tempo_button").fetchSemanticsNode()
    }

    @Test
    fun `metronome shows accent selector card`() {
        composeTestRule.setContent {
            MetronomeScreen(
                viewModel = viewModel,
                appLanguage = AppLanguage.ENGLISH
            )
        }

        composeTestRule.onNodeWithTag("accent_selector_card").fetchSemanticsNode()
    }

    @Test
    fun `metronome in SPANISH renders without crash`() {
        composeTestRule.setContent {
            MetronomeScreen(
                viewModel = viewModel,
                appLanguage = AppLanguage.SPANISH
            )
        }

        composeTestRule.onNodeWithTag("toggle_metronome_button").fetchSemanticsNode()
        composeTestRule.onNodeWithTag("beat_selector_card").fetchSemanticsNode()
    }
}
