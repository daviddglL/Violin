package com.violinmaster.app.domain.model

/**
 * Pure domain model for a chat message.
 *
 * No Room annotations — this is a plain Kotlin data class.
 *
 * REQ-ARCH-001: Domain models must be pure Kotlin with no Android imports.
 */
data class Message(
    val id: String = "",
    val assignmentId: String,
    val senderUsername: String,
    val senderRole: String,
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
)
