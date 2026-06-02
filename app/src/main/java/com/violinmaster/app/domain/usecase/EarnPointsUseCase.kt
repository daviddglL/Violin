package com.violinmaster.app.domain.usecase

import com.violinmaster.app.data.IPracticeRepository
import com.violinmaster.app.di.AuthManager

/**
 * Adds points to the currently authenticated user.
 *
 * Reads the current user from [AuthManager], increments their points,
 * and persists via both repository and auth manager.
 *
 * No Android imports — delegating to [IPracticeRepository] and [AuthManager].
 */
class EarnPointsUseCase constructor(
    private val repository: IPracticeRepository,
    private val authManager: AuthManager
) {
    /**
     * Adds [additionalPoints] to the current user's total.
     *
     * No-op when no user is authenticated.
     */
    suspend operator fun invoke(additionalPoints: Int) {
        val userVal = authManager.currentUser.value ?: return
        val updatedUser = userVal.copy(points = userVal.points + additionalPoints)
        repository.insertUser(updatedUser)
        authManager.restoreCurrentUser(updatedUser)
    }
}
