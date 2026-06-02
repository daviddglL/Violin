package com.violinmaster.app.domain.usecase

import com.violinmaster.app.data.IPracticeRepository

/**
 * Deletes a practice session by its ID.
 *
 * No Android imports — delegating to [IPracticeRepository].
 */
class DeletePracticeSessionUseCase constructor(
    private val repository: IPracticeRepository
) {
    /**
     * Deletes the session with the given [id].
     */
    suspend operator fun invoke(id: Int) {
        repository.deleteSession(id)
    }
}
