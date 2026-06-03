package com.violinmaster.app.di

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.violinmaster.app.data.UserAccount
import com.violinmaster.app.domain.model.TuningConfiguration
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * TDD tests for TuningPreferencesManager.
 *
 * Manages per-user tuning presets via SharedPreferences.
 * Stores a JSON array of TuningConfiguration objects under key tuner_configs_{username}.
 *
 * RED phase: TuningPreferencesManager.kt does not exist yet.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class TuningPreferencesManagerTest {

    private lateinit var context: Context
    private lateinit var authManager: AuthManager
    private lateinit var manager: TuningPreferencesManager

    private val testUser = UserAccount(
        username = "test_violinist",
        role = "STUDENT",
        hashedPassword = "hash",
        salt = "salt"
    )

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext<Context>()
        // Clean SharedPreferences before each test
        context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
            .edit().clear().commit()
        authManager = AuthManager(context)
        authManager.saveCurrentUser(testUser)
        manager = TuningPreferencesManager(context, authManager)
    }

    @After
    fun tearDown() {
        context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
            .edit().clear().commit()
    }

    // ═══════════════════════════════════════════════════════════════════
    // Default state
    // ═══════════════════════════════════════════════════════════════════

    @Test
    fun `presets StateFlow starts empty when no saved configs`() = runTest {
        val presets = manager.presets.first()
        assertTrue("Presets should be empty initially", presets.isEmpty())
    }

    // ═══════════════════════════════════════════════════════════════════
    // Save and Load
    // ═══════════════════════════════════════════════════════════════════

    @Test
    fun `saveConfig persists a preset and emits updated list`() = runTest {
        val config = TuningConfiguration(
            label = "Baroque 415",
            referencePitch = 415,
            maxCents = 25
        )
        manager.saveConfig(config)

        val presets = manager.presets.first()
        assertEquals("Should have 1 preset after save", 1, presets.size)
        assertEquals("Baroque 415", presets[0].label)
        assertEquals(415, presets[0].referencePitch)
        assertEquals(25, presets[0].maxCents)
    }

    @Test
    fun `saveConfig persists across manager instances`() = runTest {
        manager.saveConfig(
            TuningConfiguration(label = "Orchestra 442", referencePitch = 442, maxCents = 50)
        )

        // Create a second manager with the same SharedPreferences
        val secondManager = TuningPreferencesManager(context, authManager)
        val presets = secondManager.presets.first()

        assertEquals("Should persist 1 preset across instances", 1, presets.size)
        assertEquals("Orchestra 442", presets[0].label)
    }

    @Test
    fun `saveConfig adds to existing presets`() = runTest {
        manager.saveConfig(TuningConfiguration(label = "Default", referencePitch = 440, maxCents = 50))
        manager.saveConfig(TuningConfiguration(label = "Baroque", referencePitch = 415, maxCents = 25))

        val presets = manager.presets.first()
        assertEquals("Should have 2 presets", 2, presets.size)
        assertEquals("Default", presets[0].label)
        assertEquals("Baroque", presets[1].label)
    }

    @Test
    fun `saveConfig overwrites preset with same label`() = runTest {
        manager.saveConfig(TuningConfiguration(label = "My Preset", referencePitch = 440, maxCents = 50))
        manager.saveConfig(TuningConfiguration(label = "My Preset", referencePitch = 432, maxCents = 100))

        val presets = manager.presets.first()
        assertEquals("Should still have 1 preset (overwritten)", 1, presets.size)
        assertEquals("My Preset", presets[0].label)
        assertEquals(432, presets[0].referencePitch)
        assertEquals(100, presets[0].maxCents)
    }

    // ═══════════════════════════════════════════════════════════════════
    // Delete
    // ═══════════════════════════════════════════════════════════════════

    @Test
    fun `deleteConfig removes preset by label`() = runTest {
        manager.saveConfig(TuningConfiguration(label = "A", referencePitch = 440, maxCents = 50))
        manager.saveConfig(TuningConfiguration(label = "B", referencePitch = 415, maxCents = 25))

        manager.deleteConfig("A")

        val presets = manager.presets.first()
        assertEquals("Should have 1 preset after delete", 1, presets.size)
        assertEquals("B", presets[0].label)
    }

    @Test
    fun `deleteConfig handles non-existent label gracefully`() = runTest {
        manager.saveConfig(TuningConfiguration(label = "Only", referencePitch = 440, maxCents = 50))
        manager.deleteConfig("NonExistent")

        val presets = manager.presets.first()
        assertEquals("Should still have 1 preset", 1, presets.size)
    }

    @Test
    fun `deleteConfig on empty list does nothing`() = runTest {
        manager.deleteConfig("Anything")
        val presets = manager.presets.first()
        assertTrue("Should remain empty", presets.isEmpty())
    }

    // ═══════════════════════════════════════════════════════════════════
    // No user — safe defaults
    // ═══════════════════════════════════════════════════════════════════

    @Test
    fun `presets returns empty list when no user is logged in`() = runTest {
        val noUserAuthManager = AuthManager(context)
        // Don't save a user — currentUser stays null
        val noUserManager = TuningPreferencesManager(context, noUserAuthManager)

        val presets = noUserManager.presets.first()
        assertTrue("Should return empty list when no user", presets.isEmpty())
    }

    @Test
    fun `saveConfig is no-op when no user is logged in`() = runTest {
        val noUserAuthManager = AuthManager(context)
        val noUserManager = TuningPreferencesManager(context, noUserAuthManager)

        noUserManager.saveConfig(
            TuningConfiguration(label = "ShouldNotPersist", referencePitch = 440, maxCents = 50)
        )

        val presets = noUserManager.presets.first()
        assertTrue("Should still be empty when no user", presets.isEmpty())
    }

    // ═══════════════════════════════════════════════════════════════════
    // loadConfig — explicit single load
    // ═══════════════════════════════════════════════════════════════════

    @Test
    fun `loadConfig returns preset by label`() = runTest {
        manager.saveConfig(TuningConfiguration(label = "Find Me", referencePitch = 432, maxCents = 75))

        val loaded = manager.loadConfig("Find Me")
        assertNotNull("Should find the preset", loaded)
        assertEquals(432, loaded?.referencePitch)
        assertEquals(75, loaded?.maxCents)
    }

    @Test
    fun `loadConfig returns null for non-existent label`() = runTest {
        val loaded = manager.loadConfig("DoesNotExist")
        assertNull("Should return null for non-existent preset", loaded)
    }

    @Test
    fun `loadConfig returns null when list is empty`() = runTest {
        val loaded = manager.loadConfig("Any")
        assertNull("Should return null when no presets exist", loaded)
    }
}
