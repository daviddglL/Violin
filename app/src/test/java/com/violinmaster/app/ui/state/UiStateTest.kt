package com.violinmaster.app.ui.state

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * TDD tests for UiState sealed class.
 *
 * REQ-UISTATE-001: UiState<T> with Loading, Error(message, throwable?), Content(data).
 * REQ-UISTATE-002: Screen state handling with loading/error/content variants.
 *
 * RED phase: UiState.kt does not exist yet. These tests verify the contract.
 */
@RunWith(RobolectricTestRunner::class)
class UiStateTest {

    // ── Loading variant ──────────────────────────────────────────────

    @Test
    fun `Loading is a single object instance`() {
        val state1: UiState<String> = UiState.Loading
        val state2: UiState<Int> = UiState.Loading

        // Loading is the same object regardless of type parameter
        assertTrue("Loading should be a data object", state1 is UiState.Loading)
        assertTrue(state2 is UiState.Loading)
    }

    @Test
    fun `Loading is not Content and not Error`() {
        val state: UiState<String> = UiState.Loading

        assertTrue("Loading is not Content", state !is UiState.Content)
        assertTrue("Loading is not Error", state !is UiState.Error)
    }

    // ── Error variant ────────────────────────────────────────────────

    @Test
    fun `Error stores message and optional throwable`() {
        val throwable = RuntimeException("Test exception")
        val state: UiState<String> = UiState.Error(
            message = "Network failure",
            throwable = throwable
        )

        assertTrue("Should be Error variant", state is UiState.Error)
        val error = state as UiState.Error
        assertEquals("Network failure", error.message)
        assertEquals(throwable, error.throwable)
        assertEquals("Test exception", error.throwable!!.message)
    }

    @Test
    fun `Error throwable is null by default`() {
        val state: UiState<Int> = UiState.Error(message = "Something went wrong")

        val error = state as UiState.Error
        assertEquals("Something went wrong", error.message)
        assertNull("throwable should default to null", error.throwable)
    }

    @Test
    fun `Error is not Content and not Loading`() {
        val state: UiState<String> = UiState.Error(message = "Fail")

        assertTrue("Error is not Content", state !is UiState.Content)
        assertTrue("Error is not Loading", state !is UiState.Loading)
    }

    // ── Content variant ──────────────────────────────────────────────

    @Test
    fun `Content stores generic data`() {
        val state: UiState<List<String>> = UiState.Content(
            data = listOf("item1", "item2")
        )

        assertTrue("Should be Content variant", state is UiState.Content)
        val content = state as UiState.Content<List<String>>
        assertEquals(listOf("item1", "item2"), content.data)
        assertEquals(2, content.data.size)
    }

    @Test
    fun `Content with different types`() {
        val stringState = UiState.Content(data = "hello")
        val intState = UiState.Content(data = 42)

        assertEquals("hello", (stringState as UiState.Content).data)
        assertEquals(42, (intState as UiState.Content).data)
    }

    @Test
    fun `Content is not Loading and not Error`() {
        val state: UiState<String> = UiState.Content(data = "data")

        assertTrue("Content is not Loading", state !is UiState.Loading)
        assertTrue("Content is not Error", state !is UiState.Error)
    }

    // ── Extension functions ──────────────────────────────────────────

    @Test
    fun `isLoading returns true only for Loading`() {
        val loading: UiState<String> = UiState.Loading
        val error: UiState<String> = UiState.Error(message = "fail")
        val content: UiState<String> = UiState.Content(data = "data")

        assertTrue("Loading.isLoading should be true", loading.isLoading)
        assertFalse("Error.isLoading should be false", error.isLoading)
        assertFalse("Content.isLoading should be false", content.isLoading)
    }

    @Test
    fun `isError returns true only for Error`() {
        val loading: UiState<String> = UiState.Loading
        val error: UiState<String> = UiState.Error(message = "fail")
        val content: UiState<String> = UiState.Content(data = "data")

        assertFalse("Loading.isError should be false", loading.isError)
        assertTrue("Error.isError should be true", error.isError)
        assertFalse("Content.isError should be false", content.isError)
    }

    @Test
    fun `isContent returns true only for Content`() {
        val loading: UiState<String> = UiState.Loading
        val error: UiState<String> = UiState.Error(message = "fail")
        val content: UiState<String> = UiState.Content(data = "data")

        assertFalse("Loading.isContent should be false", loading.isContent)
        assertFalse("Error.isContent should be false", error.isContent)
        assertTrue("Content.isContent should be true", content.isContent)
    }

    @Test
    fun `getOrNull returns data for Content and null otherwise`() {
        val loading: UiState<String> = UiState.Loading
        val error: UiState<String> = UiState.Error(message = "fail")
        val content: UiState<String> = UiState.Content(data = "hello")

        assertNull("Loading.getOrNull should return null", loading.getOrNull())
        assertNull("Error.getOrNull should return null", error.getOrNull())
        assertEquals("hello", content.getOrNull())
    }

    @Test
    fun `getOrNull with different type`() {
        val content: UiState<Int> = UiState.Content(data = 99)
        assertEquals(99, content.getOrNull())
    }

    @Test
    fun `getErrorMessageOrNull returns message for Error and null otherwise`() {
        val loading: UiState<String> = UiState.Loading
        val error: UiState<String> = UiState.Error(message = "Failed to load")
        val content: UiState<String> = UiState.Content(data = "ok")

        assertNull("Loading.errorOrNull should return null", loading.errorOrNull())
        assertEquals("Failed to load", error.errorOrNull())
        assertNull("Content.errorOrNull should return null", content.errorOrNull())
    }
}
