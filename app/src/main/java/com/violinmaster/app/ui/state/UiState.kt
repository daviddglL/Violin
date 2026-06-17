package com.violinmaster.app.ui.state

/**
 * Represents the UI state for a screen backed by a ViewModel.
 *
 * Three canonical states:
 * - [Loading]: data is being fetched or operation is in progress
 * - [Error]: something went wrong, with a human-readable message and optional throwable
 * - [Content]: data is ready to display
 *
 * REQ-UISTATE-001: All ViewModels must expose UiState.
 * REQ-UISTATE-002: Screens must handle all three states.
 *
 * @param T The type of data held in the [Content] state.
 */
sealed class UiState<out T> {

    /** Initial or in-progress loading state. */
    data object Loading : UiState<Nothing>()

    /** Error state with a user-facing message and optional throwable for crash reporting. */
    data class Error(
        val message: String,
        val throwable: Throwable? = null
    ) : UiState<Nothing>()

    /** Success state holding the display data. */
    data class Content<T>(val data: T) : UiState<T>()

    // ── Convenience extensions ─────────────────────────────────────────

    /** Returns true when the state is [Loading]. */
    val isLoading: Boolean get() = this is Loading

    /** Returns true when the state is [Error]. */
    val isError: Boolean get() = this is Error

    /** Returns true when the state is [Content]. */
    val isContent: Boolean get() = this is Content

    /** Returns the data if this is [Content], or null otherwise. */
    fun getOrNull(): T? = (this as? Content)?.data

    /** Returns the error message if this is [Error], or null otherwise. */
    fun errorOrNull(): String? = (this as? Error)?.message
}
