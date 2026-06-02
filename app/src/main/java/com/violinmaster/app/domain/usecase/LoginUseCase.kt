package com.violinmaster.app.domain.usecase

import com.violinmaster.app.data.IPracticeRepository
import com.violinmaster.app.data.UserAccount
import com.violinmaster.app.di.AuthManager
import com.violinmaster.app.domain.util.Base64Encoder
import com.violinmaster.app.security.SecurityUtils

/**
 * Authenticates a user with username and pin.
 *
 * No Android imports — delegating to [IPracticeRepository] and [AuthManager].
 *
 * REQ-ARCH-001-S1: Pure-Kotlin use case testable with JUnit + Mockito.
 */
class LoginUseCase constructor(
  private val repository: IPracticeRepository,
  private val authManager: AuthManager
) {
  /**
   * Validates credentials and returns [UserAccount] on success, null on failure.
   *
   * @param username Non-blank username.
   * @param pin 4-digit pin.
   * @return Authenticated user or null.
   */
  suspend operator fun invoke(username: String, pin: String): UserAccount? {
    if (username.isBlank() || pin.length != 4) return null

    val user = repository.getUserByUsername(username) ?: return null

    val saltBytes = Base64Encoder.decode(user.salt)
    val passChars = pin.toCharArray()
    val computedHash = SecurityUtils.hashPasscode(passChars, saltBytes)

    return if (computedHash == user.hashedPassword) {
      authManager.saveCurrentUser(user)
      user
    } else {
      null
    }
  }
}
