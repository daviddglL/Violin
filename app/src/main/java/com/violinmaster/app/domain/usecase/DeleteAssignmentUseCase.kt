package com.violinmaster.app.domain.usecase

import com.violinmaster.app.data.IPracticeRepository

/**
 * Deletes an assignment by its ID.
 *
 * No Android imports — delegating to [IPracticeRepository].
 */
class DeleteAssignmentUseCase constructor(
    private val repository: IPracticeRepository
) {
    /**
     * Deletes the assignment with the given [id].
     */
    suspend operator fun invoke(id: Int) {
        repository.deleteAssignmentById(id)
    }
}
