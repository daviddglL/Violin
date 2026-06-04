package com.violinmaster.app.audio

/**
 * Result of pitch detection from the YIN algorithm.
 *
 * @param frequency Detected frequency in Hz (0 if no pitch detected)
 * @param cents Offset from nearest instrument string note in cents (-50 to +50).
 *              Positive = sharp, negative = flat.
 * @param note Nearest instrument string note: "G", "D", "A", "E", "C", or null if no match within range
 * @param confidence Detection confidence 0.0 to 1.0 (higher = more reliable detection)
 */

data class PitchResult(
    val frequency: Float,
    val cents: Float,
    val note: String?,
    val confidence: Float
)
