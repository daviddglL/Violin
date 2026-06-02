package com.violinmaster.app.domain.model

/**
 * Pure domain model for a practice session.
 *
 * No Room annotations — this is a plain Kotlin data class.
 *
 * REQ-ARCH-001: Domain models must be pure Kotlin with no Android imports.
 */
data class PracticeSession(
    val id: Int = 0,
    val dateString: String,
    val durationSeconds: Int,
    val category: String,
    val timestamp: Long = System.currentTimeMillis()
)
