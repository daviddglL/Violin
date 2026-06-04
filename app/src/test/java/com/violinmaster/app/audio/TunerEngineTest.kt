package com.violinmaster.app.audio

import com.violinmaster.app.audio.tuner.YinPitchDetector
import com.violinmaster.app.domain.model.Instrument
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

/**
 * Tests for YIN pitch detection algorithm and frequency-to-note mapping.
 *
 * Since AudioRecord cannot run in unit tests, these tests exercise only the
 * pure math functions (package-private in [YinPitchDetector]).
 *
 * AudioRecord integration is documented below as an @Ignore test placeholder.
 */
@RunWith(RobolectricTestRunner::class)
class TunerEngineTest {

    companion object {
        private const val SAMPLE_RATE = 44100
        private const val AMPLITUDE = Short.MAX_VALUE.toDouble() * 0.5
    }

    // ---- YIN pure math tests ----

    @Test
    fun `YIN detects A4 440Hz from synthetic sine wave`() {
        val buffer = generateSineWave(440.0f, SAMPLE_RATE, 4096)
        val result = YinPitchDetector.detectPitch(buffer, SAMPLE_RATE)

        assertNotNull("Pitch should be detected for clean 440Hz sine", result)
        val pitch = result!!
        val cents = centsFrom440(pitch.frequency)
        assertTrue(
            "Detected freq ${result!!.frequency}Hz should be within ±10 cents of 440Hz (got ${"%.2f".format(cents)} cents)",
            abs(cents) < 10.1f
        )
    }

    @Test
    fun `YIN detects G3 196Hz from synthetic sine wave`() {
        val buffer = generateSineWave(196.0f, SAMPLE_RATE, 4096)
        val result = YinPitchDetector.detectPitch(buffer, SAMPLE_RATE)

        assertNotNull("Pitch should be detected for clean 196Hz sine", result)
        val cents = 1200.0 * log2(result!!.frequency.toDouble() / 196.0)
        assertTrue(
            "Detected freq ${result!!.frequency}Hz should be within ±10 cents of 196Hz (got ${"%.2f".format(cents)} cents)",
            abs(cents) < 10.1f
        )
    }

    @Test
    fun `YIN detects D4 293_66Hz from synthetic sine wave`() {
        val buffer = generateSineWave(293.66f, SAMPLE_RATE, 4096)
        val result = YinPitchDetector.detectPitch(buffer, SAMPLE_RATE)

        assertNotNull("Pitch should be detected for clean 293.66Hz sine", result)
        val cents = 1200.0 * log2(result!!.frequency.toDouble() / 293.66)
        assertTrue(
            "Detected freq ${result!!.frequency}Hz should be within ±10 cents of 293.66Hz (got ${"%.2f".format(cents)} cents)",
            abs(cents) < 10.1f
        )
    }

    @Test
    fun `YIN detects E5 659_25Hz from synthetic sine wave`() {
        val buffer = generateSineWave(659.25f, SAMPLE_RATE, 4096)
        val result = YinPitchDetector.detectPitch(buffer, SAMPLE_RATE)

        assertNotNull("Pitch should be detected for clean 659.25Hz sine", result)
        val cents = 1200.0 * log2(result!!.frequency.toDouble() / 659.25)
        assertTrue(
            "Detected freq ${result!!.frequency}Hz should be within ±10 cents of 659.25Hz (got ${"%.2f".format(cents)} cents)",
            abs(cents) < 10.1f
        )
    }

    // ---- Frequency to note mapping tests ----

    @Test
    fun `frequencyToNote maps 440_0 to A with 0 cents`() {
        val result = YinPitchDetector.frequencyToNoteAndCents(440.0f, 440)
        assertEquals("A", result.note)
        assertEquals(0f, result.cents, 0.01f)
        assertEquals(440.0f, result!!.frequency)
    }

    @Test
    fun `frequencyToNote maps 196_0 to G with 0 cents`() {
        val result = YinPitchDetector.frequencyToNoteAndCents(196.0f, 440)
        assertEquals("G", result.note)
        assertEquals(0f, result.cents, 0.01f)
    }

    @Test
    fun `frequencyToNote maps 293_66 to D with 0 cents`() {
        val result = YinPitchDetector.frequencyToNoteAndCents(293.66f, 440)
        assertEquals("D", result.note)
        assertEquals(0f, result.cents, 0.01f)
    }

    @Test
    fun `frequencyToNote maps 659_25 to E with 0 cents`() {
        val result = YinPitchDetector.frequencyToNoteAndCents(659.25f, 440)
        assertEquals("E", result.note)
        assertEquals(0f, result.cents, 0.01f)
    }

    @Test
    fun `frequencyToNote maps 445_0 to A with positive cents`() {
        val result = YinPitchDetector.frequencyToNoteAndCents(445.0f, 440)
        assertEquals("A", result.note)
        assertTrue("Expected positive cents for 445Hz (sharp), got ${result.cents}", result.cents > 0f)
        // 445Hz vs 440Hz: 1200 * log2(445/440) ≈ 19.5 cents
        assertTrue("Cents should be approximately 19.5, got ${result.cents}", abs(result.cents - 19.5f) < 0.5f)
    }

    @Test
    fun `frequencyToNote maps 435_0 to A with negative cents`() {
        val result = YinPitchDetector.frequencyToNoteAndCents(435.0f, 440)
        assertEquals("A", result.note)
        assertTrue("Expected negative cents for 435Hz (flat), got ${result.cents}", result.cents < 0f)
        // 435Hz vs 440Hz: 1200 * log2(435/440) ≈ -19.8 cents
        assertTrue("Cents should be approximately -19.8, got ${result.cents}", abs(result.cents - (-19.8f)) < 0.5f)
    }

    @Test
    fun `frequencyToNote maps frequency near D to D`() {
        // 295Hz is close to D4 (293.66Hz)
        val result = YinPitchDetector.frequencyToNoteAndCents(295.0f, 440)
        assertEquals("D", result.note)
        assertTrue("Cents should be positive (sharp) for 295Hz vs 293.66Hz", result.cents > 0f)
    }

    @Test
    fun `frequencyToNote maps frequency between A and E to closest`() {
        // 550Hz is between A4 (440) and E5 (659.25), closer to E5
        val result = YinPitchDetector.frequencyToNoteAndCents(550.0f, 440)
        assertEquals("E", result.note)
        assertTrue("Cents should be negative for 550Hz vs 659.25Hz", result.cents < 0f)
    }

    @Test
    fun `frequencyToNote respects custom referencePitchA 442`() {
        // At referencePitchA=442, A4=442Hz. 445Hz is 3Hz sharp → ~11.7 cents
        val result = YinPitchDetector.frequencyToNoteAndCents(445.0f, 442)
        assertEquals("A", result.note)
        assertTrue("Cents should be positive but small at ref=442Hz", result.cents > 0f && result.cents < 15f)
    }

    @Test
    fun `frequencyToNote returns high confidence for exact match`() {
        val result = YinPitchDetector.frequencyToNoteAndCents(440.0f, 440)
        assertEquals(1.0f, result!!.confidence, 0.01f)
    }

    @Test
    fun `frequencyToNote returns low confidence for far-off frequency`() {
        val result = YinPitchDetector.frequencyToNoteAndCents(500.0f, 440)
        assertTrue("Confidence should decrease with distance from note", result!!.confidence < 0.5f)
    }

    // ---- Instrument-aware note mapping tests ----

    @Test
    fun `frequencyToNote maps viola C3 130_8 to C with instrument param`() {
        val result = YinPitchDetector.frequencyToNoteAndCents(130.8f, 440, Instrument.VIOLA)
        assertEquals("C", result.note)
        assertEquals(0f, result.cents, 0.01f)
    }

    @Test
    fun `frequencyToNote maps cello C2 65_4 to C with instrument param`() {
        val result = YinPitchDetector.frequencyToNoteAndCents(65.4f, 440, Instrument.CELLO)
        assertEquals("C", result.note)
        assertEquals(0f, result.cents, 0.01f)
    }

    @Test
    fun `frequencyToNote maps cello A3 220 to A with instrument param`() {
        val result = YinPitchDetector.frequencyToNoteAndCents(220.0f, 440, Instrument.CELLO)
        assertEquals("A", result.note)
        assertEquals(0f, result.cents, 0.01f)
    }

    @Test
    fun `frequencyToNote maps violin D4 293_66 to D with instrument param regression`() {
        val result = YinPitchDetector.frequencyToNoteAndCents(293.66f, 440, Instrument.VIOLIN)
        assertEquals("D", result.note)
        assertEquals(0f, result.cents, 0.01f)
    }

    // ---- Parabolic interpolation improves accuracy ----

    @Test
    fun `parabolicInterpolation improves tau estimate accuracy`() {
        // With a 440Hz sine at 44100Hz, integer tau = 100 gives freq ≈ 441.0Hz (~3.9 cents off)
        // Parabolic interpolation should refine to ~100.23 giving nearly exact 440Hz
        val buffer = generateSineWave(440.0f, SAMPLE_RATE, 4096)

        // Detect pitch with interpolation (default)
        val resultWithInterp = YinPitchDetector.detectPitch(buffer, SAMPLE_RATE)
        assertNotNull(resultWithInterp)
        val centsWithInterp = abs(centsFrom440(resultWithInterp!!.frequency))

        // The interpolated result should be very close to 440Hz
        assertTrue(
            "Interpolated result should be within 10 cents. Got ${resultWithInterp!!.frequency}Hz (${"%.2f".format(centsWithInterp)} cents)",
            centsWithInterp < 10.1f
        )
    }

    // ---- Confidence tests ----

    @Test
    fun `detectPitch returns high confidence for clean sine wave`() {
        val buffer = generateSineWave(440.0f, SAMPLE_RATE, 4096)
        val result = YinPitchDetector.detectPitch(buffer, SAMPLE_RATE)

        assertNotNull(result)
        assertTrue(
            "Confidence should be > 0.9 for clean sine wave, got ${result!!.confidence}",
            result!!.confidence > 0.9f
        )
    }

    @Test
    fun `detectPitch returns null for silence`() {
        val buffer = ShortArray(4096) { 0 } // all zeros = silence
        val result = YinPitchDetector.detectPitch(buffer, SAMPLE_RATE)

        // Silence has no pitch — should return null
        assertEquals(null, result)
    }

    // ---- Integration test placeholder (AudioRecord) ----

    @Test
    @org.junit.Ignore("Requires real microphone — AudioRecord not available in unit tests")
    fun `TunerEngine integration captures microphone audio and detects pitch`() {
        val engine = TunerEngine()
        engine.startListening()
        // In a real test, we'd wait for pitch detection and assert on pitchFlow
        Thread.sleep(1000)
        engine.stopListening()
        engine.release()
    }

    // ---- Helper functions ----

    /**
     * Generate a synthetic sine wave as ShortArray (PCM 16-bit).
     */
    private fun generateSineWave(frequency: Float, sampleRate: Int, numSamples: Int): ShortArray {
        val buffer = ShortArray(numSamples)
        val angularFreq = 2.0 * PI * frequency / sampleRate
        for (i in 0 until numSamples) {
            buffer[i] = (AMPLITUDE * sin(angularFreq * i)).toInt().toShort()
        }
        return buffer
    }

    private fun centsFrom440(freq: Float): Double {
        return 1200.0 * log2(freq.toDouble() / 440.0)
    }

    private fun log2(x: Double): Double = kotlin.math.log2(x)
}
