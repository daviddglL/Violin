package com.violinmaster.app.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Build
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.sin

class ViolinAudioEngine @Inject constructor() {
    private val sampleRate = 44100
    private val scope = CoroutineScope(Dispatchers.Default)

    // Reference Pitch Playback state
    private var toneJob: Job? = null
    private var isTonePlaying = false

    // Metronome state
    private var metronomeJob: Job? = null
    private var isMetronomeRunning = false
    private var bpm = 120
    private var timeSignatureBeats = 4
    private var accentDownbeat = true
    private var currentBeatIndex = 0 // 0 to beats-1

    // Listener to pulse the UI back in sync with the beat
    private var onBeatPulseListener: ((beatIndex: Int) -> Unit)? = null

    fun setOnBeatPulseListener(listener: (beatIndex: Int) -> Unit) {
        this.onBeatPulseListener = listener
    }

    /**
     * Synthesizes and returns a short, decaying sine wave click audio buffer.
     * frequency: pitch of the click (Hz)
     * durationMs: duration in milliseconds
     */
    private fun generateClickBuffer(frequency: Double, durationMs: Int): ShortArray {
        val durationSamples = (sampleRate * (durationMs / 1000.0)).toInt()
        val buffer = ShortArray(durationSamples)
        for (i in 0 until durationSamples) {
            val t = i.toDouble() / sampleRate
            // Linear decay factor
            val decay = (durationSamples - i).toDouble() / durationSamples
            // Form a decaying sine wave
            val amplitude = Short.MAX_VALUE * 0.8 * decay
            buffer[i] = (amplitude * sin(2.0 * Math.PI * frequency * t)).toInt().toShort()
        }
        return buffer
    }

    /**
     * Synthesizes continuous play buffer for reference strings
     */
    private fun playContinuousFrequency(frequency: Double) {
        stopTone()
        isTonePlaying = true

        toneJob = scope.launch {
            val bufferSize = 8192
            val shortBuffer = ShortArray(bufferSize)

            // Instantiate AudioTrack
            val track = try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    AudioTrack.Builder()
                        .setAudioAttributes(
                            AudioAttributes.Builder()
                                .setUsage(AudioAttributes.USAGE_MEDIA)
                                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                                .build()
                        )
                        .setAudioFormat(
                            AudioFormat.Builder()
                                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                                .setSampleRate(sampleRate)
                                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                                .build()
                        )
                        .setBufferSizeInBytes(bufferSize * 2)
                        .setTransferMode(AudioTrack.MODE_STREAM)
                        .build()
                } else {
                    @Suppress("DEPRECATION")
                    AudioTrack(
                        AudioManager.STREAM_MUSIC,
                        sampleRate,
                        AudioFormat.CHANNEL_OUT_MONO,
                        AudioFormat.ENCODING_PCM_16BIT,
                        bufferSize * 2,
                        AudioTrack.MODE_STREAM
                    )
                }
            } catch (e: Exception) {
                Log.e("ViolinAudioEngine", "Failed to create tone AudioTrack: ${e.message}")
                return@launch
            }

            try {
                track.play()
                var phase = 0.0
                val phaseIncrement = 2.0 * Math.PI * frequency / sampleRate

                while (isActive && isTonePlaying) {
                    for (i in 0 until bufferSize) {
                        shortBuffer[i] = (sin(phase) * Short.MAX_VALUE * 0.5).toInt().toShort()
                        phase += phaseIncrement
                        if (phase > 2.0 * Math.PI) {
                            phase -= 2.0 * Math.PI
                        }
                    }
                    track.write(shortBuffer, 0, bufferSize)
                }
            } catch (e: Exception) {
                Log.e("ViolinAudioEngine", "Tone loop writing error: ${e.message}")
            } finally {
                try {
                    track.stop()
                } catch (e: Exception) {
                    // ignore
                }
                try {
                    track.release()
                } catch (e: Exception) {
                    // ignore
                }
            }
        }
    }

    fun playCustomFrequency(hz: Double) {
        playContinuousFrequency(hz)
    }

    fun playStringTone(noteName: String, referencePitchA: Int = 440) {
        // G3 (approx 196Hz), D4 (approx 293.7Hz), A4 (approx 440Hz), E5 (approx 659.3Hz)
        val ratioToA = when (noteName.uppercase()) {
            "G" -> 196.0 / 440.0
            "D" -> 293.66 / 440.0
            "A" -> 1.0
            "E" -> 659.25 / 440.0
            else -> 1.0
        }
        val targetFreq = referencePitchA * ratioToA
        playContinuousFrequency(targetFreq)
    }

    fun stopTone() {
        isTonePlaying = false
        toneJob?.cancel()
        toneJob = null
    }

    fun isTonePlaying(): Boolean = isTonePlaying

    // --- Metronome Implementation ---

    fun startMetronome(bps: Int, beats: Int, accent: Boolean) {
        stopMetronome()
        this.bpm = bps
        this.timeSignatureBeats = beats
        this.accentDownbeat = accent
        this.isMetronomeRunning = true
        this.currentBeatIndex = 0

        metronomeJob = scope.launch {
            // Pre-generate standard click buffers to reduce CPU cycle work in the timer loop
            // Beat 1 (Accent Downbeat) = 1800 Hz click
            // Beats 2, 3, 4 (Weak Beat) = 1100 Hz click
            val accentClick = generateClickBuffer(1800.0, 45)
            val normalClick = generateClickBuffer(1100.0, 30)

            val track = try {
                val minBufferSize = AudioTrack.getMinBufferSize(
                    sampleRate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT
                )
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    AudioTrack.Builder()
                        .setAudioAttributes(
                            AudioAttributes.Builder()
                                .setUsage(AudioAttributes.USAGE_MEDIA)
                                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                                .build()
                        )
                        .setAudioFormat(
                            AudioFormat.Builder()
                                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                                .setSampleRate(sampleRate)
                                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                                .build()
                        )
                        .setBufferSizeInBytes(minBufferSize)
                        .setTransferMode(AudioTrack.MODE_STREAM)
                        .build()
                } else {
                    @Suppress("DEPRECATION")
                    AudioTrack(
                        AudioManager.STREAM_MUSIC,
                        sampleRate,
                        AudioFormat.CHANNEL_OUT_MONO,
                        AudioFormat.ENCODING_PCM_16BIT,
                        minBufferSize,
                        AudioTrack.MODE_STREAM
                    )
                }
            } catch (e: Exception) {
                Log.e("ViolinAudioEngine", "Failed to create metronome AudioTrack: ${e.message}")
                return@launch
            }

            try {
                track.play()

                while (isActive && isMetronomeRunning) {
                    val startNanos = System.nanoTime()

                    val isFirstBeat = currentBeatIndex == 0
                    val currentBuffer = if (isFirstBeat && accentDownbeat) accentClick else normalClick

                    // Sound click
                    track.write(currentBuffer, 0, currentBuffer.size)

                    // Pulse the UI callback
                    val finalBeatIndex = currentBeatIndex
                    launch(Dispatchers.Main) {
                        onBeatPulseListener?.invoke(finalBeatIndex)
                    }

                    // Prepare for next beat index
                    currentBeatIndex = (currentBeatIndex + 1) % timeSignatureBeats

                    // Duration of one beat in nanoseconds
                    val beatDurationNanos = (60.0 / bpm * 1_000_000_000).toLong()
                    val elapsedNanos = System.nanoTime() - startNanos

                    // Accurate timer sleep safely using corroutine delays without CPU spin locks
                    val sleepMs = (beatDurationNanos - elapsedNanos) / 1_000_000
                    if (sleepMs > 0) {
                        delay(sleepMs)
                    }
                }
            } catch (e: Exception) {
                Log.e("ViolinAudioEngine", "Metronome loop error: ${e.message}")
            } finally {
                try {
                    track.stop()
                    track.release()
                } catch (e: Exception) {
                    // ignore
                }
            }
        }
    }

    fun stopMetronome() {
        isMetronomeRunning = false
        metronomeJob?.cancel()
        metronomeJob = null
    }

    fun setBpm(newBpm: Int) {
        this.bpm = newBpm
    }

    fun setTimeSignature(beats: Int) {
        this.timeSignatureBeats = beats
    }

    fun setAccent(accent: Boolean) {
        this.accentDownbeat = accent
    }

    fun isMetronomeRunning(): Boolean = isMetronomeRunning

    fun releaseAll() {
        stopTone()
        stopMetronome()
    }
}
