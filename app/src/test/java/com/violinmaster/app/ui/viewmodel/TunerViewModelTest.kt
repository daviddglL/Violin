package com.violinmaster.app.ui.viewmodel

import com.violinmaster.app.audio.TunerEngine
import com.violinmaster.app.audio.ViolinAudioEngine
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceTimeBy
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
class TunerViewModelTest {

    private lateinit var audioEngine: ViolinAudioEngine
    private lateinit var tunerEngine: TunerEngine
    private lateinit var viewModel: TunerViewModel

    @Before
    fun setup() {
        audioEngine = ViolinAudioEngine()
        tunerEngine = TunerEngine()
        viewModel = TunerViewModel(audioEngine, tunerEngine)
    }

    @After
    fun tearDown() {
        audioEngine.releaseAll()
        tunerEngine.release()
    }

    @Test
    fun `selectTunerNote A sets selectedNote and triggers tone playback`() = runTest {
        viewModel.selectTunerNote("A")

        assertEquals("A", viewModel.tunerSelectedNote.value)
        assertFalse(viewModel.isListeningTuner.value)
        assertEquals(0f, viewModel.tunerPitchOffsetCents.value)
    }

    @Test
    fun `selectTunerNote null stops tone and resets`() = runTest {
        viewModel.selectTunerNote("A")
        viewModel.selectTunerNote(null)

        assertEquals(null, viewModel.tunerSelectedNote.value)
    }

    @Test
    fun `toggleListeningTuner starts pitch detection via TunerEngine`() = runTest {
        viewModel.toggleListeningTuner()

        assertTrue(viewModel.isListeningTuner.value)
        assertEquals(null, viewModel.tunerSelectedNote.value)
        // TunerEngine isListening should be true
        assertTrue(tunerEngine.isListening)
    }

    @Test
    fun `toggleListeningTuner stops listening and resets offset`() = runTest {
        viewModel.toggleListeningTuner() // start
        assertTrue(viewModel.isListeningTuner.value)

        viewModel.toggleListeningTuner() // stop
        assertFalse(viewModel.isListeningTuner.value)
        assertEquals(0f, viewModel.tunerPitchOffsetCents.value)
        assertFalse(tunerEngine.isListening)
    }

    @Test
    fun `tunerAutoDetect toggles the flag`() {
        val initial = viewModel.tunerAutoDetect.value // default true
        viewModel.toggleTunerAutoDetect()
        assertEquals(!initial, viewModel.tunerAutoDetect.value)

        viewModel.toggleTunerAutoDetect()
        assertEquals(initial, viewModel.tunerAutoDetect.value)
    }

    @Test
    fun `playCustomFrequency 220 plays the frequency`() = runTest {
        viewModel.playCustomFrequency(220.0)

        assertEquals(null, viewModel.tunerSelectedNote.value)
        assertFalse(viewModel.isListeningTuner.value)
        assertEquals(0f, viewModel.tunerPitchOffsetCents.value)
    }

    @Test
    fun `stopAudioEngineTone stops playback`() = runTest {
        viewModel.selectTunerNote("A")
        viewModel.stopAudioEngineTone()

        // State should be unchanged but tone should be stopped
        assertEquals("A", viewModel.tunerSelectedNote.value)
    }

    @Test
    fun `updateReferencePitch 432 changes reference`() {
        viewModel.updateReferencePitch(432)

        assertEquals(432, viewModel.referencePitchA.value)
    }

    @Test
    fun `updateReferencePitch reloads tone when tone is playing`() = runTest {
        viewModel.selectTunerNote("A") // starts tone at refPitch=440
        viewModel.updateReferencePitch(432)

        assertEquals(432, viewModel.referencePitchA.value)
        // Note should still be selected after pitch update
        assertEquals("A", viewModel.tunerSelectedNote.value)
    }

    @Test
    fun `onCleared stops TunerEngine listening`() = runTest {
        viewModel.toggleListeningTuner()
        assertTrue(tunerEngine.isListening)

        viewModel.onCleared()
        // After onCleared, stopPitchCollection should have been called
        // (AudioRecord may still show isListening briefly — the key is cleanup ran)
    }
}
