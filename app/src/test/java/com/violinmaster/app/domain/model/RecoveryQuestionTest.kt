package com.violinmaster.app.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for RecoveryQuestion enum (REQ-PINREC-001: 5 preset questions).
 */
class RecoveryQuestionTest {

    @Test
    fun `enum has exactly 5 preset questions`() {
        assertEquals(5, RecoveryQuestion.entries.size)
    }

    @Test
    fun `FIRST_PET has correct question key`() {
        assertEquals("recovery_q_first_pet", RecoveryQuestion.FIRST_PET.questionKey)
    }

    @Test
    fun `BIRTH_CITY has correct question key`() {
        assertEquals("recovery_q_birth_city", RecoveryQuestion.BIRTH_CITY.questionKey)
    }

    @Test
    fun `FAVORITE_TEACHER has correct question key`() {
        assertEquals("recovery_q_favorite_teacher", RecoveryQuestion.FAVORITE_TEACHER.questionKey)
    }

    @Test
    fun `CHILDHOOD_NICKNAME has correct question key`() {
        assertEquals("recovery_q_childhood_nickname", RecoveryQuestion.CHILDHOOD_NICKNAME.questionKey)
    }

    @Test
    fun `FIRST_INSTRUMENT has correct question key`() {
        assertEquals("recovery_q_first_instrument", RecoveryQuestion.FIRST_INSTRUMENT.questionKey)
    }

    @Test
    fun `all question keys are unique`() {
        val keys = RecoveryQuestion.entries.map { it.questionKey }
        assertEquals(keys.size, keys.toSet().size)
    }

    @Test
    fun `questionKey is a non-empty string`() {
        for (entry in RecoveryQuestion.entries) {
            assertTrue("questionKey for ${entry.name} must not be blank", entry.questionKey.isNotBlank())
        }
    }
}
