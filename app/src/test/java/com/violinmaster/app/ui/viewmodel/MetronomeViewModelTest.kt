package com.violinmaster.app.ui.viewmodel

import com.violinmaster.app.audio.ViolinAudioEngine
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class MetronomeViewModelTest {

    private lateinit var audioEngine: ViolinAudioEngine

    @Before
    fun setup() {
        audioEngine = ViolinAudioEngine()
    }

    @After
    fun tearDown() {
        audioEngine.releaseAll()
    }

    @Test
    fun `toggleMetronome starts when stopped`() = runTest {
        val viewModel = MetronomeViewModel(audioEngine)

        viewModel.toggleMetronome()

        assertTrue(viewModel.isMetronomePlaying.value)
    }

    @Test
    fun `toggleMetronome stops when playing`() = runTest {
        val viewModel = MetronomeViewModel(audioEngine)
        viewModel.toggleMetronome() // start
        assertTrue(viewModel.isMetronomePlaying.value)

        viewModel.toggleMetronome() // stop

        assertFalse(viewModel.isMetronomePlaying.value)
        assertEquals(-1, viewModel.metronomeBeatPulse.value)
    }

    @Test
    fun `updateMetronomeBpm 120 sets bpm in range 40 to 240`() = runTest {
        val viewModel = MetronomeViewModel(audioEngine)

        viewModel.updateMetronomeBpm(120)

        assertEquals(120, viewModel.metronomeBpm.value)
    }

    @Test
    fun `updateMetronomeBpm 300 clamps to 240`() = runTest {
        val viewModel = MetronomeViewModel(audioEngine)

        viewModel.updateMetronomeBpm(300)

        assertEquals(240, viewModel.metronomeBpm.value)
    }

    @Test
    fun `updateMetronomeBpm 20 clamps to 40`() = runTest {
        val viewModel = MetronomeViewModel(audioEngine)

        viewModel.updateMetronomeBpm(20)

        assertEquals(40, viewModel.metronomeBpm.value)
    }

    @Test
    fun `updateMetronomeBeats 3 sets time signature in range 1 to 8`() = runTest {
        val viewModel = MetronomeViewModel(audioEngine)

        viewModel.updateMetronomeBeats(3)

        assertEquals(3, viewModel.metronomeBeats.value)
    }

    @Test
    fun `updateMetronomeBeats 12 clamps to 8`() = runTest {
        val viewModel = MetronomeViewModel(audioEngine)

        viewModel.updateMetronomeBeats(12)

        assertEquals(8, viewModel.metronomeBeats.value)
    }

    @Test
    fun `updateMetronomeBeats 0 clamps to 1`() = runTest {
        val viewModel = MetronomeViewModel(audioEngine)

        viewModel.updateMetronomeBeats(0)

        assertEquals(1, viewModel.metronomeBeats.value)
    }

    @Test
    fun `toggleMetronomeAccent toggles accent flag`() = runTest {
        val viewModel = MetronomeViewModel(audioEngine)
        val initial = viewModel.metronomeAccent.value // default true

        viewModel.toggleMetronomeAccent()

        assertEquals(!initial, viewModel.metronomeAccent.value)
    }

    @Test
    fun `metronomeBeatPulse updates on beat callback`() = runTest {
        val viewModel = MetronomeViewModel(audioEngine)

        // Simulate the audio engine sending a beat pulse
        viewModel.onBeatPulse(2)

        assertEquals(2, viewModel.metronomeBeatPulse.value)
    }
}
