package com.violinmaster.app.service

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.media.MediaMuxer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
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
 * 2. Detect faces with [FaceDetectionProcessor]
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
open class FaceBlurProcessor @Inject constructor() {

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

            // ── 2. Extract frames + face detection ─────────────────────
            val totalFrames = estimateFrameCount(durationMs, FaceBlurUtils.TARGET_FRAME_RATE)
            val sampledIndices = FaceBlurUtils.sampleFrames(totalFrames)

            val detectionProcessor = FaceDetectionProcessor()
            val detectionResult = detectionProcessor.detectFaces(
                retriever = retriever,
                sampledIndices = sampledIndices,
                videoWidth = videoWidth,
                videoHeight = videoHeight,
                rotation = rotation,
                targetFrameRate = FaceBlurUtils.TARGET_FRAME_RATE,
                sampleRate = FaceBlurUtils.SAMPLE_RATE,
                detectionWindowSeconds = FaceBlurUtils.FACE_DETECTION_WINDOW_SECONDS,
                onProgress = onProgress
            )

            // REQ-BLR-007: Fallback — no faces detected in detection window
            if (detectionResult.shouldFallback) {
                outputFile.parentFile?.mkdirs()
                inputFile.copyTo(outputFile, overwrite = true)
                onProgress("Face blur could not be applied. Video sent without blur.")
                retriever.release()
                return@withContext outputFile
            }

            val faceBounds = detectionResult.faceBounds
            val anyFaceDetected = detectionResult.anyFaceDetected

            // ── 3. Transcode video with blurred face regions ────────────
            //
            // REQ-BLR-001 through REQ-BLR-005: Full decode→modify→encode
            // pipeline. Compressed frames from MediaExtractor are decoded
            // to raw NV12 pixel data via MediaCodec decoder, face regions
            // are pixelated in-place on the NV12 buffer (no full-frame
            // per-pixel conversion), then modified frames are re-encoded.
            //
            // Architecture:
            //   extractor → decoder → (pixelate face in NV12) → encoder → muxer
            //
            // Blur approach: box pixelation directly on NV12 Y+UV planes.
            // Each face region is divided into kernelSize×kernelSize blocks;
            // block pixels are replaced with the block average. This is
            // O(face-area) not O(frame-area).
            //
            // Edge cases handled:
            //   - No face bounds for a frame → pass through unmodified
            //   - Multiple faces → each face region pixelated independently
            //   - Face partially out of frame → bounds clamped to frame dims

            // Detach the retriever's file handle before extractor opens it
            retriever.release()

            extractor = MediaExtractor()
            extractor.setDataSource(inputFile.absolutePath)

            val videoTrackIndex = findVideoTrack(extractor)
            if (videoTrackIndex < 0) {
                throw IllegalStateException("No video track found in input file")
            }

            extractor.selectTrack(videoTrackIndex)
            val inputFormat = extractor.getTrackFormat(videoTrackIndex)
            val mime = inputFormat.getString(MediaFormat.KEY_MIME)
                ?: throw IllegalStateException("Unknown video MIME type")

            // ── 3a. Set up decoder ─────────────────────────────────────
            var decoder: MediaCodec? = null
            try {
                decoder = MediaCodec.createDecoderByType(mime)
                decoder.configure(inputFormat, null, null, 0)
                decoder.start()

                // ── 3b. Set up encoder ─────────────────────────────────
                // Preserve original bitrate; allow encoder to request any
                // COLOR_FormatYUV420Flexible variant for broad compatibility.
                codec = MediaCodec.createEncoderByType(FaceBlurUtils.MIME_TYPE)
                val encoderFormat = MediaFormat.createVideoFormat(
                    FaceBlurUtils.MIME_TYPE,
                    videoWidth,
                    videoHeight
                ).apply {
                    setInteger(
                        MediaFormat.KEY_BIT_RATE,
                        inputFormat.getInteger(MediaFormat.KEY_BIT_RATE)
                    )
                    setInteger(MediaFormat.KEY_FRAME_RATE, FaceBlurUtils.TARGET_FRAME_RATE)
                    setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, FaceBlurUtils.I_FRAME_INTERVAL)
                    setInteger(
                        MediaFormat.KEY_COLOR_FORMAT,
                        MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible
                    )
                }
                codec.configure(encoderFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
                codec.start()

                // ── 3c. Set up muxer ───────────────────────────────────
                outputFile.parentFile?.mkdirs()
                muxer = MediaMuxer(
                    outputFile.absolutePath,
                    MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4
                )

                // ── 3d. Transcoding loop ───────────────────────────────
                var muxerStarted = false
                var muxerTrackIndex = -1
                var frameIndex = 0
                var extractorDone = false
                var decoderEosReceived = false
                var encoderEosReceived = false
                val decInfo = MediaCodec.BufferInfo()
                val encInfo = MediaCodec.BufferInfo()

                while (!encoderEosReceived) {
                    // Feed extractor → decoder
                    if (!extractorDone) {
                        val decInIdx = decoder.dequeueInputBuffer(FaceBlurUtils.TIMEOUT_US)
                        if (decInIdx >= 0) {
                            val decInBuf = decoder.getInputBuffer(decInIdx)
                            if (decInBuf != null) {
                                val sampleSize = extractor.readSampleData(decInBuf, 0)
                                if (sampleSize < 0) {
                                    decoder.queueInputBuffer(
                                        decInIdx, 0, 0, 0,
                                        MediaCodec.BUFFER_FLAG_END_OF_STREAM
                                    )
                                    extractorDone = true
                                } else {
                                    decoder.queueInputBuffer(
                                        decInIdx, 0, sampleSize,
                                        extractor.sampleTime, 0
                                    )
                                    extractor.advance()
                                }
                            }
                        }
                    }

                    // Drain decoder → pixelate faces → feed encoder
                    var decOutIdx = decoder.dequeueOutputBuffer(decInfo, FaceBlurUtils.TIMEOUT_US)
                    while (decOutIdx >= 0) {
                        if ((decInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                            decoder.releaseOutputBuffer(decOutIdx, false)
                            decOutIdx = decoder.dequeueOutputBuffer(decInfo, FaceBlurUtils.TIMEOUT_US)
                            continue
                        }

                        // Propagate EOS from decoder to encoder
                        if ((decInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                            decoderEosReceived = true
                            val encInIdx = codec.dequeueInputBuffer(FaceBlurUtils.TIMEOUT_US)
                            if (encInIdx >= 0) {
                                codec.queueInputBuffer(
                                    encInIdx, 0, 0, 0,
                                    MediaCodec.BUFFER_FLAG_END_OF_STREAM
                                )
                            }
                            decoder.releaseOutputBuffer(decOutIdx, false)
                            break
                        }

                        val decOutBuf = decoder.getOutputBuffer(decOutIdx)
                        if (decOutBuf != null && decInfo.size > 0) {
                            // Apply pixelation blur to face regions in-place on NV12 buffer
                            val bounds = findFaceBoundsForFrame(
                                faceBounds, frameIndex, FaceBlurUtils.SAMPLE_RATE
                            )
                            if (bounds.isNotEmpty()) {
                                val kernel = calculateBlurKernelForFrame(bounds)
                                pixelateFaceRegionsNv12(
                                    buffer = decOutBuf,
                                    bufferOffset = decInfo.offset,
                                    frameWidth = videoWidth,
                                    frameHeight = videoHeight,
                                    faceRegions = bounds,
                                    kernelSize = kernel
                                )
                            }

                            // Feed modified buffer to encoder
                            var encInIdx = codec.dequeueInputBuffer(FaceBlurUtils.TIMEOUT_US)
                            // Drain encoder outputs if encoder input isn't ready (avoids deadlock)
                            while (encInIdx < 0) {
                                drainEncoderOutput(codec, muxer, encInfo, muxerStarted, muxerTrackIndex)?.let {
                                    if (it.first) muxerStarted = true
                                    if (it.second >= 0) muxerTrackIndex = it.second
                                    if (it.third) encoderEosReceived = true
                                }
                                if (encoderEosReceived) break
                                encInIdx = codec.dequeueInputBuffer(FaceBlurUtils.TIMEOUT_US)
                            }
                            if (encoderEosReceived) {
                                decoder.releaseOutputBuffer(decOutIdx, false)
                                break
                            }

                            val encInBuf = codec.getInputBuffer(encInIdx)
                            if (encInBuf != null) {
                                // Copy modified decoder output to encoder input
                                decOutBuf.position(decInfo.offset)
                                decOutBuf.limit(decInfo.offset + decInfo.size)
                                encInBuf.clear()
                                encInBuf.put(decOutBuf)
                                codec.queueInputBuffer(
                                    encInIdx, 0, decInfo.size,
                                    decInfo.presentationTimeUs, 0
                                )
                            }

                            frameIndex++
                            if (frameIndex % 30 == 0) {
                                onProgress("Processing frame $frameIndex/${totalFrames}")
                            }
                        }

                        decoder.releaseOutputBuffer(decOutIdx, false)
                        decOutIdx = decoder.dequeueOutputBuffer(decInfo, FaceBlurUtils.TIMEOUT_US)
                    }

                    // Drain encoder → muxer
                    drainEncoderOutput(codec, muxer, encInfo, muxerStarted, muxerTrackIndex)?.let {
                        if (it.first) muxerStarted = true
                        if (it.second >= 0) muxerTrackIndex = it.second
                        if (it.third) encoderEosReceived = true
                    }
                }

                // ── 3e. Cleanup decoder ─────────────────────────────────
                try { decoder.stop() } catch (_: Exception) {}
                try { decoder.release() } catch (_: Exception) {}
                decoder = null

                // ── 4. Finalize ─────────────────────────────────────────
                muxer.stop()
                muxer.release()
                muxer = null
                codec.stop()
                codec.release()
                codec = null
                extractor.release()
                extractor = null

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
                // Release decoder on any failure within the transcode block
                try { decoder?.stop() } catch (_: Exception) {}
                try { decoder?.release() } catch (_: Exception) {}
                throw e
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
    // Frame-level helpers
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Maps a frame index to the nearest sampled frame's face bounds.
     *
     * Face detection runs every [sampleRate]-th frame, but blur must be
     * applied to ALL frames. This function finds the nearest sampled
     * frame that has detected faces, so non-sampled frames reuse bounds
     * from the closest detection sample. Face movement between 2-3 frames
     * at 30fps is negligible for privacy blur purposes.
     *
     * Search order: current block's sampled frame → previous block → next block.
     *
     * @param faceBounds Map from sampled frame index to detected face rectangles.
     * @param frameIndex The current frame being processed.
     * @param sampleRate Frame sampling interval (e.g., 5 = every 5th frame).
     * @return List of face rectangles for this frame, or empty if none nearby.
     */
    private fun findFaceBoundsForFrame(
        faceBounds: Map<Int, List<android.graphics.Rect>>,
        frameIndex: Int,
        sampleRate: Int
    ): List<android.graphics.Rect> {
        if (faceBounds.isEmpty()) return emptyList()

        // Map current frame to its sample block (e.g., frames 0-4 → sample 0)
        val sampledFrame = (frameIndex / sampleRate) * sampleRate

        // Try exact match first, then adjacent sampled frames (within ±1 block)
        faceBounds[sampledFrame]?.let { return it }
        faceBounds[sampledFrame - sampleRate]?.let { return it }
        faceBounds[sampledFrame + sampleRate]?.let { return it }

        return emptyList()
    }

    /**
     * Calculates a pixelation kernel size from detected face bounds.
     *
     * Uses the widest face box to determine kernel size per
     * [FaceBlurUtils.calculateBlurKernel]. Falls back to
     * [FaceBlurUtils.MIN_KERNEL] if no bounds exist.
     */
    private fun calculateBlurKernelForFrame(bounds: List<android.graphics.Rect>): Int {
        val maxWidth = bounds.maxOfOrNull { it.width() } ?: FaceBlurUtils.MIN_KERNEL
        return FaceBlurUtils.calculateBlurKernel(maxWidth)
    }

    /**
     * Applies box pixelation to face regions directly in an NV12 (YUV420 Semi-Planar)
     * buffer — no Bitmap allocation, no full-frame per-pixel conversion.
     *
     * **NV12 layout** (COLOR_FormatYUV420Flexible / COLOR_FormatYUV420SemiPlanar):
     * ```
     * Y plane:  [Y Y Y ...]                      ← frameWidth × frameHeight bytes
     * UV plane: [U V U V U V ...]                ← frameWidth × frameHeight/2 bytes (interleaved)
     * ```
     * Each UV pair covers a 2×2 block of Y pixels (4:2:0 chroma subsampling).
     *
     * **Pixelation algorithm**:
     * For each face region, partition it into [kernelSize]×[kernelSize] blocks.
     * Replace every pixel in a block with the block's average value.
     * Both Y (luminance) and UV (chrominance) planes are pixelated independently.
     *
     * This is O(face-area) — the rest of the frame passes through untouched.
     *
     * @param buffer The NV12 ByteBuffer containing decoded frame data.
     * @param bufferOffset Starting offset of frame data within the buffer (from BufferInfo.offset).
     * @param frameWidth Width of the video frame in pixels.
     * @param frameHeight Height of the video frame in pixels.
     * @param faceRegions List of face bounding rectangles in pixel coordinates.
     * @param kernelSize Block size for pixelation (from [FaceBlurUtils.calculateBlurKernel]).
     */
    private fun pixelateFaceRegionsNv12(
        buffer: java.nio.ByteBuffer,
        bufferOffset: Int,
        frameWidth: Int,
        frameHeight: Int,
        faceRegions: List<android.graphics.Rect>,
        kernelSize: Int
    ) {
        if (faceRegions.isEmpty() || kernelSize <= 0) return

        val yOffset = bufferOffset
        val uvOffset = bufferOffset + frameWidth * frameHeight

        for (region in faceRegions) {
            // Clamp region to frame boundaries (handles partially out-of-frame faces)
            val left = region.left.coerceIn(0, frameWidth)
            val top = region.top.coerceIn(0, frameHeight)
            val right = region.right.coerceIn(0, frameWidth)
            val bottom = region.bottom.coerceIn(0, frameHeight)

            if (right <= left || bottom <= top) continue

            // ── Pixelate Y (luminance) plane ──────────────────────────
            var blockY = top
            while (blockY < bottom) {
                val blockH = minOf(kernelSize, bottom - blockY)
                var blockX = left
                while (blockX < right) {
                    val blockW = minOf(kernelSize, right - blockX)

                    // Average Y values in this block
                    var sumY = 0
                    var count = 0
                    for (dy in 0 until blockH) {
                        for (dx in 0 until blockW) {
                            val idx = yOffset + (blockY + dy) * frameWidth + (blockX + dx)
                            sumY += buffer[idx].toInt() and 0xFF
                            count++
                        }
                    }
                    val avgY = (sumY / count).toByte()

                    // Write averaged Y back to block
                    for (dy in 0 until blockH) {
                        for (dx in 0 until blockW) {
                            val idx = yOffset + (blockY + dy) * frameWidth + (blockX + dx)
                            buffer.put(idx, avgY)
                        }
                    }

                    blockX += blockW
                }
                blockY += blockH
            }

            // ── Pixelate UV (chrominance) plane ───────────────────────
            // UV is at half resolution: each UV pair covers 2×2 Y pixels.
            // UV plane row stride = frameWidth bytes (same as Y plane stride).
            val uvLeft = left / 2
            val uvTop = top / 2
            val uvRight = (right + 1) / 2  // ceiling division for safety
            val uvBottom = (bottom + 1) / 2
            val uvKernel = maxOf(kernelSize / 2, 1)  // half-res kernel

            var uvBlockY = uvTop
            while (uvBlockY < uvBottom) {
                val uvBlockH = minOf(uvKernel, uvBottom - uvBlockY)
                var uvBlockX = uvLeft
                while (uvBlockX < uvRight) {
                    val uvBlockW = minOf(uvKernel, uvRight - uvBlockX)

                    // Average U and V values in this UV block
                    var sumU = 0
                    var sumV = 0
                    var uvCount = 0
                    for (dy in 0 until uvBlockH) {
                        for (dx in 0 until uvBlockW) {
                            // Each UV pair is 2 bytes: U at even offset, V at odd offset
                            val uIdx = uvOffset + (uvBlockY + dy) * frameWidth + (uvBlockX + dx) * 2
                            sumU += buffer[uIdx].toInt() and 0xFF
                            sumV += buffer[uIdx + 1].toInt() and 0xFF
                            uvCount++
                        }
                    }
                    val avgU = (sumU / uvCount).toByte()
                    val avgV = (sumV / uvCount).toByte()

                    // Write averaged UV back to block
                    for (dy in 0 until uvBlockH) {
                        for (dx in 0 until uvBlockW) {
                            val uIdx = uvOffset + (uvBlockY + dy) * frameWidth + (uvBlockX + dx) * 2
                            buffer.put(uIdx, avgU)
                            buffer.put(uIdx + 1, avgV)
                        }
                    }

                    uvBlockX += uvBlockW
                }
                uvBlockY += uvBlockH
            }
        }
    }

    /**
     * Drains output buffers from the encoder to the muxer.
     *
     * Non-blocking: returns null if no output is available.
     * Handles CODEC_CONFIG buffers (skip), starts the muxer on first valid
     * output format, and detects end-of-stream.
     *
     * @return Triple(muxerStarted, muxerTrackIndex, encoderEosReceived) or null if no output.
     */
    private fun drainEncoderOutput(
        encoder: MediaCodec,
        muxer: MediaMuxer,
        encInfo: MediaCodec.BufferInfo,
        muxerStarted: Boolean,
        muxerTrackIndex: Int
    ): Triple<Boolean, Int, Boolean>? {
        val outIdx = encoder.dequeueOutputBuffer(encInfo, FaceBlurUtils.TIMEOUT_US)
        if (outIdx < 0) return null

        var started = muxerStarted
        var trackIdx = muxerTrackIndex
        var eos = false

        try {
            if ((encInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                encoder.releaseOutputBuffer(outIdx, false)
                return Triple(started, trackIdx, eos)
            }

            if ((encInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                eos = true
                encoder.releaseOutputBuffer(outIdx, false)
                return Triple(started, trackIdx, eos)
            }

            if (!started && encInfo.size > 0) {
                trackIdx = muxer.addTrack(encoder.outputFormat)
                muxer.start()
                started = true
            }

            if (started && encInfo.size > 0) {
                val outBuf = encoder.getOutputBuffer(outIdx)
                if (outBuf != null) {
                    outBuf.position(encInfo.offset)
                    outBuf.limit(encInfo.offset + encInfo.size)
                    muxer.writeSampleData(trackIdx, outBuf, encInfo)
                }
            }

            encoder.releaseOutputBuffer(outIdx, false)
        } catch (_: Exception) {
            try { encoder.releaseOutputBuffer(outIdx, false) } catch (_: Exception) {}
        }

        return Triple(started, trackIdx, eos)
    }

    // ═══════════════════════════════════════════════════════════════════
    // Utility helpers
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
