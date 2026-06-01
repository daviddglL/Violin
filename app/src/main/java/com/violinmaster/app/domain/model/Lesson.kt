package com.violinmaster.app.domain.model

/**
 * Pure domain model for a lesson.
 *
 * No Room annotations — this is a plain Kotlin data class.
 *
 * REQ-ARCH-001: Domain models must be pure Kotlin with no Android imports.
 */
data class Lesson(
    val lessonId: String,
    val title: String,
    val difficulty: String,
    val completed: Boolean = false,
    val totalPracticedSeconds: Int = 0
)
