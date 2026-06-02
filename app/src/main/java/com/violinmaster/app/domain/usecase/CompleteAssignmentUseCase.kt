package com.violinmaster.app.domain.usecase

import com.violinmaster.app.data.IPracticeRepository
import com.violinmaster.app.di.AuthManager

/**
 * Marks an assignment as complete and awards points to the student.
 *
 * No Android imports — delegating to [IPracticeRepository] and [AuthManager].
 */
class CompleteAssignmentUseCase constructor(
  private val repository: IPracticeRepository,
  private val authManager: AuthManager
) {
  /**
   * Marks the assignment complete and awards 200 points.
   *
   * Points are only awarded if a current user is logged in.
   *
   * @param assignmentId The assignment ID to mark complete.
   */
  suspend operator fun invoke(assignmentId: Int) {
    repository.updateAssignmentCompletion(assignmentId, true)

    val user = authManager.currentUser.value ?: return
    val updated = user.copy(points = user.points + 200)
    repository.insertUser(updated)
    authManager.restoreCurrentUser(updated)
  }
}
