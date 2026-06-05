package com.violinmaster.app.service

import java.io.File

/**
 * Utility functions and constants for face blur processing.
 *
 * Extracted from FaceBlurProcessor companion object to keep files under the
 * SDD line limit (REQ-VM-001: ≤300 lines).
 */
object FaceBlurUtils {
    const val SAMPLE_RATE = 5
    const val MIN_KERNEL = 15
    const val KERNEL_DIVISOR = 10
    const val MIME_TYPE = "video/avc"
    const val TARGET_FRAME_RATE = 30
    const val I_FRAME_INTERVAL = 1
    const val TIMEOUT_US = 10_000L
    const val FACE_DETECTION_WINDOW_SECONDS = 30

    /**
     * Determines whether a user is a minor (under 18) based on birth year.
     *
     * REQ-BLR-006: Blur pipeline gate.
     *
     * @param birthYear User's birth year from registration.
     * @param currentYear Current calendar year.
     * @return true if user is under 18, false otherwise (including legacy users with birthYear ≤ 1900).
     */
    fun isMinor(birthYear: Int, currentYear: Int): Boolean {
        if (birthYear <= 1900) return false  // legacy/edge guard
        return (currentYear - birthYear) < 18
    }

    /**
     * Calculates the Gaussian blur kernel size based on face box width.
     *
     * REQ-BLR-004: kernelSize = max(faceBox.width / 10, 15).
     * Always returns an odd number for proper Gaussian matrix symmetry.
     *
     * @param faceBoxWidth Width of the detected face bounding box in pixels.
     * @return Odd kernel size, minimum 15.
     */
    fun calculateBlurKernel(faceBoxWidth: Int): Int {
        val kernel = maxOf(faceBoxWidth / KERNEL_DIVISOR, MIN_KERNEL)
        // Ensure odd (Gaussian blur requires odd kernel)
        return if (kernel % 2 == 0) kernel + 1 else kernel
    }

    /**
     * Generates the list of frame indices to sample for face detection.
     *
     * REQ-BLR-003: Every 5th frame (0, 5, 10, 15, ...).
     *
     * @param totalFrames Total number of frames in the video.
     * @param sampleRate How often to sample (default 5 = every 5th frame).
     * @return List of frame indices to process.
     */
    fun sampleFrames(totalFrames: Int, sampleRate: Int = SAMPLE_RATE): List<Int> {
        if (totalFrames <= 0) return emptyList()
        return (0 until totalFrames step sampleRate).toList()
    }

    /**
     * Simulates the copy path for non-minor users: copies inputFile to outputFile.
     *
     * Used in tests to verify the non-minor gate without needing MediaCodec.
     *
     * @param inputFile Source file to copy.
     * @param outputFile Destination file.
     * @return The output file (copy of input).
     */
    fun processFileCopyWhenNotMinor(inputFile: File, outputFile: File): File {
        outputFile.parentFile?.mkdirs()
        inputFile.copyTo(outputFile, overwrite = true)
        return outputFile
    }
}
