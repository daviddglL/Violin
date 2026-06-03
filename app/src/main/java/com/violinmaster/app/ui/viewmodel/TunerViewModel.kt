package com.violinmaster.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.violinmaster.app.audio.TunerEngine
import com.violinmaster.app.audio.ViolinAudioEngine
import com.violinmaster.app.di.TuningPreferencesManager
import com.violinmaster.app.domain.model.TuningConfiguration
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
    private val tunerEngine: TunerEngine,
    private val tuningPreferencesManager: TuningPreferencesManager
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

    // --- Max Cents Range ---
    private val _maxCents = MutableStateFlow(50)
    val maxCents: StateFlow<Int> = _maxCents.asStateFlow()

    /**
     * Update the maximum cents range for the tuning gauge.
     * Valid values are clamped to 25–200 (discrete step: 25).
     */
    fun updateMaxCents(cents: Int) {
        _maxCents.value = cents.coerceIn(25, 200)
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

    // --- Presets (delegated to TuningPreferencesManager) ---
    val presets: StateFlow<List<TuningConfiguration>> = tuningPreferencesManager.presets

    /**
     * Save the current referencePitch and maxCents as a named preset.
     * Overwrites any existing preset with the same [label].
     */
    fun saveCurrentAsPreset(label: String) {
        val config = TuningConfiguration(
            label = label,
            referencePitch = _referencePitchA.value,
            maxCents = _maxCents.value
        )
        tuningPreferencesManager.saveConfig(config)
    }

    /**
     * Load a preset by [label] and apply its referencePitch and maxCents
     * to the current tuning state.
     */
    fun loadPreset(label: String) {
        val config = tuningPreferencesManager.loadConfig(label) ?: return
        _referencePitchA.value = config.referencePitch
        _maxCents.value = config.maxCents
    }

    /**
     * Delete the preset with the given [label].
     */
    fun deletePreset(label: String) {
        tuningPreferencesManager.deleteConfig(label)
    }

    // --- Tuner note selection ---

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
                    // Pitch offset is NOT clamped — actual cents value from detection
                    _tunerPitchOffsetCents.value = result.cents

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
