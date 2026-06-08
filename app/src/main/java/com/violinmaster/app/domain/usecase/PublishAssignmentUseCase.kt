package com.violinmaster.app.domain.usecase

import com.violinmaster.app.data.Assignment
import com.violinmaster.app.data.IPracticeRepository
import com.violinmaster.app.di.AuthManager

/**
 * Creates and publishes a new assignment.
 *
 * Reads the teacher code from [AuthManager] and stores the real video URL
 * from the upload pipeline directly. No longer generates fake signed URLs
 * via [com.violinmaster.app.security.VideoSecurityService].
 * No Android imports — delegating to [IPracticeRepository] and [AuthManager].
 */
class PublishAssignmentUseCase constructor(
    private val repository: IPracticeRepository,
    private val authManager: AuthManager
) {
    /**
     * Creates an [Assignment] and persists it via the repository.
     *
     * @param title Assignment title.
     * @param description Assignment description.
     * @param targetStudent Target student username (or "ALL").
     * @param videoTitle Optional video title.
     * @param durationSeconds Video duration in seconds.
     * @param videoUrl Real video URL from the upload pipeline (Firebase Storage download URL).
     * @return The created [Assignment].
     */
    suspend operator fun invoke(
        title: String,
        description: String,
        targetStudent: String,
        videoTitle: String,
        durationSeconds: Int,
        videoUrl: String = ""
    ): Assignment {
        val userVal = authManager.currentUser.value
            ?: throw IllegalStateException("No authenticated user")

        val assignment = Assignment(
            title = title,
            description = description,
            teacherUsername = userVal.teacherCode,
            studentUsername = targetStudent,
            videoTitle = videoTitle,
            videoDurationSeconds = durationSeconds,
            videoResourceUrl = videoUrl
        )
        repository.insertAssignment(assignment)
        return assignment
    }
}
