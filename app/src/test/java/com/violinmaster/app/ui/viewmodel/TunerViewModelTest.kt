package com.violinmaster.app.ui.viewmodel

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.violinmaster.app.audio.TunerEngine
import com.violinmaster.app.audio.ViolinAudioEngine
import com.violinmaster.app.data.UserAccount
import com.violinmaster.app.di.AuthManager
import com.violinmaster.app.di.TuningPreferencesManager
import com.violinmaster.app.domain.model.TuningConfiguration
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
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
    private lateinit var authManager: AuthManager
    private lateinit var tuningPreferencesManager: TuningPreferencesManager
    private lateinit var viewModel: TunerViewModel

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext<Context>()
        // Clean SharedPreferences before each test
        context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
            .edit().clear().commit()

        audioEngine = ViolinAudioEngine()
        tunerEngine = TunerEngine()
        authManager = AuthManager(context)
        authManager.saveCurrentUser(
            UserAccount(
                username = "test_user",
                role = "STUDENT",
                hashedPassword = "hash",
                salt = "salt"
            )
        )
        tuningPreferencesManager = TuningPreferencesManager(context, authManager)
        viewModel = TunerViewModel(audioEngine, tunerEngine, tuningPreferencesManager)
    }

    @After
    fun tearDown() {
        audioEngine.releaseAll()
        tunerEngine.release()
        context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
            .edit().clear().commit()
    }

    // ═══════════════════════════════════════════════════════════════════
    // Existing tests (unchanged behavior)
    // ═══════════════════════════════════════════════════════════════════

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
        assertEquals("A", viewModel.tunerSelectedNote.value)
    }

    @Test
    fun `toggleListeningTuner starts and stops engine`() = runTest {
        viewModel.toggleListeningTuner()
        assertTrue("Engine should be listening after toggle", tunerEngine.isListening)

        viewModel.toggleListeningTuner()
    }

    // ═══════════════════════════════════════════════════════════════════
    // NEW: maxCents state
    // ═══════════════════════════════════════════════════════════════════

    @Test
    fun `maxCents defaults to 50`() = runTest {
        assertEquals(50, viewModel.maxCents.value)
    }

    @Test
    fun `updateMaxCents changes maxCents state`() = runTest {
        viewModel.updateMaxCents(100)
        assertEquals(100, viewModel.maxCents.value)
    }

    @Test
    fun `updateMaxCents clamps to valid range 25-200`() = runTest {
        viewModel.updateMaxCents(10)
        assertEquals(25, viewModel.maxCents.value)

        viewModel.updateMaxCents(300)
        assertEquals(200, viewModel.maxCents.value)
    }

    // ═══════════════════════════════════════════════════════════════════
    // NEW: presets and CRUD
    // ═══════════════════════════════════════════════════════════════════

    @Test
    fun `presets StateFlow starts empty`() = runTest {
        val presets = viewModel.presets.first()
        assertTrue("Presets should start empty", presets.isEmpty())
    }

    @Test
    fun `saveCurrentAsPreset persists current configuration`() = runTest {
        // Set up current values
        viewModel.updateReferencePitch(432)
        viewModel.updateMaxCents(100)

        viewModel.saveCurrentAsPreset("Custom 432")

        val presets = viewModel.presets.first()
        assertEquals("Should have 1 preset", 1, presets.size)
        assertEquals("Custom 432", presets[0].label)
        assertEquals(432, presets[0].referencePitch)
        assertEquals(100, presets[0].maxCents)
    }

    @Test
    fun `saveCurrentAsPreset with duplicate label overwrites`() = runTest {
        viewModel.updateReferencePitch(440)
        viewModel.updateMaxCents(50)
        viewModel.saveCurrentAsPreset("My Preset")

        viewModel.updateReferencePitch(415)
        viewModel.updateMaxCents(25)
        viewModel.saveCurrentAsPreset("My Preset")

        val presets = viewModel.presets.first()
        assertEquals("Should have 1 preset (overwritten)", 1, presets.size)
        assertEquals(415, presets[0].referencePitch)
        assertEquals(25, presets[0].maxCents)
    }

    @Test
    fun `loadPreset applies preset values to current state`() = runTest {
        // Save a preset
        viewModel.updateReferencePitch(440)
        viewModel.updateMaxCents(50)
        viewModel.saveCurrentAsPreset("Baroque 415")

        // Create second preset and save
        viewModel.updateReferencePitch(415)
        viewModel.updateMaxCents(25)
        viewModel.saveCurrentAsPreset("Baroque 415") // overwrites

        // Change current state
        viewModel.updateReferencePitch(440)
        viewModel.updateMaxCents(100)

        // Load the preset
        viewModel.loadPreset("Baroque 415")

        assertEquals(415, viewModel.referencePitchA.value)
        assertEquals(25, viewModel.maxCents.value)
    }

    @Test
    fun `loadPreset with non-existent label does nothing`() = runTest {
        viewModel.updateReferencePitch(440)
        viewModel.updateMaxCents(50)

        viewModel.loadPreset("DoesNotExist")

        assertEquals(440, viewModel.referencePitchA.value)
        assertEquals(50, viewModel.maxCents.value)
    }

    @Test
    fun `deletePreset removes preset by label`() = runTest {
        viewModel.updateReferencePitch(440)
        viewModel.updateMaxCents(50)
        viewModel.saveCurrentAsPreset("Preset A")

        viewModel.updateReferencePitch(415)
        viewModel.saveCurrentAsPreset("Preset B")

        viewModel.deletePreset("Preset A")

        val presets = viewModel.presets.first()
        assertEquals("Should have 1 preset after delete", 1, presets.size)
        assertEquals("Preset B", presets[0].label)
    }

    @Test
    fun `deletePreset with non-existent label does nothing`() = runTest {
        viewModel.saveCurrentAsPreset("Only Preset")
        viewModel.deletePreset("Ghost")

        val presets = viewModel.presets.first()
        assertEquals("Should still have 1 preset", 1, presets.size)
    }

    @Test
    fun `saveCurrentAsPreset preserves multiple distinct presets`() = runTest {
        viewModel.updateReferencePitch(440)
        viewModel.updateMaxCents(50)
        viewModel.saveCurrentAsPreset("Default")

        viewModel.updateReferencePitch(415)
        viewModel.updateMaxCents(25)
        viewModel.saveCurrentAsPreset("Baroque")

        viewModel.updateReferencePitch(442)
        viewModel.updateMaxCents(75)
        viewModel.saveCurrentAsPreset("Orchestra")

        val presets = viewModel.presets.first()
        assertEquals("Should have 3 presets", 3, presets.size)
        assertEquals("Default", presets[0].label)
        assertEquals("Baroque", presets[1].label)
        assertEquals("Orchestra", presets[2].label)
    }

    // ═══════════════════════════════════════════════════════════════════
    // NEW: input validation guards
    // ═══════════════════════════════════════════════════════════════════

    @Test
    fun `updateReferencePitch clamps below 350 to 350`() {
        viewModel.updateReferencePitch(300)
        assertEquals(350, viewModel.referencePitchA.value)
    }

    @Test
    fun `updateReferencePitch clamps above 500 to 500`() {
        viewModel.updateReferencePitch(600)
        assertEquals(500, viewModel.referencePitchA.value)
    }

    @Test
    fun `updateReferencePitch accepts values within 350-500 as-is`() {
        viewModel.updateReferencePitch(350)
        assertEquals(350, viewModel.referencePitchA.value)

        viewModel.updateReferencePitch(440)
        assertEquals(440, viewModel.referencePitchA.value)

        viewModel.updateReferencePitch(500)
        assertEquals(500, viewModel.referencePitchA.value)
    }

    @Test
    fun `updateMaxCents snaps to nearest allowed value`() {
        viewModel.updateMaxCents(1)
        assertEquals(25, viewModel.maxCents.value)

        viewModel.updateMaxCents(30)
        assertEquals(25, viewModel.maxCents.value)

        viewModel.updateMaxCents(60)
        assertEquals(50, viewModel.maxCents.value)

        viewModel.updateMaxCents(80)
        assertEquals(75, viewModel.maxCents.value)

        viewModel.updateMaxCents(110)
        assertEquals(100, viewModel.maxCents.value)

        viewModel.updateMaxCents(140)
        assertEquals(150, viewModel.maxCents.value)

        viewModel.updateMaxCents(185)
        assertEquals(200, viewModel.maxCents.value)

        viewModel.updateMaxCents(999)
        assertEquals(200, viewModel.maxCents.value)
    }

    @Test
    fun `updateMaxCents with values near boundary snaps correctly`() {
        // 37 is closer to 25 than 50
        viewModel.updateMaxCents(37)
        assertEquals(25, viewModel.maxCents.value)

        // 38 is closer to 50 than 25
        viewModel.updateMaxCents(38)
        assertEquals(50, viewModel.maxCents.value)

        // 87 is closer to 100 than 75
        viewModel.updateMaxCents(88)
        assertEquals(100, viewModel.maxCents.value)

        // 125 is halfway between 100 and 150 → minByOrNull returns first (100)
        viewModel.updateMaxCents(125)
        assertEquals(100, viewModel.maxCents.value)
    }
}
