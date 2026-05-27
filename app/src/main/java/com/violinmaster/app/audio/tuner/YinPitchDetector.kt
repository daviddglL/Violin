package com.violinmaster.app.audio.tuner

import com.violinmaster.app.audio.PitchResult
import kotlin.math.abs
import kotlin.math.log2

/**
 * Pure-function implementation of the YIN pitch detection algorithm.
 *
 * Based on "YIN, a fundamental frequency estimator for speech and music"
 * by Alain de Cheveigné and Hideki Kawahara (2002).
 *
 * All functions are internal (package-private) and have NO Android dependencies,
 * making them unit-testable with synthetic PCM data.
 *
 * @see TunerEngine for the AudioRecord-based wrapper.
 */
internal object YinPitchDetector {

    /** Violin string frequencies relative to A4=440Hz */
    private val VIOLIN_NOTE_RATIOS = mapOf(
        "G" to 196.00 / 440.00,
        "D" to 293.66 / 440.00,
        "A" to 1.0,
        "E" to 659.25 / 440.00
    )

    /**
     * Detect the fundamental frequency in a PCM buffer using the YIN algorithm.
     *
     * @param buffer PCM 16-bit audio samples
     * @param sampleRate Audio sample rate in Hz (default 44100)
     * @param threshold YIN absolute threshold — lower = more selective (default 0.15)
     * @param minFrequency Minimum detectable frequency in Hz (default 80)
     * @param maxFrequency Maximum detectable frequency in Hz (default 2000)
     * @return [PitchResult] with detected frequency and confidence, or null if no pitch found
     */
    fun detectPitch(
        buffer: ShortArray,
        sampleRate: Int = 44100,
        threshold: Float = 0.15f,
        minFrequency: Float = 80f,
        maxFrequency: Float = 2000f
    ): PitchResult? {
        val bufferSize = buffer.size
        val minTau = (sampleRate / maxFrequency).toInt().coerceAtLeast(1)
        val maxTau = (sampleRate / minFrequency).toInt().coerceAtMost(bufferSize / 2)

        if (maxTau <= minTau) return null

        // Step 1: Difference function d(τ) for τ=1..maxTau
        val diff = DoubleArray(maxTau + 1)
        for (tau in 1..maxTau) {
            var sum = 0.0
            for (j in 0 until bufferSize - tau) {
                val delta = buffer[j].toDouble() - buffer[j + tau].toDouble()
                sum += delta * delta
            }
            diff[tau] = sum
        }

        // Step 2: Cumulative mean normalized difference d'(τ)
        val cmnd = DoubleArray(maxTau + 1)
        cmnd[0] = 1.0
        var runningSum = 0.0
        for (tau in 1..maxTau) {
            runningSum += diff[tau]
            cmnd[tau] = if (runningSum > 0.0) diff[tau] * tau / runningSum else 1.0
        }

        // Step 3: Absolute threshold — find first local minimum below threshold
        var tauEstimate = -1
        for (tau in minTau until maxTau) {
            if (cmnd[tau] < threshold &&
                cmnd[tau] < cmnd[tau - 1] &&
                cmnd[tau] < cmnd[tau + 1]
            ) {
                tauEstimate = tau
                break
            }
        }

        if (tauEstimate < 0) return null

        // Step 4: Parabolic interpolation for sub-sample τ accuracy
        val refinedTau = parabolicInterpolation(cmnd, tauEstimate)

        // Step 5: Convert τ to frequency
        val frequency = if (refinedTau > 0.0) {
            (sampleRate / refinedTau).toFloat()
        } else {
            (sampleRate.toFloat() / tauEstimate)
        }

        // Confidence: deeper CMND dip = more reliable detection
        val confidence = if (threshold < 1.0f) {
            ((1.0 - cmnd[tauEstimate]) / (1.0 - threshold)).toFloat().coerceIn(0f, 1f)
        } else {
            0f
        }

        return PitchResult(
            frequency = frequency,
            cents = 0f, // caller should apply note mapping
            note = null,
            confidence = confidence
        )
    }

    /**
     * Map a detected frequency to the nearest violin string note and compute cents offset.
     *
     * @param frequency Detected frequency in Hz
     * @param referencePitchA Reference pitch for A4 in Hz (default 440)
     * @return [PitchResult] with note name, cents offset, and mapping confidence
     */
    fun frequencyToNoteAndCents(
        frequency: Float,
        referencePitchA: Int = 440
    ): PitchResult {
        val ratio = referencePitchA.toFloat() / 440.0f

        // Find closest violin string
        var bestNote: String? = null
        var bestDist = Float.MAX_VALUE
        var bestTargetFreq = 0f

        for ((note, noteRatio) in VIOLIN_NOTE_RATIOS) {
            val targetFreq = (noteRatio * referencePitchA).toFloat()
            val dist = abs(frequency - targetFreq)
            if (dist < bestDist) {
                bestDist = dist
                bestNote = note
                bestTargetFreq = targetFreq
            }
        }

        // Cents = 1200 * log2(freq / targetFreq)
        val cents = if (bestTargetFreq > 0f) {
            (1200.0 * log2(frequency.toDouble() / bestTargetFreq.toDouble())).toFloat()
        } else {
            0f
        }

        // Confidence based on distance from note (0 cents = 1.0, ±50 cents = 0.0)
        val confidence = (1.0f - (abs(cents) / 50f)).coerceIn(0f, 1f)

        return PitchResult(
            frequency = frequency,
            cents = cents,
            note = bestNote,
            confidence = confidence
        )
    }

    /**
     * Parabolic interpolation to refine the lag estimate from discrete τ.
     *
     * Fits a parabola through three points (τ-1, τ, τ+1) and finds the vertex.
     *
     * @param cmnd Cumulative mean normalized difference array
     * @param tauEstimate Integer lag estimate (index into cmnd)
     * @return Refined fractional τ (minimum of the fitted parabola)
     */
    internal fun parabolicInterpolation(cmnd: DoubleArray, tauEstimate: Int): Double {
        if (tauEstimate <= 0 || tauEstimate >= cmnd.size - 1) {
            return tauEstimate.toDouble()
        }

        val alpha = cmnd[tauEstimate - 1]
        val beta = cmnd[tauEstimate]
        val gamma = cmnd[tauEstimate + 1]

        val denominator = 2.0 * (2.0 * beta - alpha - gamma)
        if (abs(denominator) < 1e-10) {
            return tauEstimate.toDouble()
        }

        return tauEstimate + (alpha - gamma) / denominator
    }
}
