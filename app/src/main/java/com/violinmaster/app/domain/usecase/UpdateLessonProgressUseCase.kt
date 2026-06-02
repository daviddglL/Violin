package com.violinmaster.app.domain.usecase

import com.violinmaster.app.data.IPracticeRepository

/**
 * Updates lesson progress with additional practice seconds.
 *
 * Marks the lesson as completed when totalPracticedSeconds >= 300.
 * Returns whether the lesson was newly completed (for point awards).
 *
 * No Android imports — delegating to [IPracticeRepository].
 */
class UpdateLessonProgressUseCase constructor(
    private val repository: IPracticeRepository
) {
    /**
     * Appends practice time and checks completion threshold.
     *
     * @param lessonId The lesson to update.
     * @param additionalSeconds Seconds to add to total.
     * @return `true` if the lesson was newly completed by this update.
     */
    suspend operator fun invoke(lessonId: String, additionalSeconds: Int): Boolean {
        val current = repository.getLessonProgressById(lessonId) ?: return false
        val finalSecs = current.totalPracticedSeconds + additionalSeconds
        val wasCompletedAlready = current.completed
        val markedDone = finalSecs >= 300

        repository.insertLessonProgress(
            current.copy(
                totalPracticedSeconds = finalSecs,
                completed = markedDone || current.completed,
                lastPracticedTimestamp = System.currentTimeMillis()
            )
        )

        return markedDone && !wasCompletedAlready
    }
}
