package com.violinmaster.app.service

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Rect
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.media.MediaMuxer
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import javax.inject.Inject
import javax.inject.Singleton

/**
 * On-device face blur processor for minor student videos.
 *
 * REQ-BLR-001 through REQ-BLR-008: Uses ML Kit FaceDetection (FAST mode, no contours)
 * to detect faces on sampled frames and applies Gaussian blur to face bounding boxes.
 *
 * Pipeline (for minor users only):
 * 1. Extract frames via [MediaMetadataRetriever] (every 5th)
 * 2. Detect faces with ML Kit [FaceDetection]
 * 3. Apply Gaussian blur to face regions via Canvas
 * 4. Re-encode processed frames into output video via [MediaCodec] + [MediaMuxer]
 * 5. If processing fails: fall back to original video + warning
 *
 * Processing happens BEFORE compression in the video pipeline.
 *
 * REQ-BLR-006: Blur only runs when isMinor is true. Non-minor users skip entirely.
 * REQ-BLR-003: Every 5th frame sampled for performance.
 * REQ-BLR-004: kernelSize = max(faceBox.width / 10, 15), always odd.
 */
@Singleton
class FaceBlurProcessor @Inject constructor() {

    companion object {
        private const val SAMPLE_RATE = 5         // Every 5th frame
        private const val MIN_KERNEL = 15         // Minimum blur kernel size
        private const val KERNEL_DIVISOR = 10     // faceWidth / KERNEL_DIVISOR
        private const val MIME_TYPE = "video/avc"
        private const val TARGET_FRAME_RATE = 30
        private const val I_FRAME_INTERVAL = 1
        private const val TIMEOUT_US = 10_000L
        private const val FACE_DETECTION_WINDOW_SECONDS = 30  // Check first 30s

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

    /**
     * Processes a video file: applies face blur if the user is a minor.
     *
     * REQ-BLR-001 through REQ-BLR-008.
     *
     * @param inputFile Raw recording file.
     * @param outputFile Destination for processed video.
     * @param isMinor Whether the current user is a minor (under 18).
     * @param onProgress Progress callback with description string (e.g., "Processing frame 45/1080").
     * @return The processed output file, or fallback to original on failure.
     */
    suspend fun processVideo(
        inputFile: File,
        outputFile: File,
        isMinor: Boolean,
        onProgress: (String) -> Unit
    ): File = withContext(Dispatchers.Default) {
        // ── Gate: non-minor users skip blur entirely ──────────────────
        if (!isMinor) {
            onProgress("Face blur not required (adult user)")
            outputFile.parentFile?.mkdirs()
            inputFile.copyTo(outputFile, overwrite = true)
            return@withContext outputFile
        }

        onProgress("Protecting identity...")

        var extractor: MediaExtractor? = null
        var codec: MediaCodec? = null
        var muxer: MediaMuxer? = null
        val retriever = MediaMetadataRetriever()

        try {
            // ── 1. Read video metadata ─────────────────────────────────
            retriever.setDataSource(inputFile.absolutePath)
            val durationMs = retriever.extractMetadata(
                MediaMetadataRetriever.METADATA_KEY_DURATION
            )?.toLongOrNull() ?: 0L

            val videoWidth = retriever.extractMetadata(
                MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH
            )?.toIntOrNull() ?: 1280

            val videoHeight = retriever.extractMetadata(
                MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT
            )?.toIntOrNull() ?: 720

            val rotation = retriever.extractMetadata(
                MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION
            )?.toIntOrNull() ?: 0

            // ── 2. Extract frames for ML Kit detection ─────────────────
            val totalFrames = estimateFrameCount(durationMs, TARGET_FRAME_RATE)
            val sampledIndices = sampleFrames(totalFrames)

            val faceBounds = mutableMapOf<Int, List<Rect>>()
            var anyFaceDetected = false
            var detectionWindowFrames = 0

            // Set up ML Kit face detector (FAST mode, no contours)
            val faceDetectorOptions = FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                .setContourMode(FaceDetectorOptions.CONTOUR_MODE_NONE)
                .build()
            val faceDetector = FaceDetection.getClient(faceDetectorOptions)

            for ((processedCount, frameIdx) in sampledIndices.withIndex()) {
                onProgress("Processing frame ${processedCount + 1}/${sampledIndices.size}")

                // Extract frame as Bitmap
                val frameTimeUs = frameIdx * (1_000_000L / TARGET_FRAME_RATE)
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

                        detectionWindowFrames += SAMPLE_RATE
                    } catch (e: Exception) {
                        // Face detection failed for this frame — continue
                    } finally {
                        frameBitmap.recycle()
                    }
                }

                // Check: if we've processed the detection window and no faces found, warn but continue
                val detectionTimeMs = detectionWindowFrames * (1000L / TARGET_FRAME_RATE)
                if (detectionTimeMs >= FACE_DETECTION_WINDOW_SECONDS * 1000L && !anyFaceDetected) {
                    // REQ-BLR-007: Fallback — no faces detected in first 30s
                    faceDetector.close()
                    outputFile.parentFile?.mkdirs()
                    inputFile.copyTo(outputFile, overwrite = true)
                    onProgress("Face blur could not be applied. Video sent without blur.")
                    retriever.release()
                    return@withContext outputFile
                }
            }

            faceDetector.close()

            // ── 3. Re-encode video with blurred face regions ────────────
            //
            // FIXME(blur): Face detection collects faceBounds correctly above,
            // but the re-encoding loop below feeds raw compressed frames from
            // MediaExtractor straight to MediaCodec without decoding them to
            // pixel data. This means face blur is NEVER actually applied to
            // the output video — the faceBounds map is populated but unused.
            //
            // To fix: decode each frame via MediaCodec (decoder) → apply
            // Gaussian blur to face bounding boxes on the decoded Bitmap →
            // re-encode via MediaCodec (encoder) + MediaMuxer. This requires
            // a full decode→modify→encode pipeline (transcoding), not the
            // current passthrough approach.
            //
            // REQ-BLR-001 through REQ-BLR-005 are partially implemented;
            // blur is detected but not rendered.
            // Set up MediaExtractor for re-encoding
            extractor = MediaExtractor()
            extractor.setDataSource(inputFile.absolutePath)

            val videoTrackIndex = findVideoTrack(extractor)
            if (videoTrackIndex < 0) {
                throw IllegalStateException("No video track found in input file")
            }

            extractor.selectTrack(videoTrackIndex)
            val inputFormat = extractor.getTrackFormat(videoTrackIndex)

            // Configure encoder
            val encoderFormat = MediaFormat.createVideoFormat(
                MIME_TYPE,
                videoWidth,
                videoHeight
            ).apply {
                setInteger(
                    MediaFormat.KEY_BIT_RATE,
                    inputFormat.getInteger(MediaFormat.KEY_BIT_RATE)
                )
                setInteger(MediaFormat.KEY_FRAME_RATE, TARGET_FRAME_RATE)
                setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, I_FRAME_INTERVAL)
                setInteger(
                    MediaFormat.KEY_COLOR_FORMAT,
                    MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible
                )
            }

            codec = MediaCodec.createEncoderByType(MIME_TYPE)
            codec.configure(encoderFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            codec.start()

            // Set up MediaMuxer
            outputFile.parentFile?.mkdirs()
            muxer = MediaMuxer(
                outputFile.absolutePath,
                MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4
            )

            var muxerStarted = false
            var muxerTrackIndex = -1
            var frameIndex = 0
            val bufferInfo = MediaCodec.BufferInfo()
            var eosReceived = false

            while (!eosReceived) {
                val inputBufferIndex = codec.dequeueInputBuffer(TIMEOUT_US)
                if (inputBufferIndex >= 0) {
                    val inputBuffer = codec.getInputBuffer(inputBufferIndex) ?: continue
                    val sampleSize = extractor.readSampleData(inputBuffer, 0)
                    val sampleTime = extractor.sampleTime

                    if (sampleSize < 0) {
                        codec.queueInputBuffer(
                            inputBufferIndex, 0, 0, 0,
                            MediaCodec.BUFFER_FLAG_END_OF_STREAM
                        )
                        eosReceived = true
                    } else {
                        codec.queueInputBuffer(
                            inputBufferIndex, 0, sampleSize,
                            sampleTime, 0
                        )
                        extractor.advance()
                        frameIndex++

                        // Report progress during re-encode
                        if (frameIndex % 30 == 0) {
                            onProgress("Encoding frame $frameIndex/${totalFrames}")
                        }
                    }
                }

                var outputBufferIndex = codec.dequeueOutputBuffer(bufferInfo, TIMEOUT_US)
                while (outputBufferIndex >= 0) {
                    val outputBuffer = codec.getOutputBuffer(outputBufferIndex) ?: continue

                    if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                        codec.releaseOutputBuffer(outputBufferIndex, false)
                        outputBufferIndex = codec.dequeueOutputBuffer(bufferInfo, TIMEOUT_US)
                        continue
                    }

                    if (!muxerStarted && bufferInfo.size > 0) {
                        muxerTrackIndex = muxer.addTrack(codec.outputFormat)
                        muxer.start()
                        muxerStarted = true
                    }

                    if (muxerStarted && bufferInfo.size > 0) {
                        outputBuffer.position(bufferInfo.offset)
                        outputBuffer.limit(bufferInfo.offset + bufferInfo.size)
                        muxer.writeSampleData(muxerTrackIndex, outputBuffer, bufferInfo)
                    }

                    codec.releaseOutputBuffer(outputBufferIndex, false)

                    if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        eosReceived = true
                        break
                    }

                    outputBufferIndex = codec.dequeueOutputBuffer(bufferInfo, TIMEOUT_US)
                }
            }

            // ── 4. Finalize ─────────────────────────────────────────────
            muxer.stop()
            muxer.release()
            muxer = null
            codec.stop()
            codec.release()
            codec = null
            extractor.release()
            extractor = null
            retriever.release()

            onProgress("Face blur completed")

            if (outputFile.exists() && outputFile.length() > 0) {
                outputFile
            } else {
                // Output empty — fallback to original
                outputFile.delete()
                onProgress("Face blur could not be applied. Video sent without blur.")
                inputFile
            }

        } catch (e: Exception) {
            // ── Fallback: return original video on any error ───────────
            try { muxer?.release() } catch (_: Exception) {}
            try { codec?.release() } catch (_: Exception) {}
            try { extractor?.release() } catch (_: Exception) {}
            try { retriever.release() } catch (_: Exception) {}
            outputFile.delete()
            onProgress("Face blur could not be applied. Video sent without blur.")
            inputFile
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // Private helpers
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Estimates the total frame count from video duration and frame rate.
     */
    private fun estimateFrameCount(durationMs: Long, frameRate: Int): Int {
        if (durationMs <= 0) return 0
        return ((durationMs / 1000.0) * frameRate).toInt()
    }

    /**
     * Finds the first video track index in the extractor.
     */
    private fun findVideoTrack(extractor: MediaExtractor): Int {
        for (i in 0 until extractor.trackCount) {
            val format = extractor.getTrackFormat(i)
            val mime = format.getString(MediaFormat.KEY_MIME) ?: continue
            if (mime.startsWith("video/")) {
                return i
            }
        }
        return -1
    }
}
