package com.violinmaster.app.ui.screens

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.core.app.ApplicationProvider
import com.violinmaster.app.audio.TunerEngine
import com.violinmaster.app.audio.ViolinAudioEngine
import com.violinmaster.app.di.UserPreferencesManager
import com.violinmaster.app.ui.theme.AppLanguage
import com.violinmaster.app.ui.viewmodel.TunerViewModel
import org.junit.After
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@Ignore("Requires ActivityResultRegistry — Compose UI tests need NavHost or Activity wrapper")
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
        viewModel = TunerViewModel(audioEngine, tunerEngine, userPreferencesManager)
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

        composeTestRule.onNodeWithTag("string_note_button_G").assertIsDisplayed()
        composeTestRule.onNodeWithTag("string_note_button_D").assertIsDisplayed()
        composeTestRule.onNodeWithTag("string_note_button_A").assertIsDisplayed()
        composeTestRule.onNodeWithTag("string_note_button_E").assertIsDisplayed()
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

        composeTestRule.onNodeWithTag("string_note_button_A").assertIsDisplayed()
        composeTestRule.onNodeWithTag("tuner_gauge_container").assertIsDisplayed()
    }
}
