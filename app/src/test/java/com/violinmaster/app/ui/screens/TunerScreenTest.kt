package com.violinmaster.app.ui.screens

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.violinmaster.app.audio.TunerEngine
import com.violinmaster.app.audio.ViolinAudioEngine
import com.violinmaster.app.data.UserAccount
import com.violinmaster.app.di.AuthManager
import com.violinmaster.app.di.TuningPreferencesManager
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

    private lateinit var audioEngine: ViolinAudioEngine
    private lateinit var tunerEngine: TunerEngine
    private lateinit var viewModel: TunerViewModel
    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext<Context>()
        context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
            .edit().clear().commit()

        val authManager = AuthManager(context)
        authManager.saveCurrentUser(
            UserAccount(
                username = "test_user",
                role = "STUDENT",
                hashedPassword = "hash",
                salt = "salt"
            )
        )
        val tuningPreferencesManager = TuningPreferencesManager(context, authManager)

        audioEngine = ViolinAudioEngine()
        tunerEngine = TunerEngine()
        viewModel = TunerViewModel(audioEngine, tunerEngine, tuningPreferencesManager)
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

    // ═══════════════════════════════════════════════════════════════════
    // Config Sheet UI tests
    // ═══════════════════════════════════════════════════════════════════

    @Test
    fun `config button is displayed`() {
        composeTestRule.setContent {
            TunerScreen(
                viewModel = viewModel,
                appLanguage = AppLanguage.ENGLISH
            )
        }
        composeTestRule.onNodeWithTag("config_button").assertIsDisplayed()
    }

    @Test
    fun `config button opens bottom sheet with reference pitch slider`() {
        composeTestRule.setContent {
            TunerScreen(
                viewModel = viewModel,
                appLanguage = AppLanguage.ENGLISH
            )
        }

        // Open config sheet
        composeTestRule.onNodeWithTag("config_button").performClick()

        // Sheet content should be visible
        composeTestRule.onNodeWithTag("reference_pitch_slider").assertIsDisplayed()
    }

    @Test
    fun `config sheet shows max cents options`() {
        composeTestRule.setContent {
            TunerScreen(
                viewModel = viewModel,
                appLanguage = AppLanguage.ENGLISH
            )
        }

        composeTestRule.onNodeWithTag("config_button").performClick()

        // Verify all max-cents options are rendered
        composeTestRule.onNodeWithTag("max_cents_option_25").assertIsDisplayed()
        composeTestRule.onNodeWithTag("max_cents_option_50").assertIsDisplayed()
        composeTestRule.onNodeWithTag("max_cents_option_75").assertIsDisplayed()
        composeTestRule.onNodeWithTag("max_cents_option_100").assertIsDisplayed()
        composeTestRule.onNodeWithTag("max_cents_option_150").assertIsDisplayed()
        composeTestRule.onNodeWithTag("max_cents_option_200").assertIsDisplayed()
    }

    @Test
    fun `config sheet shows preset name input and save button`() {
        composeTestRule.setContent {
            TunerScreen(
                viewModel = viewModel,
                appLanguage = AppLanguage.ENGLISH
            )
        }

        composeTestRule.onNodeWithTag("config_button").performClick()

        composeTestRule.onNodeWithTag("preset_name_input").assertIsDisplayed()
        composeTestRule.onNodeWithTag("save_preset_button").assertIsDisplayed()
    }

    @Test
    fun `config sheet shows no presets message when list is empty`() {
        composeTestRule.setContent {
            TunerScreen(
                viewModel = viewModel,
                appLanguage = AppLanguage.ENGLISH
            )
        }

        composeTestRule.onNodeWithTag("config_button").performClick()

        // Presets list should NOT be displayed when empty
        composeTestRule.onNodeWithTag("presets_list").assertIsNotDisplayed()
    }

    @Test
    fun `config sheet close button closes the sheet`() {
        composeTestRule.setContent {
            TunerScreen(
                viewModel = viewModel,
                appLanguage = AppLanguage.ENGLISH
            )
        }

        // Open
        composeTestRule.onNodeWithTag("config_button").performClick()
        composeTestRule.onNodeWithTag("reference_pitch_slider").assertIsDisplayed()

        // Close
        composeTestRule.onNodeWithTag("close_config_sheet").performClick()

        // After close, sheet content should no longer be displayed
        composeTestRule.onNodeWithTag("reference_pitch_slider").assertIsNotDisplayed()
    }

    @Test
    fun `config sheet shows saved preset with load and delete buttons`() {
        // Save a preset via ViewModel before rendering
        viewModel.updateReferencePitch(432)
        viewModel.updateMaxCents(75)
        viewModel.saveCurrentAsPreset("Baroque 432")

        composeTestRule.setContent {
            TunerScreen(
                viewModel = viewModel,
                appLanguage = AppLanguage.ENGLISH
            )
        }

        // Open config sheet
        composeTestRule.onNodeWithTag("config_button").performClick()

        // Presets list should now be visible
        composeTestRule.onNodeWithTag("presets_list").assertIsDisplayed()

        // Load and delete buttons for the saved preset
        composeTestRule.onNodeWithTag("load_preset_Baroque 432").assertIsDisplayed()
        composeTestRule.onNodeWithTag("delete_preset_Baroque 432").assertIsDisplayed()
    }
}
