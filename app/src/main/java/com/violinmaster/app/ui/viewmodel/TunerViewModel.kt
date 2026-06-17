package com.violinmaster.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.violinmaster.app.audio.TunerEngine
import com.violinmaster.app.audio.ViolinAudioEngine
import com.violinmaster.app.audio.tuner.YinPitchDetector
import com.violinmaster.app.data.AnalyticsHelper
import com.violinmaster.app.di.UserPreferencesManager
import com.violinmaster.app.domain.model.Instrument
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TunerViewModel @Inject constructor(
    private val audioEngine: ViolinAudioEngine,
    private val tunerEngine: TunerEngine,
    private val userPreferencesManager: UserPreferencesManager,
    private val analyticsHelper: AnalyticsHelper
) : ViewModel() {

    // --- Active Instrument ---
    val selectedInstrument: StateFlow<Instrument> = userPreferencesManager.selectedInstrument

    init {
        // Update selected note on instrument change: re-resolve frequency if the
        // note exists in the new instrument, clear it if the string no longer exists.
        viewModelScope.launch {
            selectedInstrument.drop(1).collect { newInstrument ->
                val currentNote = _tunerSelectedNote.value
                if (currentNote != null) {
                    if (newInstrument.strings.any { it.name == currentNote }) {
                        // Note exists in both — re-play at the new instrument's frequency
                        selectTunerNote(currentNote)
                    } else {
                        // Note is absent — clear selection
                        selectTunerNote(null)
                    }
                }
            }
        }
    }

    // --- Reference Pitch A ---
    private val _referencePitchA = MutableStateFlow(440)
    val referencePitchA: StateFlow<Int> = _referencePitchA.asStateFlow()

    fun updateReferencePitch(pitch: Int) {
        _referencePitchA.value = pitch
        if (audioEngine.isTonePlaying()) {
            _tunerSelectedNote.value?.let { note ->
                audioEngine.playStringTone(note, pitch, selectedInstrument.value)
            }
        }
    }

    // --- Tuner Control State ---
    private val _tunerSelectedNote = MutableStateFlow<String?>("A")
    val tunerSelectedNote: StateFlow<String?> = _tunerSelectedNote.asStateFlow()

    private val _isListeningTuner = MutableStateFlow(false)
    val isListeningTuner: StateFlow<Boolean> = _isListeningTuner.asStateFlow()

    private val _tunerPitchOffsetCents = MutableStateFlow(0f)
    val tunerPitchOffsetCents: StateFlow<Float> = _tunerPitchOffsetCents.asStateFlow()

    private val _tunerAutoDetect = MutableStateFlow(true)
    val tunerAutoDetect: StateFlow<Boolean> = _tunerAutoDetect.asStateFlow()

    // TunerEngine pitch flow collection job
    private var pitchCollectionJob: Job? = null

    fun selectTunerNote(note: String?) {
        _tunerSelectedNote.value = note
        if (note != null) {
            audioEngine.playStringTone(note, _referencePitchA.value, selectedInstrument.value)
            _isListeningTuner.value = false
            stopPitchCollection()
            _tunerPitchOffsetCents.value = 0f
        } else {
            audioEngine.stopTone()
        }
    }

    fun playCustomFrequency(hz: Double) {
        _tunerSelectedNote.value = null
        _isListeningTuner.value = false
        stopPitchCollection()
        _tunerPitchOffsetCents.value = 0f
        audioEngine.playCustomFrequency(hz)
    }

    fun stopAudioEngineTone() {
        audioEngine.stopTone()
    }

    fun toggleListeningTuner() {
        if (_isListeningTuner.value) {
            // Stop listening
            _isListeningTuner.value = false
            stopPitchCollection()
            _tunerPitchOffsetCents.value = 0f
        } else {
            // Start listening
            selectTunerNote(null)
            _isListeningTuner.value = true
            startPitchCollection()
        }
    }

    private fun startPitchCollection() {
        stopPitchCollection()

        tunerEngine.startListening(minFrequencyHz = minFrequencyForInstrument(selectedInstrument.value))

        pitchCollectionJob = viewModelScope.launch {
            tunerEngine.pitchFlow.collect { result ->
                if (result != null) {
                    // Map frequency to the active instrument's strings
                    val instrument = selectedInstrument.value
                    val mapped = YinPitchDetector.frequencyToNoteAndCents(
                        frequency = result.frequency,
                        referencePitchA = _referencePitchA.value,
                        instrument = instrument
                    )
                    _tunerPitchOffsetCents.value = mapped.cents.coerceIn(-50f, 50f)

                    // Auto-detect: update selected note when close to an instrument string
                    if (_tunerAutoDetect.value && mapped.note != null && mapped.confidence > 0.3f) {
                        _tunerSelectedNote.value = mapped.note
                    }
                }
            }
        }
    }

    private fun stopPitchCollection() {
        pitchCollectionJob?.cancel()
        pitchCollectionJob = null
        tunerEngine.stopListening()
        _tunerPitchOffsetCents.value = 0f
    }

    fun toggleTunerAutoDetect() {
        _tunerAutoDetect.value = !_tunerAutoDetect.value
    }

    /**
     * Returns the minimum detectable frequency for the given instrument.
     * Uses 70% of the instrument's lowest string frequency, clamped to
     * a floor that avoids mains hum false positives (50/60 Hz).
     *
     * Instruments with strings below 60 Hz (e.g., double bass E1 = 41.2 Hz)
     * use a lower floor of [LOW_FREQ_FLOOR_HZ] to capture fundamentals,
     * trading some noise immunity for detection range.
     */
    private fun minFrequencyForInstrument(instrument: Instrument): Float {
        val lowestString = instrument.strings.minOf { it.frequency }
        val computed = (lowestString * 0.7).toFloat()
        val floor = if (lowestString < 60.0) LOW_FREQ_FLOOR_HZ else MIN_FREQ_HZ
        return maxOf(computed, floor)
    }

    override fun onCleared() {
        super.onCleared()
        stopPitchCollection()
        audioEngine.stopTone()
    }

    companion object {
        /** Safe pitch floor for violin/viola/cello — above 50/60 Hz mains hum. */
        private const val MIN_FREQ_HZ = 65f
        /** Lower floor for double bass — needed to detect E1 = 41.2 Hz. */
        private const val LOW_FREQ_FLOOR_HZ = 30f
    }
}
