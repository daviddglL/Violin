package com.violinmaster.app.domain.usecase

import com.violinmaster.app.data.IPracticeRepository
import com.violinmaster.app.data.UserAccount
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Returns users sorted by points descending (leaderboard).
 *
 * No Android imports — delegating to [IPracticeRepository].
 */
class GetLeaderboardUseCase @Inject constructor(
  private val repository: IPracticeRepository
) {
  /**
   * Returns a Flow of all users sorted by points in descending order.
   */
  operator fun invoke(): Flow<List<UserAccount>> {
    return repository.allUsers.map { users ->
      users.sortedByDescending { it.points }
    }
  }
}
