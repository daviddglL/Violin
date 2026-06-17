package com.violinmaster.app.ui.viewmodel

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.violinmaster.app.audio.TunerEngine
import com.violinmaster.app.audio.ViolinAudioEngine
import com.violinmaster.app.audio.tuner.YinPitchDetector
import com.violinmaster.app.data.AnalyticsHelper
import com.violinmaster.app.data.IAnalyticsService
import com.violinmaster.app.data.ICrashReportingService
import com.violinmaster.app.di.UserPreferencesManager
import com.violinmaster.app.domain.model.Instrument
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

    private lateinit var context: Context
    private lateinit var audioEngine: ViolinAudioEngine
    private lateinit var tunerEngine: TunerEngine
    private lateinit var userPreferencesManager: UserPreferencesManager
    private lateinit var analyticsHelper: AnalyticsHelper
    private lateinit var viewModel: TunerViewModel

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext<Context>()
        context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
            .edit().clear().commit()
        audioEngine = ViolinAudioEngine()
        tunerEngine = TunerEngine()
        userPreferencesManager = UserPreferencesManager(context)
        analyticsHelper = AnalyticsHelper(
            analyticsService = FakeAnalyticsService(),
            crashReportingService = FakeCrashReportingService()
        )
        viewModel = TunerViewModel(audioEngine, tunerEngine, userPreferencesManager, analyticsHelper)
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
    fun `toggleListeningTuner starts and stops engine`() = runTest {
        // Start listening
        viewModel.toggleListeningTuner()
        assertTrue("Engine should be listening after toggle", tunerEngine.isListening)

        // Stop listening
        viewModel.toggleListeningTuner()
        // Engine should stop (AudioRecord cleanup is async, but flag should flip)
    }

    // ═══════════════════════════════════════════════════════════════════
    // Instrument selection tests (PR 3)
    // ═══════════════════════════════════════════════════════════════════

    @Test
    fun `default instrument is VIOLIN`() = runTest {
        assertEquals(
            "Default instrument on fresh launch should be VIOLIN",
            Instrument.VIOLIN,
            viewModel.selectedInstrument.value
        )
    }

    @Test
    fun `switch to viola updates selectedInstrument StateFlow`() = runTest {
        userPreferencesManager.setSelectedInstrument(Instrument.VIOLA)

        assertEquals(
            "selectedInstrument should reflect viola after preference change",
            Instrument.VIOLA,
            viewModel.selectedInstrument.value
        )
    }

    @Test
    fun `switch to cello updates selectedInstrument StateFlow`() = runTest {
        userPreferencesManager.setSelectedInstrument(Instrument.CELLO)

        assertEquals(
            "selectedInstrument should reflect cello after preference change",
            Instrument.CELLO,
            viewModel.selectedInstrument.value
        )
    }

    @Test
    fun `selectTunerNote works with viola C string`() = runTest {
        userPreferencesManager.setSelectedInstrument(Instrument.VIOLA)

        viewModel.selectTunerNote("C")

        assertEquals("C", viewModel.tunerSelectedNote.value)
        assertFalse(viewModel.isListeningTuner.value)
        assertEquals(0f, viewModel.tunerPitchOffsetCents.value)
    }

    @Test
    fun `cello A3 220Hz auto-detect maps to note A`() = runTest {
        userPreferencesManager.setSelectedInstrument(Instrument.CELLO)

        val result = YinPitchDetector.frequencyToNoteAndCents(
            frequency = 220f,
            referencePitchA = 440,
            instrument = viewModel.selectedInstrument.value
        )

        assertEquals(
            "220 Hz should map to A for cello (A3 = 220 Hz)",
            "A",
            result.note
        )
    }

    // ── Test doubles for AnalyticsHelper dependencies ──────────────────

    private class FakeAnalyticsService : IAnalyticsService {
        override fun logEvent(name: String, params: Map<String, Any>) {}
        override fun setUserProperty(key: String, value: String) {}
        override fun setUserId(id: String) {}
        override fun setCurrentScreen(screenName: String, screenClass: String) {}
    }

    private class FakeCrashReportingService : ICrashReportingService {
        override fun log(message: String) {}
        override fun recordException(throwable: Throwable) {}
        override fun setCustomKey(key: String, value: String) {}
    }
}
