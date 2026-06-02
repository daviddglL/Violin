package com.violinmaster.app.domain.util

/**
 * Centralized scoring rules for the application.
 *
 * No Android imports — pure Kotlin, testable without Robolectric.
 *
 * REQ-ARCH-005: Extract scoring logic from ViewModels into pure domain utilities.
 */
object ScoringPolicy {

    /** Points awarded when a lesson is newly completed. */
    const val LESSON_COMPLETION_POINTS = 150

    /**
     * Points awarded for completing a daily task, scaled by number of attempts.
     *
     * - 1st attempt: 100
     * - 2nd attempt: 75
     * - 3rd attempt: 50
     * - 4th+ attempt: 25
     *
     * @param attempts Number of attempts made for this task (1-indexed).
     * @return Points to award.
     */
    fun pointsForTaskCompletion(attempts: Int): Int = when (attempts) {
        1 -> 100
        2 -> 75
        3 -> 50
        else -> 25
    }
}
