package com.violinmaster.app.domain.usecase

import com.violinmaster.app.data.IPracticeRepository
import com.violinmaster.app.data.UserAccount
import com.violinmaster.app.di.AuthManager
import com.violinmaster.app.domain.util.Base64Encoder
import com.violinmaster.app.security.SecurityUtils
import java.util.Arrays

/**
 * Resets a user's PIN after successful identity verification.
 *
 * Re-hashes the new PIN using the same salt+SHA-256 pattern and
 * auto-logs the user in after the reset.
 *
 * REQ-PINREC-004: Post-recovery PIN reset with auto-login.
 */
class ResetPinUseCase(
    private val repository: IPracticeRepository,
    private val authManager: AuthManager
) {
    /**
     * Resets the PIN for a user and auto-logs them in.
     *
     * @param username The user to reset the PIN for.
     * @param newPin The new 4-digit PIN.
     * @return true if the reset was successful, false otherwise.
     */
    suspend operator fun invoke(username: String, newPin: String): Boolean {
        if (newPin.length != 4) return false

        val user = repository.getUserByUsername(username) ?: return false

        val salt = SecurityUtils.generateSalt()
        val passChars = newPin.toCharArray()
        val hash = SecurityUtils.hashPasscode(passChars, salt)
        Arrays.fill(passChars, '0')

        val updated = user.copy(
            hashedPassword = hash,
            salt = Base64Encoder.encodeToString(salt)
        )
        repository.insertUser(updated)

        // Auto-login after reset
        authManager.saveCurrentUser(updated)
        return true
    }
}
