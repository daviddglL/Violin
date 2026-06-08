package com.violinmaster.app.domain.usecase

import com.violinmaster.app.data.IPracticeRepository
import com.violinmaster.app.di.AuthManager

/**
 * Updates the skill level of the currently authenticated user.
 *
 * Reads the current user from [AuthManager], updates their skill level,
 * and persists via both repository and auth manager.
 *
 * No Android imports — delegating to [IPracticeRepository] and [AuthManager].
 */
class UpdateSkillLevelUseCase constructor(
    private val repository: IPracticeRepository,
    private val authManager: AuthManager
) {
    /**
     * Sets the current user's skill level to [level] if the quiz score gate passes.
     *
     * @param level the target skill level
     * @param quizScore the quiz score (default -1 = backwards compat, bypasses gate).
     *        Score >= 80 is required to persist the level update.
     *
     * No-op when no user is authenticated or quiz score < 80.
     */
    suspend operator fun invoke(level: String, quizScore: Int = -1) {
        val userVal = authManager.currentUser.value ?: return
        if (quizScore < 80 && quizScore != -1) return
        val updatedUser = userVal.copy(skillLevel = level)
        repository.insertUser(updatedUser)
        authManager.restoreCurrentUser(updatedUser)
    }
}
