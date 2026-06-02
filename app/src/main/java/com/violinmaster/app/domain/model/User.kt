package com.violinmaster.app.domain.model

import java.time.LocalDate

/**
 * Pure domain model for a user account.
 *
 * No Room annotations — this is a plain Kotlin data class.
 * The [isMinor] property is computed from [birthYear] using java.time.LocalDate
 * (no android.* or java.util.Calendar dependency).
 *
 * REQ-ARCH-001: Domain models must be pure Kotlin with no Android imports.
 */
data class User(
    val username: String,
    val role: String,
    val skillLevel: String = "Beginner",
    val points: Int = 0,
    val teacherCode: String = "",
    val birthYear: Int = 0
) {
    /** Whether the user is a minor (under 18). Computed from birthYear using java.time. */
    val isMinor: Boolean
        get() {
            if (birthYear <= 1900) return false
            val currentYear = LocalDate.now().year
            return (currentYear - birthYear) < 18
        }
}
