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
     * Sets the current user's skill level to [level].
     *
     * No-op when no user is authenticated.
     */
    suspend operator fun invoke(level: String) {
        val userVal = authManager.currentUser.value ?: return
        val updatedUser = userVal.copy(skillLevel = level)
        repository.insertUser(updatedUser)
        authManager.restoreCurrentUser(updatedUser)
    }
}
