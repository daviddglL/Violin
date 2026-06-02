package com.violinmaster.app.domain.usecase

import com.violinmaster.app.data.IPracticeRepository
import com.violinmaster.app.data.PracticeSession
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Retrieves practice sessions filtered by date range.
 *
 * No Android imports — delegating to [IPracticeRepository].
 */
class GetPracticeSessionsUseCase constructor(
  private val repository: IPracticeRepository
) {
  /**
   * Returns a Flow of sessions within [startDate] and [endDate] inclusive.
   *
   * @param startDate Start date string (YYYY-MM-DD).
   * @param endDate End date string (YYYY-MM-DD).
   */
  operator fun invoke(startDate: String, endDate: String): Flow<List<PracticeSession>> {
    return repository.allSessions.map { sessions ->
      sessions.filter { it.dateString in startDate..endDate }
    }
  }
}
