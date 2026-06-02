package com.violinmaster.app.domain.usecase

import com.violinmaster.app.data.IPracticeRepository
import com.violinmaster.app.data.PracticeSession

/**
 * Saves a practice session to the repository.
 *
 * No Android imports — delegating to [IPracticeRepository].
 */
class SavePracticeSessionUseCase constructor(
  private val repository: IPracticeRepository
) {
  /**
   * Persists a completed practice session.
   *
   * @param session The session data to save.
   */
  suspend operator fun invoke(session: PracticeSession) {
    repository.insertSession(session)
  }
}
