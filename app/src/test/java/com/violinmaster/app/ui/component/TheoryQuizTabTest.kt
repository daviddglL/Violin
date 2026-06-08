package com.violinmaster.app.ui.component

import com.violinmaster.app.domain.model.Instrument
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * MI-002: TheoryQuizTab instrument-specific quiz banks.
 *
 * Verifies that quizBanks is keyed by Instrument enum and contains
 * instrument-specific Q1 (tuning) and Q2 (fingering) questions,
 * plus shared general-theory Q3-Q5 for each instrument.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class TheoryQuizTabTest {

    // ═══════════════════════════════════════════════════════════════
    // MI-002: Structure — each instrument has a 5-question bank
    // ═══════════════════════════════════════════════════════════════

    @Test
    fun `MI-002 - quizBanks has entries for all 4 instruments`() {
        assertNotNull("quizBanks must have VIOLIN key", quizBanks[Instrument.VIOLIN])
        assertNotNull("quizBanks must have VIOLA key", quizBanks[Instrument.VIOLA])
        assertNotNull("quizBanks must have CELLO key", quizBanks[Instrument.CELLO])
        assertNotNull("quizBanks must have DOUBLE_BASS key", quizBanks[Instrument.DOUBLE_BASS])
        assertEquals("quizBanks must have exactly 4 instrument entries", 4, quizBanks.size)
    }

    @Test
    fun `MI-002 - each instrument quiz bank has exactly 5 questions`() {
        for (instrument in Instrument.values()) {
            val bank = quizBanks[instrument]
            assertNotNull("Bank must not be null for $instrument", bank)
            assertEquals("$instrument must have 5 quiz questions", 5, bank!!.size)
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // MI-002(a): Instrument-specific Q1 (tuning) questions
    // ═══════════════════════════════════════════════════════════════

    @Test
    fun `MI-002a - VIOLIN Q1 asks about violin tuning G D A E`() {
        val q1 = quizBanks[Instrument.VIOLIN]!![0]
        assertTrue(
            "Violin Q1 must mention violin tuning",
            q1.question.contains("violin", ignoreCase = true)
        )
        assertTrue(
            "Violin Q1 must mention G3",
            q1.options[0].contains("G3") || q1.options[q1.correctAnswerIndex].contains("G3")
        )
    }

    @Test
    fun `MI-002a - VIOLA Q1 asks about viola tuning C G D A`() {
        val q1 = quizBanks[Instrument.VIOLA]!![0]
        assertTrue(
            "Viola Q1 must mention viola",
            q1.question.contains("viola", ignoreCase = true)
        )
        // Correct answer (index 0) should have C3 as lowest string
        val correctAnswer = q1.options[q1.correctAnswerIndex]
        assertTrue(
            "Viola Q1 correct answer must include C3 (lowest string)",
            correctAnswer.contains("C3")
        )
    }

    @Test
    fun `MI-002a - CELLO Q1 asks about cello tuning C2 G2 D3 A3`() {
        val q1 = quizBanks[Instrument.CELLO]!![0]
        assertTrue(
            "Cello Q1 must mention cello",
            q1.question.contains("cello", ignoreCase = true)
        )
        val correctAnswer = q1.options[q1.correctAnswerIndex]
        assertTrue(
            "Cello Q1 correct answer must include C2 (lowest string)",
            correctAnswer.contains("C2")
        )
        assertTrue(
            "Cello Q1 correct answer must include A3 (highest string)",
            correctAnswer.contains("A3")
        )
    }

    @Test
    fun `MI-002a - DOUBLE_BASS Q1 asks about bass tuning E1 A1 D2 G2`() {
        val q1 = quizBanks[Instrument.DOUBLE_BASS]!![0]
        assertTrue(
            "Bass Q1 must mention double bass or bass",
            q1.question.contains("bass", ignoreCase = true) ||
                q1.question.contains("double bass", ignoreCase = true)
        )
        val correctAnswer = q1.options[q1.correctAnswerIndex]
        assertTrue(
            "Bass Q1 correct answer must include E1 (lowest string)",
            correctAnswer.contains("E1")
        )
    }

    // ═══════════════════════════════════════════════════════════════
    // MI-002(b): Instrument-specific Q2 (fingering) questions
    // ═══════════════════════════════════════════════════════════════

    @Test
    fun `MI-002b - VIOLIN Q2 asks about violin D-string fingering`() {
        val q2 = quizBanks[Instrument.VIOLIN]!![1]
        assertTrue(
            "Violin Q2 should be about fingering/position",
            q2.question.contains("D string", ignoreCase = true) ||
                q2.question.contains("finger", ignoreCase = true) ||
                q2.question.contains("Position", ignoreCase = true)
        )
    }

    @Test
    fun `MI-002b - VIOLA Q2 asks about viola fingering`() {
        val q2 = quizBanks[Instrument.VIOLA]!![1]
        assertTrue(
            "Viola Q2 should mention C string or viola fingering",
            q2.question.contains("C string", ignoreCase = true) ||
                q2.question.contains("viola", ignoreCase = true)
        )
    }

    @Test
    fun `MI-002b - CELLO Q2 asks about cello fingering`() {
        val q2 = quizBanks[Instrument.CELLO]!![1]
        assertTrue(
            "Cello Q2 should mention C string or cello fingering",
            q2.question.contains("C string", ignoreCase = true) ||
                q2.question.contains("cello", ignoreCase = true)
        )
    }

    @Test
    fun `MI-002b - DOUBLE_BASS Q2 asks about bass fingering`() {
        val q2 = quizBanks[Instrument.DOUBLE_BASS]!![1]
        assertTrue(
            "Bass Q2 should mention E string or bass fingering",
            q2.question.contains("E string", ignoreCase = true) ||
                q2.question.contains("bass", ignoreCase = true)
        )
    }

    // ═══════════════════════════════════════════════════════════════
    // Q3–Q5 are shared general theory across all instruments
    // ═══════════════════════════════════════════════════════════════

    @Test
    fun `MI-002 - Q3 is vibrato theory question for all instruments`() {
        for (instrument in Instrument.values()) {
            val q3 = quizBanks[instrument]!![2]
            assertTrue(
                "$instrument Q3 should mention vibrato",
                q3.question.contains("vibrato", ignoreCase = true) ||
                    q3.question.contains("pulsating", ignoreCase = true) ||
                    q3.options.any { it.contains("Vibrato", ignoreCase = true) }
            )
        }
    }

    @Test
    fun `MI-002 - Q4 is octave-check theory for all instruments`() {
        for (instrument in Instrument.values()) {
            val q4 = quizBanks[instrument]!![3]
            assertTrue(
                "$instrument Q4 should mention octave or intonation",
                q4.question.contains("octave", ignoreCase = true) ||
                    q4.question.contains("intonation", ignoreCase = true)
            )
        }
    }

    @Test
    fun `MI-002 - Q5 is Martelé bowing theory for all instruments`() {
        for (instrument in Instrument.values()) {
            val q5 = quizBanks[instrument]!![4]
            assertTrue(
                "$instrument Q5 should mention Martelé or bowing",
                q5.options.any { it.contains("Martelé", ignoreCase = true) }
            )
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // Backwards compat: violin quiz matches original quizQuestions
    // ═══════════════════════════════════════════════════════════════

    @Test
    fun `VIOLIN quiz bank Q1 correctAnswerIndex matches original violintuning answer`() {
        val q1 = quizBanks[Instrument.VIOLIN]!![0]
        // Original violin question had correctAnswerIndex = 0
        assertEquals("Violin Q1 should have correctAnswerIndex 0", 0, q1.correctAnswerIndex)
    }

    @Test
    fun `VIOLIN quiz bank Q2 correctAnswerIndex matches original fingering answer`() {
        val q2 = quizBanks[Instrument.VIOLIN]!![1]
        // Original F♯4 fingering question had correctAnswerIndex = 1
        assertEquals("Violin Q2 should have correctAnswerIndex 1", 1, q2.correctAnswerIndex)
    }
}
