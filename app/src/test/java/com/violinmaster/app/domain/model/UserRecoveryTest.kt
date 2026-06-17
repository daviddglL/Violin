package com.violinmaster.app.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for User domain model recovery fields (REQ-PINREC-001).
 */
class UserRecoveryTest {

    @Test
    fun `User has securityQuestion field with empty default`() {
        val user = User(username = "test", role = "STUDENT")
        assertEquals("", user.securityQuestion)
    }

    @Test
    fun `User has securityAnswerHash field with empty default`() {
        val user = User(username = "test", role = "STUDENT")
        assertEquals("", user.securityAnswerHash)
    }

    @Test
    fun `User has securityAnswerSalt field with empty default`() {
        val user = User(username = "test", role = "STUDENT")
        assertEquals("", user.securityAnswerSalt)
    }

    @Test
    fun `User can be constructed with explicit recovery fields`() {
        val user = User(
            username = "recovery_user",
            role = "TEACHER",
            securityQuestion = "recovery_q_first_pet",
            securityAnswerHash = "hash123",
            securityAnswerSalt = "salt456"
        )
        assertEquals("recovery_q_first_pet", user.securityQuestion)
        assertEquals("hash123", user.securityAnswerHash)
        assertEquals("salt456", user.securityAnswerSalt)
    }

    @Test
    fun `User with recovery fields still works with isMinor`() {
        val user = User(
            username = "minor_user",
            role = "STUDENT",
            birthYear = 2015,
            securityQuestion = "recovery_q_birth_city",
            securityAnswerHash = "abc",
            securityAnswerSalt = "def"
        )
        assertTrue(user.isMinor)
    }

    @Test
    fun `User with recovery fields preserves backward compatibility`() {
        // Legacy constructor (no recovery fields) should work
        val user = User(username = "legacy", role = "FREELANCER", points = 100)
        assertEquals("legacy", user.username)
        assertEquals("FREELANCER", user.role)
        assertEquals(100, user.points)
        assertEquals("", user.securityQuestion)
        assertEquals("", user.securityAnswerHash)
        assertEquals("", user.securityAnswerSalt)
        assertFalse(user.isMinor)
    }
}
