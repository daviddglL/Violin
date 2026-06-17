package com.violinmaster.app.domain.usecase

import com.violinmaster.app.data.IPracticeRepository
import com.violinmaster.app.domain.util.Base64Encoder
import com.violinmaster.app.security.SecurityUtils
import java.util.Arrays

/**
 * Verifies a user's recovery answer against the stored hash.
 *
 * Uses the same salt+SHA-256 algorithm (SecurityUtils.hashPasscode) to
 * compute the hash of the provided answer and compares it to the stored value.
 *
 * REQ-PINREC-002: Forgot PIN flow — security question verification.
 */
class VerifyRecoveryAnswerUseCase(
    private val repository: IPracticeRepository
) {
    /**
     * Verifies the recovery answer for a user.
     *
     * @param username The user to verify.
     * @param answer The plain-text answer to check.
     * @return true if the answer matches, false otherwise.
     */
    suspend operator fun invoke(username: String, answer: String): Boolean {
        val user = repository.getUserByUsername(username) ?: return false

        // User must have a recovery question configured
        if (user.securityQuestion.isBlank() ||
            user.securityAnswerSalt.isBlank() ||
            user.securityAnswerHash.isBlank()
        ) {
            return false
        }

        val salt = Base64Encoder.decode(user.securityAnswerSalt)
        val answerChars = answer.toCharArray()
        val computedHash = SecurityUtils.hashPasscode(answerChars, salt)
        Arrays.fill(answerChars, '0')

        return computedHash == user.securityAnswerHash
    }
}
