package com.violinmaster.app.service

import android.graphics.Rect
import android.media.MediaMetadataRetriever
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions

/**
 * Result of face detection on video frames.
 *
 * @param faceBounds Map of frame index to list of detected face bounding boxes.
 * @param anyFaceDetected Whether at least one face was found.
 * @param shouldFallback Whether the detection window elapsed without finding faces
 *   (REQ-BLR-007: fallback to original video).
 */
data class FaceDetectionResult(
    val faceBounds: Map<Int, List<Rect>>,
    val anyFaceDetected: Boolean,
    val shouldFallback: Boolean
)

/**
 * Extracts and processes video frames for face detection using ML Kit.
 *
 * Extracted from FaceBlurProcessor.processVideo() to keep the file within
 * the SDD line limit (REQ-VM-001: ≤300 lines).
 *
 * REQ-BLR-003: Every 5th frame sampled.
 * REQ-BLR-007: Fallback if no faces detected within the detection window.
 */
class FaceDetectionProcessor {

    /**
     * Detects faces in sampled frames from the given video.
     *
     * REQ-BLR-001 through REQ-BLR-007: Uses ML Kit FaceDetection (FAST mode,
     * no contours) to detect faces on sampled frames.
     *
     * @param retriever MediaMetadataRetriever already configured with the data source.
     * @param sampledIndices Frame indices to sample (from [FaceBlurUtils.sampleFrames]).
     * @param videoWidth Width of the video in pixels.
     * @param videoHeight Height of the video in pixels.
     * @param rotation Video rotation metadata value.
     * @param targetFrameRate Assumed frame rate (used to calculate frame timestamps).
     * @param sampleRate How many frames each sample covers (used for window tracking).
     * @param detectionWindowSeconds Max seconds to wait for face detection before fallback.
     * @param onProgress Progress callback.
     * @return [FaceDetectionResult] with bounds, detection status, and fallback flag.
     */
    fun detectFaces(
        retriever: MediaMetadataRetriever,
        sampledIndices: List<Int>,
        videoWidth: Int,
        videoHeight: Int,
        rotation: Int,
        targetFrameRate: Int,
        sampleRate: Int,
        detectionWindowSeconds: Int,
        onProgress: (String) -> Unit
    ): FaceDetectionResult {
        val faceBounds = mutableMapOf<Int, List<Rect>>()
        var anyFaceDetected = false
        var detectionWindowFrames = 0

        // Set up ML Kit face detector (FAST mode, no contours)
        val faceDetectorOptions = FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .setContourMode(FaceDetectorOptions.CONTOUR_MODE_NONE)
            .build()
        val faceDetector = FaceDetection.getClient(faceDetectorOptions)

        try {
            for ((processedCount, frameIdx) in sampledIndices.withIndex()) {
                onProgress("Processing frame ${processedCount + 1}/${sampledIndices.size}")

                // Extract frame as Bitmap
                val frameTimeUs = frameIdx * (1_000_000L / targetFrameRate)
                val frameBitmap = retriever.getFrameAtTime(
                    frameTimeUs,
                    MediaMetadataRetriever.OPTION_CLOSEST_SYNC
                )

                if (frameBitmap != null) {
                    try {
                        val inputImage = InputImage.fromBitmap(frameBitmap, rotation)
                        val faces = com.google.android.gms.tasks.Tasks.await(
                            faceDetector.process(inputImage)
                        )

                        if (faces.isNotEmpty()) {
                            anyFaceDetected = true
                            val bounds = faces.map { face ->
                                val box = face.boundingBox
                                Rect(box).apply {
                                    // Expand slightly for safety margin
                                    val margin = 5
                                    left = maxOf(0, left - margin)
                                    top = maxOf(0, top - margin)
                                    right = minOf(videoWidth, right + margin)
                                    bottom = minOf(videoHeight, bottom + margin)
                                }
                            }
                            faceBounds[frameIdx] = bounds
                        }

                        detectionWindowFrames += sampleRate
                    } catch (e: Exception) {
                        // Face detection failed for this frame — continue
                    } finally {
                        frameBitmap.recycle()
                    }
                }

                // Check: if we've processed the detection window and no faces found
                val detectionTimeMs = detectionWindowFrames * (1000L / targetFrameRate)
                if (detectionTimeMs >= detectionWindowSeconds * 1000L && !anyFaceDetected) {
                    // REQ-BLR-007: Fallback — no faces detected in detection window
                    return FaceDetectionResult(
                        faceBounds = emptyMap(),
                        anyFaceDetected = false,
                        shouldFallback = true
                    )
                }
            }
        } finally {
            faceDetector.close()
        }

        return FaceDetectionResult(
            faceBounds = faceBounds,
            anyFaceDetected = anyFaceDetected,
            shouldFallback = false
        )
    }
}
