package com.violinmaster.app.domain.usecase

import com.violinmaster.app.data.IPracticeRepository
import com.violinmaster.app.di.AuthManager
import com.violinmaster.app.domain.util.ScoringPolicy

/**
 * Toggles the completion status of a lesson and awards points on first completion.
 *
 * Reads the current lesson from [IPracticeRepository], updates its completion status,
 * and awards [ScoringPolicy.LESSON_COMPLETION_POINTS] if the lesson is newly completed.
 *
 * No Android imports — delegating to [IPracticeRepository] and [AuthManager].
 */
class ToggleLessonStatusUseCase constructor(
    private val repository: IPracticeRepository,
    private val authManager: AuthManager
) {
    /**
     * Sets the lesson completion status and conditionally awards points.
     *
     * @param lessonId The lesson to update.
     * @param completed Whether the lesson should be marked completed.
     */
    suspend operator fun invoke(lessonId: String, completed: Boolean) {
        val current = repository.getLessonProgressById(lessonId)
        repository.updateLessonCompletion(lessonId, completed)
        if (completed && current?.completed == false) {
            val userVal = authManager.currentUser.value ?: return
            val updatedUser = userVal.copy(
                points = userVal.points + ScoringPolicy.LESSON_COMPLETION_POINTS
            )
            repository.insertUser(updatedUser)
            authManager.restoreCurrentUser(updatedUser)
        }
    }
}
