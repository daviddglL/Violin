package com.violinmaster.app.data

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Tests for UserAccount recovery fields — REQ-PINREC-006.
 */
class UserAccountRecoveryTest {

    @Test
    fun `UserAccount has securityQuestion field with empty default`() {
        val account = UserAccount(
            username = "test", role = "STUDENT",
            hashedPassword = "hash", salt = "salt"
        )
        assertEquals("", account.securityQuestion)
    }

    @Test
    fun `UserAccount has securityAnswerSalt field with empty default`() {
        val account = UserAccount(
            username = "test", role = "STUDENT",
            hashedPassword = "hash", salt = "salt"
        )
        assertEquals("", account.securityAnswerSalt)
    }

    @Test
    fun `UserAccount has securityAnswerHash field with empty default`() {
        val account = UserAccount(
            username = "test", role = "STUDENT",
            hashedPassword = "hash", salt = "salt"
        )
        assertEquals("", account.securityAnswerHash)
    }

    @Test
    fun `UserAccount can be constructed with explicit recovery fields`() {
        val account = UserAccount(
            username = "recovery_user",
            role = "TEACHER",
            hashedPassword = "hash",
            salt = "salt_pin",
            securityQuestion = "recovery_q_first_pet",
            securityAnswerSalt = "salt_ans",
            securityAnswerHash = "hash_ans"
        )
        assertEquals("recovery_q_first_pet", account.securityQuestion)
        assertEquals("salt_ans", account.securityAnswerSalt)
        assertEquals("hash_ans", account.securityAnswerHash)
    }

    @Test
    fun `UserAccount backward compatible — legacy constructor works`() {
        val account = UserAccount(
            username = "legacy", role = "FREELANCER",
            hashedPassword = "hash", salt = "salt",
            points = 200
        )
        assertEquals("legacy", account.username)
        assertEquals("FREELANCER", account.role)
        assertEquals(200, account.points)
        assertEquals("", account.securityQuestion)
        assertEquals("", account.securityAnswerSalt)
        assertEquals("", account.securityAnswerHash)
    }
}
