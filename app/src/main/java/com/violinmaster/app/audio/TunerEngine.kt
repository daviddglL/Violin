package com.violinmaster.app.audio

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import com.violinmaster.app.audio.tuner.YinPitchDetector
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Real-time violin tuner using microphone capture and YIN pitch detection.
 *
 * Captures 44100Hz 16-bit mono audio via [AudioRecord], processes in 4096-sample
 * buffers on [Dispatchers.Default], and applies YIN algorithm via [YinPitchDetector].
 * Results are emitted as [PitchResult] objects on [pitchFlow].
 *
 * Usage:
 * ```kotlin
 * tunerEngine.startListening(referencePitchA = 440)
 * tunerEngine.pitchFlow.collect { result -> /* update UI */ }
 * tunerEngine.stopListening()
 * ```
 */
@Singleton
class TunerEngine @Inject constructor() {

    companion object {
        private const val TAG = "TunerEngine"
        private const val SAMPLE_RATE = 44100
        private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
        private const val BUFFER_SIZE_SAMPLES = 4096
    }

    private val _pitchFlow = MutableStateFlow<PitchResult?>(null)
    val pitchFlow: StateFlow<PitchResult?> = _pitchFlow.asStateFlow()

    private var audioRecord: AudioRecord? = null
    private var captureJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default)

    @Volatile
    var isListening: Boolean = false
        private set

    /**
     * Start capturing microphone audio and detecting pitch.
     *
     * Emits raw [PitchResult] values (frequency + YIN confidence) via [pitchFlow].
     * Note-to-string mapping is performed downstream by the ViewModel using the
     * active instrument — the engine stays instrument-agnostic.
     */
    fun startListening() {
        if (isListening) return

        val bufferSizeInBytes = BUFFER_SIZE_SAMPLES * 2 // 16-bit = 2 bytes per sample
        val minBufferSize = AudioRecord.getMinBufferSize(
            SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT
        )
        val actualBufferSize = maxOf(bufferSizeInBytes, minBufferSize)

        try {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                CHANNEL_CONFIG,
                AUDIO_FORMAT,
                actualBufferSize
            )

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                Log.e(TAG, "AudioRecord failed to initialize")
                audioRecord?.release()
                audioRecord = null
                return
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create AudioRecord: ${e.message}")
            _pitchFlow.value = null
            return
        }

        isListening = true
        audioRecord?.startRecording()

        captureJob = scope.launch {
            val shortBuffer = ShortArray(BUFFER_SIZE_SAMPLES)

            try {
                while (isActive && isListening) {
                    val readResult = audioRecord?.read(shortBuffer, 0, BUFFER_SIZE_SAMPLES) ?: -1

                    if (readResult > 0) {
                        // Run YIN pitch detection on the PCM buffer.
                        // Note mapping is performed downstream by the ViewModel using
                        // the active instrument — TunerEngine emits raw frequency only.
                        val pitchResult = YinPitchDetector.detectPitch(
                            buffer = shortBuffer,
                            sampleRate = SAMPLE_RATE,
                            threshold = 0.15f,
                            minFrequency = 50f
                        )

                        _pitchFlow.value = pitchResult
                    } else if (readResult < 0) {
                        Log.w(TAG, "AudioRecord read error: $readResult")
                        break
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Audio capture error: ${e.message}")
            }
        }
    }

    /**
     * Stop microphone capture and pitch detection.
     * Reference tone playback (via [ViolinAudioEngine]) is unaffected.
     */
    fun stopListening() {
        isListening = false
        captureJob?.cancel()
        captureJob = null

        try {
            audioRecord?.stop()
        } catch (e: Exception) {
            Log.w(TAG, "Error stopping AudioRecord: ${e.message}")
        }
        try {
            audioRecord?.release()
        } catch (e: Exception) {
            Log.w(TAG, "Error releasing AudioRecord: ${e.message}")
        }
        audioRecord = null

        _pitchFlow.value = null
    }

    /**
     * Release all resources. Call when the engine is no longer needed.
     */
    fun release() {
        stopListening()
    }
}
