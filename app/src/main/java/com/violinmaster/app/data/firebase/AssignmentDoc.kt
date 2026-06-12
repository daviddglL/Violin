package com.violinmaster.app.data.firebase

import com.violinmaster.app.data.Assignment

/**
 * Firestore-compatible assignment document for cloud ↔ local sync.
 *
 * Mirrors [Assignment] fields. The Room auto-generated Int [id] is
 * stored as String for Firestore document ID compatibility.
 *
 * HashedPassword and salt are NEVER included — those fields don't exist
 * on Assignment (they're on UserAccount only).
 */
data class AssignmentDoc(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val teacherUsername: String = "",
    val studentUsername: String = "",
    val videoTitle: String = "",
    val videoDurationSeconds: Int = 0,
    val videoResourceUrl: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val completed: Boolean = false
) {
    companion object {
        /**
         * Creates an [AssignmentDoc] from a Room [Assignment] entity.
         */
        fun fromEntity(entity: Assignment): AssignmentDoc = AssignmentDoc(
            id = entity.id.toString(),
            title = entity.title,
            description = entity.description,
            teacherUsername = entity.teacherUsername,
            studentUsername = entity.studentUsername,
            videoTitle = entity.videoTitle,
            videoDurationSeconds = entity.videoDurationSeconds,
            videoResourceUrl = entity.videoResourceUrl,
            timestamp = entity.timestamp,
            completed = entity.completed
        )
    }

    /**
     * Converts this Firestore document back to a Room [Assignment].
     */
    fun toEntity(): Assignment = Assignment(
        id = id.toIntOrNull() ?: 0,
        title = title,
        description = description,
        teacherUsername = teacherUsername,
        studentUsername = studentUsername,
        videoTitle = videoTitle,
        videoDurationSeconds = videoDurationSeconds,
        videoResourceUrl = videoResourceUrl,
        timestamp = timestamp,
        completed = completed
    )
}
