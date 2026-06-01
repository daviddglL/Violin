package com.violinmaster.app.domain.usecase

import com.violinmaster.app.data.IPracticeRepository
import com.violinmaster.app.data.UserAccount
import com.violinmaster.app.domain.util.Base64Encoder
import com.violinmaster.app.security.SecurityUtils
import java.time.LocalDate
import java.util.Arrays
import javax.inject.Inject

/**
 * Registers a new user account.
 *
 * Validates username, pin, birth year, and uniqueness before creating.
 * No Android imports — delegating to [IPracticeRepository].
 *
 * REQ-ARCH-001: Pure-Kotlin use case with no Android SDK dependencies.
 */
class RegisterUseCase @Inject constructor(
  private val repository: IPracticeRepository
) {
  /**
   * Creates and persists a new user account.
   *
   * @param username Non-blank, unique username.
   * @param pin Exactly 4-digit pin.
   * @param role User role (STUDENT, TEACHER, FREELANCER).
   * @param birthYear Valid birth year (1900..current year).
   * @return Created [UserAccount] or null if validation fails.
   */
  suspend operator fun invoke(username: String, pin: String, role: String, birthYear: Int = 0): UserAccount? {
    if (username.isBlank()) return null
    if (pin.length != 4) return null
    val currentYear = LocalDate.now().year
    if (birthYear < 1900 || birthYear > currentYear) return null

    val existing = repository.getUserByUsername(username)
    if (existing != null) return null

    val salt = SecurityUtils.generateSalt()
    val passChars = pin.toCharArray()
    val hashed = SecurityUtils.hashPasscode(passChars, salt)
    Arrays.fill(passChars, '0')

    val inviteCode = if (role == "TEACHER") "TEACH-${(1000..9999).random()}" else ""

    val account = UserAccount(
      username = username,
      role = role,
      hashedPassword = hashed,
      salt = Base64Encoder.encodeToString(salt),
      teacherCode = inviteCode,
      birthYear = birthYear
    )

    repository.insertUser(account)
    return account
  }
}
