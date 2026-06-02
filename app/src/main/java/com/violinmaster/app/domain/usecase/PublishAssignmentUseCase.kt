package com.violinmaster.app.domain.usecase

import com.violinmaster.app.data.Assignment
import com.violinmaster.app.data.IPracticeRepository
import com.violinmaster.app.di.AuthManager
import com.violinmaster.app.security.VideoSecurityService

/**
 * Creates and publishes a new assignment.
 *
 * Reads the teacher code from [AuthManager] and generates a secure video URL.
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
     * @return The created [Assignment].
     */
    suspend operator fun invoke(
        title: String,
        description: String,
        targetStudent: String,
        videoTitle: String,
        durationSeconds: Int
    ): Assignment {
        val userVal = authManager.currentUser.value
            ?: throw IllegalStateException("No authenticated user")

        val secureVideoUrl = if (videoTitle.isNotEmpty()) {
            VideoSecurityService.obtainSecureSignedUrl(
                "vid_dynamic_tutor_" + System.currentTimeMillis() % 1000,
                "session_token_master"
            )
        } else ""

        val assignment = Assignment(
            title = title,
            description = description,
            teacherUsername = userVal.teacherCode,
            studentUsername = targetStudent,
            videoTitle = videoTitle,
            videoDurationSeconds = durationSeconds,
            videoResourceUrl = secureVideoUrl
        )
        repository.insertAssignment(assignment)
        return assignment
    }
}
