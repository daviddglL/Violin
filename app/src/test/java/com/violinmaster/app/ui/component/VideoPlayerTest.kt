package com.violinmaster.app.ui.component

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class VideoPlayerTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `VP-001a - VideoPlayer renders player view with valid video URL`() {
        var closeClicked = false
        composeTestRule.setContent {
            VideoPlayer(
                videoUrl = "https://example.com/sample.mp4",
                onClose = { closeClicked = true }
            )
        }

        // The AndroidView wrapping PlayerView should be displayed
        composeTestRule.onNodeWithTag("video_player_view").assertIsDisplayed()
        // The close button should be visible
        composeTestRule.onNodeWithTag("video_player_close_button").assertIsDisplayed()
    }

    @Test
    fun `VP-001b - VideoPlayer shows error state with empty URL`() {
        composeTestRule.setContent {
            VideoPlayer(
                videoUrl = "",
                onClose = {}
            )
        }

        // Error state should be visible instead of player
        composeTestRule.onNodeWithTag("video_player_error").assertIsDisplayed()
    }

    @Test
    fun `VP-001a - VideoPlayer close button triggers onClose callback`() {
        var closeClicked = false
        composeTestRule.setContent {
            VideoPlayer(
                videoUrl = "https://example.com/sample.mp4",
                onClose = { closeClicked = true }
            )
        }

        composeTestRule.onNodeWithTag("video_player_close_button").performClick()
        assert(closeClicked) { "onClose callback should have been invoked" }
    }

    @Test
    fun `LessonVideoPlayer wraps VideoPlayer with videoUrl param`() {
        composeTestRule.setContent {
            LessonVideoPlayer(
                videoTitle = "Test Lesson",
                videoUrl = "https://example.com/lesson.mp4",
                onClose = {}
            )
        }

        // The wrapped VideoPlayer should render
        composeTestRule.onNodeWithTag("video_player_view").assertIsDisplayed()
    }
}
