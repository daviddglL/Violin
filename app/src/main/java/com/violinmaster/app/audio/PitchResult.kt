package com.violinmaster.app.audio

/**
 * Result of pitch detection from the YIN algorithm.
 *
 * @param frequency Detected frequency in Hz (0 if no pitch detected)
 * @param cents Offset from nearest violin string note in cents.
 *              Positive = sharp, negative = flat. Not clamped; actual offset.
 * @param note Nearest violin string note: "G", "D", "A", "E", or null if no match within range
 * @param confidence Detection confidence 0.0 to 1.0 (higher = more reliable detection)
 * @param maxCents Maximum cents range used for confidence normalization (default 50)
 */

data class PitchResult(
    val frequency: Float,
    val cents: Float,
    val note: String?,
    val confidence: Float,
    val maxCents: Int = 50
)
