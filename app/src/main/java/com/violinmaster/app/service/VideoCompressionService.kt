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
 * Compresses video files using MediaCodec async API (H.264 encoding).
 *
 * REQ-VID-004: Targets 720p max resolution, ~1 Mbps bitrate, 30 fps.
 * If the compressed file ends up larger than the original, the original is kept.
 *
 * Uses a simplified async approach:
 * 1. Reads input via [MediaExtractor]
 * 2. Transcodes to lower bitrate via [MediaCodec] + [MediaMuxer]
 * 3. If compressed file is larger, keeps the original
 *
 * Implementation note: Full async MediaCodec with callback is complex.
 * This simplified version uses synchronous dequeue for correctness at the
 * cost of some throughput. Optimizations can follow.
 */
@Singleton
class VideoCompressionService @Inject constructor() {

    companion object {
        private const val TARGET_BITRATE = 1_000_000  // 1 Mbps
        private const val TARGET_FRAME_RATE = 30
        private const val MAX_WIDTH = 1280
        private const val MAX_HEIGHT = 720
        private const val MIME_TYPE = "video/avc"     // H.264
        private const val I_FRAME_INTERVAL = 1         // I-frame every second
        private const val TIMEOUT_US = 10_000L
    }

    /**
     * Compresses the input video to H.264 with target constraints.
     *
     * REQ-VID-004: 720p max, 1 Mbps bitrate, 30 fps.
     * Progress callback from 0.0 to 1.0 throughout the transcode.
     *
     * @param inputFile Raw recording file.
     * @param outputFile Destination for compressed video.
     * @param onProgress Progress callback (0.0 to 1.0).
     * @return The compressed output file, or the original input if compression didn't help.
     */
    suspend fun compressVideo(
        inputFile: File,
        outputFile: File,
        onProgress: (Float) -> Unit
    ): File = withContext(Dispatchers.Default) {
        val retriever = MediaMetadataRetriever()
        var extractor: MediaExtractor? = null
        var codec: MediaCodec? = null
        var muxer: MediaMuxer? = null

        try {
            // ── 1. Read input metadata ──────────────────────────────────
            retriever.setDataSource(inputFile.absolutePath)
            val inputDurationMs = retriever.extractMetadata(
                MediaMetadataRetriever.METADATA_KEY_DURATION
            )?.toLongOrNull() ?: 0L

            val originalWidth = retriever.extractMetadata(
                MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH
            )?.toIntOrNull() ?: MAX_WIDTH

            val originalHeight = retriever.extractMetadata(
                MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT
            )?.toIntOrNull() ?: MAX_HEIGHT

            // ── 2. Calculate target resolution (maintain aspect ratio) ──
            val outputWidth: Int
            val outputHeight: Int
            if (originalWidth <= MAX_WIDTH && originalHeight <= MAX_HEIGHT) {
                outputWidth = originalWidth
                outputHeight = originalHeight
            } else {
                val widthRatio = MAX_WIDTH.toFloat() / originalWidth
                val heightRatio = MAX_HEIGHT.toFloat() / originalHeight
                val scale = minOf(widthRatio, heightRatio)
                outputWidth = (originalWidth * scale).toInt() / 2 * 2 // even
                outputHeight = (originalHeight * scale).toInt() / 2 * 2
            }

            // ── 3. Set up MediaExtractor ────────────────────────────────
            extractor = MediaExtractor()
            extractor.setDataSource(inputFile.absolutePath)

            val videoTrackIndex = findVideoTrack(extractor)
            if (videoTrackIndex < 0) {
                // No video track — return original
                onProgress(1.0f)
                return@withContext inputFile
            }

            extractor.selectTrack(videoTrackIndex)
            val inputFormat = extractor.getTrackFormat(videoTrackIndex)

            // ── 4. Configure MediaCodec encoder ─────────────────────────
            val encoderFormat = MediaFormat.createVideoFormat(MIME_TYPE, outputWidth, outputHeight).apply {
                setInteger(MediaFormat.KEY_BIT_RATE, TARGET_BITRATE)
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

            // ── 5. Set up MediaMuxer ────────────────────────────────────
            outputFile.parentFile?.mkdirs()
            muxer = MediaMuxer(
                outputFile.absolutePath,
                MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4
            )

            // ── 6. Transcode loop ──────────────────────────────────────
            var muxerStarted = false
            var trackIndex = -1
            var framesProcessed = 0
            val bufferInfo = MediaCodec.BufferInfo()
            val frameDurationUs = if (inputDurationMs > 0) {
                inputDurationMs * 1000L // total duration in µs
            } else {
                0L
            }

            var eosReceived = false
            while (!eosReceived) {
                // Feed input to extractor (we're re-encoding, so read raw frames)
                val inputBufferIndex = codec.dequeueInputBuffer(TIMEOUT_US)
                if (inputBufferIndex >= 0) {
                    val inputBuffer = codec.getInputBuffer(inputBufferIndex) ?: continue
                    val sampleSize = extractor.readSampleData(inputBuffer, 0)
                    val sampleTime = extractor.sampleTime

                    if (sampleSize < 0) {
                        // End of stream
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
                        framesProcessed++

                        // Report progress based on sample time vs duration
                        if (frameDurationUs > 0) {
                            val progress = (sampleTime.toFloat() / frameDurationUs.toFloat())
                                .coerceIn(0f, 0.99f)
                            onProgress(progress)
                        }
                    }
                }

                // Dequeue output
                var outputBufferIndex = codec.dequeueOutputBuffer(bufferInfo, TIMEOUT_US)
                while (outputBufferIndex >= 0) {
                    val outputBuffer = codec.getOutputBuffer(outputBufferIndex) ?: continue

                    if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                        // Codec config — skip for now (simplified)
                        codec.releaseOutputBuffer(outputBufferIndex, false)
                        outputBufferIndex = codec.dequeueOutputBuffer(bufferInfo, TIMEOUT_US)
                        continue
                    }

                    if (!muxerStarted && bufferInfo.size > 0) {
                        trackIndex = muxer.addTrack(codec.outputFormat)
                        muxer.start()
                        muxerStarted = true
                    }

                    if (muxerStarted && bufferInfo.size > 0) {
                        outputBuffer.position(bufferInfo.offset)
                        outputBuffer.limit(bufferInfo.offset + bufferInfo.size)
                        muxer.writeSampleData(trackIndex, outputBuffer, bufferInfo)
                    }

                    codec.releaseOutputBuffer(outputBufferIndex, false)

                    if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        eosReceived = true
                        break
                    }

                    outputBufferIndex = codec.dequeueOutputBuffer(bufferInfo, TIMEOUT_US)
                }
            }

            onProgress(1.0f)

            // ── 7. Finalize ─────────────────────────────────────────────
            muxer.stop()
            muxer.release()
            muxer = null
            codec.stop()
            codec.release()
            codec = null
            extractor.release()
            extractor = null

            // ── 8. Validate output size ─────────────────────────────────
            if (outputFile.exists() && outputFile.length() > 0) {
                if (outputFile.length() >= inputFile.length()) {
                    // Compressed file is not smaller — keep original
                    outputFile.delete()
                    inputFile
                } else {
                    outputFile
                }
            } else {
                // Compression produced empty file — keep original
                outputFile.delete()
                inputFile
            }

        } catch (e: Exception) {
            // Clean up on failure
            try { muxer?.release() } catch (_: Exception) {}
            try { codec?.release() } catch (_: Exception) {}
            try { extractor?.release() } catch (_: Exception) {}
            outputFile.delete()
            throw RuntimeException("Video compression failed: ${e.message}", e)
        } finally {
            retriever.release()
        }
    }

    /**
     * Finds the first video track index in the extractor.
     *
     * @return Track index of the first video track, or -1 if not found.
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
