package com.violinmaster.app.data.firebase

/**
 * Firebase Firestore collection path constants.
 *
 * Centralizes collection names and path builders so Firestore
 * paths are defined in one place. All cloud-sync collection
 * paths are documented here for consistency across the codebase.
 */
object FirebaseCollections {
    const val USERS = "users"
    const val LESSON_PROGRESS = "lesson_progress"
    const val ASSIGNMENTS = "assignments"
    const val MESSAGES = "messages"

    /**
     * Returns the Firestore path for a user's session subcollection.
     *
     * Example: sessionsPath("abc123") → "users/abc123/sessions"
     */
    fun sessionsPath(uid: String): String =
        "$USERS/$uid/sessions"

    /**
     * Returns the Firestore collection path for messages within an assignment.
     *
     * Example: messagesPath("A1") → "assignments/A1/messages"
     */
    fun messagesPath(assignmentId: String): String =
        "$ASSIGNMENTS/$assignmentId/$MESSAGES"
}
