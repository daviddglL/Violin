package com.violinmaster.app.data.firebase

import com.violinmaster.app.data.PracticeSession

/**
 * Firestore-compatible session document for cloud ↔ local sync.
 *
 * Mirrors [PracticeSession] fields. The Room auto-generated Int [id] is
 * stored as String for Firestore document ID compatibility.
 */
data class SessionDoc(
    val id: String = "",
    val dateString: String = "",
    val durationSeconds: Int = 0,
    val category: String = "",
    val timestamp: Long = System.currentTimeMillis()
) {
    companion object {
        /**
         * Creates a [SessionDoc] from a Room [PracticeSession] entity.
         */
        fun fromEntity(entity: PracticeSession): SessionDoc = SessionDoc(
            id = entity.id.toString(),
            dateString = entity.dateString,
            durationSeconds = entity.durationSeconds,
            category = entity.category,
            timestamp = entity.timestamp
        )
    }

    /**
     * Converts this Firestore document back to a Room [PracticeSession].
     */
    fun toEntity(): PracticeSession = PracticeSession(
        id = id.toIntOrNull() ?: 0,
        dateString = dateString,
        durationSeconds = durationSeconds,
        category = category,
        timestamp = timestamp
    )
}
