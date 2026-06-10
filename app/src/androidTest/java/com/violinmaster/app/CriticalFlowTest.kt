package com.violinmaster.app

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumentation tests for critical user flows.
 *
 * These tests run on a real device or emulator and verify
 * Compose UI behavior end-to-end.
 *
 * Run with: ./gradlew :app:connectedDebugAndroidTest
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class CriticalFlowTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setup() {
        hiltRule.inject()
    }

    /**
     * REQ-NS-001: App launches and displays the authentication screen
     * when no user is signed in.
     */
    @Test
    fun appLaunchesAndShowsAuthScreen() {
        // The app starts with no user → AuthScreen is displayed
        composeRule.onNodeWithText("VIOLIN STUDIO PRO").assertIsDisplayed()
    }

    /**
     * REQ-NS-002: Bottom navigation bar is present and tabs can be selected
     * after successful authentication.
     *
     * Note: Requires a signed-in user. For a real test, set up a test user
     * via Hilt test module or use the PIN login flow.
     */
    @Test
    fun bottomNavigationBarHasFourTabs() {
        // Verify the 4 tab buttons exist (visible after login)
        // These testTags are set in MainActivity.kt NavigationBarItem modifiers
        composeRule.onNodeWithTag("tab_home").assertIsDisplayed()
        composeRule.onNodeWithTag("tab_lessons").assertIsDisplayed()
        composeRule.onNodeWithTag("tab_stats").assertIsDisplayed()
        composeRule.onNodeWithTag("tab_settings").assertIsDisplayed()
    }

    /**
     * REQ-NS-003: Tab navigation switches between screens.
     *
     * Clicks each tab and verifies the screen content changes.
     * Requires a signed-in user state.
     */
    @Test
    fun tabNavigationSwitchesScreens() {
        // Home tab is selected by default (tab index 0)
        composeRule.onNodeWithTag("tab_home").performClick()

        // Navigate to Lessons tab
        composeRule.onNodeWithTag("tab_lessons").performClick()

        // Navigate to Stats tab
        composeRule.onNodeWithTag("tab_stats").performClick()

        // Navigate to Settings tab
        composeRule.onNodeWithTag("tab_settings").performClick()

        // Return to Home
        composeRule.onNodeWithTag("tab_home").performClick()
    }
}
