package com.violinmaster.app.domain.util

/**
 * Pure utility for calculating points based on attempts and skill level.
 *
 * No Android imports — functions are pure and testable without Robolectric.
 *
 * REQ-ARCH-005: Extract scoring logic from ViewModels into pure domain utilities.
 */
object PointsCalculator {

    /**
     * Calculates points for a task completion.
     *
     * Base points by attempt count:
     * - 1st attempt: 100
     * - 2nd attempt: 75
     * - 3rd attempt: 50
     * - 4th attempt: 25
     * - 5th+ attempt: 10 (minimum)
     *
     * Skill level multiplier:
     * - Beginner: 1.0x
     * - Intermediate: 1.5x
     * - Advanced: 2.0x
     */
    fun calculatePoints(attempts: Int, skillLevel: String): Int {
        val base = when {
            attempts <= 1 -> 100
            attempts == 2 -> 75
            attempts == 3 -> 50
            attempts == 4 -> 25
            else -> 10
        }
        val multiplier = when (skillLevel.lowercase()) {
            "beginner" -> 1.0
            "intermediate" -> 1.5
            "advanced" -> 2.0
            else -> 1.0
        }
        return (base * multiplier).toInt()
    }
}
