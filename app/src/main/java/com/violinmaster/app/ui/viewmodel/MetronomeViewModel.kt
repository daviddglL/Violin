package com.violinmaster.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.violinmaster.app.audio.ViolinAudioEngine
import com.violinmaster.app.data.AnalyticsHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class MetronomeViewModel @Inject constructor(
    private val audioEngine: ViolinAudioEngine,
    private val analyticsHelper: AnalyticsHelper
) : ViewModel() {

    // --- Metronome Control State ---
    private val _metronomeBpm = MutableStateFlow(100)
    val metronomeBpm: StateFlow<Int> = _metronomeBpm.asStateFlow()

    private val _metronomeBeats = MutableStateFlow(4)
    val metronomeBeats: StateFlow<Int> = _metronomeBeats.asStateFlow()

    private val _metronomeAccent = MutableStateFlow(true)
    val metronomeAccent: StateFlow<Boolean> = _metronomeAccent.asStateFlow()

    private val _isMetronomePlaying = MutableStateFlow(false)
    val isMetronomePlaying: StateFlow<Boolean> = _isMetronomePlaying.asStateFlow()

    private val _metronomeBeatPulse = MutableStateFlow(-1)
    val metronomeBeatPulse: StateFlow<Int> = _metronomeBeatPulse.asStateFlow()

    init {
        audioEngine.setOnBeatPulseListener(::onBeatPulse)
    }

    internal fun onBeatPulse(beatIndex: Int) {
        _metronomeBeatPulse.value = beatIndex
    }

    fun toggleMetronome() {
        if (_isMetronomePlaying.value) {
            audioEngine.stopMetronome()
            _isMetronomePlaying.value = false
            _metronomeBeatPulse.value = -1
        } else {
            audioEngine.startMetronome(
                _metronomeBpm.value,
                _metronomeBeats.value,
                _metronomeAccent.value
            )
            _isMetronomePlaying.value = true
        }
    }

    fun updateMetronomeBpm(bpm: Int) {
        val clampedBpm = bpm.coerceIn(40, 240)
        _metronomeBpm.value = clampedBpm
        audioEngine.setBpm(clampedBpm)
    }

    fun updateMetronomeBeats(beats: Int) {
        val clampedBeats = beats.coerceIn(1, 8)
        _metronomeBeats.value = clampedBeats
        audioEngine.setTimeSignature(clampedBeats)
        if (_isMetronomePlaying.value) {
            audioEngine.startMetronome(_metronomeBpm.value, _metronomeBeats.value, _metronomeAccent.value)
        }
    }

    fun toggleMetronomeAccent() {
        _metronomeAccent.value = !_metronomeAccent.value
        audioEngine.setAccent(_metronomeAccent.value)
    }

    override fun onCleared() {
        super.onCleared()
        if (_isMetronomePlaying.value) {
            audioEngine.stopMetronome()
        }
    }
}
