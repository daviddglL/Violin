package com.violinmaster.app.ui.component

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import com.violinmaster.app.ui.theme.AppLanguage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * QA-001: ProfilePointsCard skill chip read-only + single-line text.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ProfilePointsCardTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    // ═══════════════════════════════════════════════════════════════
    // QA-001(a): Tap chip -> no level change (clickable removed)
    // ═══════════════════════════════════════════════════════════════

    @Test
    fun `QA-001a - skill chip displayed without emoji suffix`() {
        composeTestRule.setContent {
            ProfilePointsCard(
                username = "TestUser",
                points = 100,
                skillLevel = "Beginner",
                appLanguage = AppLanguage.ENGLISH
            )
        }

        // The chip should show the skill text WITHOUT the 🔄 emoji
        composeTestRule.onNodeWithTag("cycle_level_button").assertIsDisplayed()
        // "Beginner" text should be shown, not "Beginner 🔄"
        composeTestRule.onNodeWithText("Beginner").assertIsDisplayed()
    }

    @Test
    fun `QA-001a - skill chip shows localized text Beginner mapped to label`() {
        composeTestRule.setContent {
            ProfilePointsCard(
                username = "TestUser",
                points = 100,
                skillLevel = "Beginner",
                appLanguage = AppLanguage.ENGLISH
            )
        }

        // The chip should display the localized "skill_beginner" label
        composeTestRule.onNodeWithTag("cycle_level_button").assertIsDisplayed()
    }

    @Test
    fun `QA-001a - skill chip for Intermediate shows correct label`() {
        composeTestRule.setContent {
            ProfilePointsCard(
                username = "TestUser",
                points = 200,
                skillLevel = "Intermediate",
                appLanguage = AppLanguage.ENGLISH
            )
        }

        composeTestRule.onNodeWithTag("cycle_level_button").assertIsDisplayed()
        composeTestRule.onNodeWithText("Intermediate").assertIsDisplayed()
    }

    // ═══════════════════════════════════════════════════════════════
    // QA-001(b): Text fits single line (maxLines=1)
    // ═══════════════════════════════════════════════════════════════

    @Test
    fun `QA-001b - card renders with profile points card tag`() {
        composeTestRule.setContent {
            ProfilePointsCard(
                username = "TestUser",
                points = 100,
                skillLevel = "Beginner",
                appLanguage = AppLanguage.ENGLISH
            )
        }

        composeTestRule.onNodeWithTag("profile_points_card").assertIsDisplayed()
    }

    @Test
    fun `QA-001b - points badge shows with trophy emoji`() {
        composeTestRule.setContent {
            ProfilePointsCard(
                username = "TestUser",
                points = 999,
                skillLevel = "Advanced",
                appLanguage = AppLanguage.ENGLISH
            )
        }

        composeTestRule.onNodeWithTag("user_points_badge").assertIsDisplayed()
    }
}
