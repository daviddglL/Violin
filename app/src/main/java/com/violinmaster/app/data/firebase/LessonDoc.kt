package com.violinmaster.app.data.firebase

import com.violinmaster.app.data.LessonProgress

/**
 * Firestore-compatible lesson progress document for cloud ↔ local sync.
 *
 * Mirrors [LessonProgress] fields. Flat collection with compound docId:
 * `{firebaseUid}_{lessonId}` for per-user scoping.
 */
data class LessonDoc(
    val lessonId: String = "",
    val lessonTitle: String = "",
    val difficulty: String = "Beginner",
    val completed: Boolean = false,
    val totalPracticedSeconds: Int = 0,
    val lastPracticedTimestamp: Long = 0L
) {
    companion object {
        /**
         * Creates a [LessonDoc] from a Room [LessonProgress] entity.
         */
        fun fromEntity(entity: LessonProgress): LessonDoc = LessonDoc(
            lessonId = entity.lessonId,
            lessonTitle = entity.lessonTitle,
            difficulty = entity.difficulty,
            completed = entity.completed,
            totalPracticedSeconds = entity.totalPracticedSeconds,
            lastPracticedTimestamp = entity.lastPracticedTimestamp
        )
    }

    /**
     * Converts this Firestore document back to a Room [LessonProgress].
     */
    fun toEntity(): LessonProgress = LessonProgress(
        lessonId = lessonId,
        lessonTitle = lessonTitle,
        difficulty = difficulty,
        completed = completed,
        totalPracticedSeconds = totalPracticedSeconds,
        lastPracticedTimestamp = lastPracticedTimestamp
    )
}
