package com.violinmaster.app.domain.model

/**
 * Pure domain model for a student assignment.
 *
 * No Room annotations — this is a plain Kotlin data class.
 *
 * REQ-ARCH-001: Domain models must be pure Kotlin with no Android imports.
 */
data class Assignment(
    val id: Int = 0,
    val title: String,
    val description: String,
    val teacherUsername: String,
    val studentUsername: String,
    val completed: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)
