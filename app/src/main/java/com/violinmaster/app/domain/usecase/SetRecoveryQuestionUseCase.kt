package com.violinmaster.app.domain.usecase

import com.violinmaster.app.data.IPracticeRepository
import com.violinmaster.app.data.UserAccount
import com.violinmaster.app.domain.model.RecoveryQuestion
import com.violinmaster.app.domain.util.Base64Encoder
import com.violinmaster.app.security.SecurityUtils
import java.util.Arrays

/**
 * Sets (or overwrites) a security question and hashed answer for a user.
 *
 * Hashes the answer using the same salt+SHA-256 algorithm as PIN hashing
 * (SecurityUtils.hashPasscode). Stores the question key and hashed answer
 * in the UserAccount entity.
 *
 * REQ-PINREC-001: Security question setup with same hashing as PIN.
 */
class SetRecoveryQuestionUseCase(
    private val repository: IPracticeRepository
) {
    /**
     * Sets the recovery question and answer for a user.
     *
     * @param username The user to set recovery for.
     * @param question The selected security question.
     * @param answer The plain-text answer to hash and store.
     * @return true if the user was found and updated, false otherwise.
     */
    suspend operator fun invoke(
        username: String,
        question: RecoveryQuestion,
        answer: String
    ): Boolean {
        val user = repository.getUserByUsername(username) ?: return false
        if (answer.isBlank()) return false

        val salt = SecurityUtils.generateSalt()
        val answerChars = answer.toCharArray()
        val hash = SecurityUtils.hashPasscode(answerChars, salt)
        Arrays.fill(answerChars, '0')

        val updated = user.copy(
            securityQuestion = question.questionKey,
            securityAnswerSalt = Base64Encoder.encodeToString(salt),
            securityAnswerHash = hash
        )
        repository.insertUser(updated)
        return true
    }
}
