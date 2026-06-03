package com.violinmaster.app.audio.tuner

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * TDD tests for YinPitchDetector frequencyToNoteAndCents with configurable maxCents.
 *
 * RED phase: maxCents parameter and updated confidence formula do not exist yet.
 */
class YinPitchDetectorTest {

    // ═══════════════════════════════════════════════════════════════════
    // frequencyToNoteAndCents — maxCents confidence
    // ═══════════════════════════════════════════════════════════════════

    @Test
    fun `confidence uses default maxCents 50 — exact A4 gives confidence 1`() {
        // A4 = 440 Hz at referencePitchA = 440 → 0 cents offset
        val result = YinPitchDetector.frequencyToNoteAndCents(
            frequency = 440f,
            referencePitchA = 440
        )
        assertEquals("A", result.note)
        assertEquals(0f, result.cents, 0.1f)
        assertEquals(1.0f, result.confidence, 0.01f)
        assertEquals(50, result.maxCents)
    }

    @Test
    fun `confidence scales with custom maxCents — 25 cents at maxCents 25 gives 0`() {
        // At exactly maxCents offset, confidence should be 0
        // 25 cents sharp from A4 = 440 * 2^(25/1200) ≈ 446.38 Hz
        // Actually let's find a frequency that gives ~25 cents
        // 25 cents = 1200 * log2(f/440) → f = 440 * 2^(25/1200) ≈ 446.38
        val freq25cents = 440f * Math.pow(2.0, 25.0 / 1200.0).toFloat()
        val result = YinPitchDetector.frequencyToNoteAndCents(
            frequency = freq25cents,
            referencePitchA = 440,
            maxCents = 25
        )
        assertEquals("A", result.note)
        assertEquals(25f, result.cents, 0.5f)
        assertEquals(0.0f, result.confidence, 0.05f)
        assertEquals(25, result.maxCents)
    }

    @Test
    fun `confidence scales with custom maxCents — 50 cents at maxCents 100 gives 0_5`() {
        // 50 cents sharp from A4
        val freq50cents = 440f * Math.pow(2.0, 50.0 / 1200.0).toFloat()
        val result = YinPitchDetector.frequencyToNoteAndCents(
            frequency = freq50cents,
            referencePitchA = 440,
            maxCents = 100
        )
        assertEquals("A", result.note)
        assertEquals(50f, result.cents, 0.5f)
        // confidence = 1 - 50/100 = 0.5
        assertEquals(0.5f, result.confidence, 0.05f)
        assertEquals(100, result.maxCents)
    }

    @Test
    fun `confidence capped at 0 when cents exceed maxCents`() {
        // 60 cents offset with maxCents=50 should give confidence clamped to 0
        val freq60cents = 440f * Math.pow(2.0, 60.0 / 1200.0).toFloat()
        val result = YinPitchDetector.frequencyToNoteAndCents(
            frequency = freq60cents,
            referencePitchA = 440,
            maxCents = 50
        )
        assertEquals(60f, result.cents, 0.5f)
        assertEquals(0.0f, result.confidence, 0.01f)
        assertEquals(50, result.maxCents)
    }

    @Test
    fun `confidence capped at 1 when cents are negative and within range`() {
        // -10 cents with maxCents=50 → confidence = 1 - 10/50 = 0.8
        val freqMinus10cents = 440f * Math.pow(2.0, -10.0 / 1200.0).toFloat()
        val result = YinPitchDetector.frequencyToNoteAndCents(
            frequency = freqMinus10cents,
            referencePitchA = 440,
            maxCents = 50
        )
        assertEquals(-10f, result.cents, 0.5f)
        assertEquals(0.8f, result.confidence, 0.05f)
    }

    @Test
    fun `custom maxCents 200 — 100 cents offset gives confidence 0_5`() {
        val freq100cents = 440f * Math.pow(2.0, 100.0 / 1200.0).toFloat()
        val result = YinPitchDetector.frequencyToNoteAndCents(
            frequency = freq100cents,
            referencePitchA = 440,
            maxCents = 200
        )
        assertEquals(100f, result.cents, 0.5f)
        assertEquals(0.5f, result.confidence, 0.05f)
        assertEquals(200, result.maxCents)
    }

    @Test
    fun `detects G string note correctly with custom maxCents`() {
        // G3 ≈ 196 Hz at referencePitchA=440
        val result = YinPitchDetector.frequencyToNoteAndCents(
            frequency = 196f,
            referencePitchA = 440,
            maxCents = 100
        )
        assertEquals("G", result.note)
        assertEquals(100, result.maxCents)
        assertTrue(result.confidence > 0.5f)
    }

    @Test
    fun `backward compatible — default maxCents 50 matches old behavior`() {
        // 25 cents offset with default maxCents=50 → confidence = 1 - 25/50 = 0.5
        val freq25cents = 440f * Math.pow(2.0, 25.0 / 1200.0).toFloat()
        val result = YinPitchDetector.frequencyToNoteAndCents(
            frequency = freq25cents,
            referencePitchA = 440
            // maxCents defaults to 50
        )
        assertEquals(0.5f, result.confidence, 0.05f)
        assertEquals(50, result.maxCents)
    }
}
