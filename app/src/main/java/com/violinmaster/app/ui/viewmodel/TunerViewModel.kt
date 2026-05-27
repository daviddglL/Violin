package com.violinmaster.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.violinmaster.app.audio.TunerEngine
import com.violinmaster.app.audio.ViolinAudioEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TunerViewModel @Inject constructor(
    private val audioEngine: ViolinAudioEngine,
    private val tunerEngine: TunerEngine
) : ViewModel() {

    // --- Reference Pitch A ---
    private val _referencePitchA = MutableStateFlow(440)
    val referencePitchA: StateFlow<Int> = _referencePitchA.asStateFlow()

    fun updateReferencePitch(pitch: Int) {
        _referencePitchA.value = pitch
        if (audioEngine.isTonePlaying()) {
            _tunerSelectedNote.value?.let { note ->
                audioEngine.playStringTone(note, pitch)
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
            audioEngine.playStringTone(note, _referencePitchA.value)
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

        tunerEngine.startListening(_referencePitchA.value)

        pitchCollectionJob = viewModelScope.launch {
            tunerEngine.pitchFlow.collect { result ->
                if (result != null) {
                    _tunerPitchOffsetCents.value = result.cents.coerceIn(-50f, 50f)

                    // Auto-detect: update selected note when close to a violin string
                    if (_tunerAutoDetect.value && result.note != null && result.confidence > 0.3f) {
                        _tunerSelectedNote.value = result.note
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

    override fun onCleared() {
        super.onCleared()
        stopPitchCollection()
    }
}
