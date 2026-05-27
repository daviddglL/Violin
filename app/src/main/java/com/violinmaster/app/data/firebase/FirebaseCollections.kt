package com.violinmaster.app.data.firebase

/**
 * Firebase Firestore collection path constants.
 *
 * Centralizes collection names and path builders so Firestore
 * paths are defined in one place.
 */
object FirebaseCollections {
    const val ASSIGNMENTS = "assignments"
    const val MESSAGES = "messages"

    /**
     * Returns the Firestore collection path for messages within an assignment.
     *
     * Example: messagesPath("A1") → "assignments/A1/messages"
     */
    fun messagesPath(assignmentId: String): String =
        "$ASSIGNMENTS/$assignmentId/$MESSAGES"
}
