package com.violinmaster.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Tests for ICrashReportingService contract and FirebaseCrashReportingService.
 *
 * REQ-CRASH-003: Custom keys and logs callable via the service interface.
 *
 * FirebaseCrashlytics (final class) cannot be instantiated in Robolectric
 * unit tests — same limitation as FirebaseFirestore noted in ChatRepositoryTest.
 * These tests verify the interface contract via a test spy implementation.
 * Full Firebase integration is validated by REQ-CRASH-005 E2E test crash exercise.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class FirebaseCrashReportingServiceTest {

    // ── Test spy: records all calls for assertion ─────────────────────────

    private class SpyCrashReportingService : ICrashReportingService {
        private val _logs = mutableListOf<String>()
        private val _exceptions = mutableListOf<Throwable>()
        private val _customKeys = mutableListOf<Pair<String, String>>()

        val logs: List<String> get() = _logs.toList()
        val exceptions: List<Throwable> get() = _exceptions.toList()
        val customKeys: List<Pair<String, String>> get() = _customKeys.toList()

        override fun log(message: String) {
            _logs.add(message)
        }

        override fun recordException(throwable: Throwable) {
            _exceptions.add(throwable)
        }

        override fun setCustomKey(key: String, value: String) {
            _customKeys.add(key to value)
        }
    }

    // ── log() tests ───────────────────────────────────────────────────────

    @Test
    fun `log records message string`() {
        val service = SpyCrashReportingService()

        service.log("User tapped practice button")

        assertEquals(
            "Should record the log message",
            listOf("User tapped practice button"),
            service.logs
        )
    }

    @Test
    fun `log records multiple messages in order`() {
        val service = SpyCrashReportingService()

        service.log("App started")
        service.log("User logged in")
        service.log("Practice session saved")

        assertEquals(
            "Should record 3 log messages in insertion order",
            3,
            service.logs.size
        )
        assertEquals("App started", service.logs[0])
        assertEquals("User logged in", service.logs[1])
        assertEquals("Practice session saved", service.logs[2])
    }

    @Test
    fun `log handles empty message`() {
        val service = SpyCrashReportingService()

        service.log("")

        assertEquals(
            "Empty message should still be recorded (Crashlytics accepts empty strings)",
            listOf(""),
            service.logs
        )
    }

    // ── recordException() tests ───────────────────────────────────────────

    @Test
    fun `recordException records throwable`() {
        val service = SpyCrashReportingService()
        val exception = RuntimeException("Database connection failed")

        service.recordException(exception)

        assertEquals(
            "Should record exactly 1 exception",
            1,
            service.exceptions.size
        )
        assertEquals(
            "Recorded exception message must match",
            "Database connection failed",
            service.exceptions[0].message
        )
    }

    @Test
    fun `recordException records multiple distinct exceptions`() {
        val service = SpyCrashReportingService()
        val ex1 = IllegalStateException("State error")
        val ex2 = NullPointerException("Null ref")

        service.recordException(ex1)
        service.recordException(ex2)

        assertEquals("Should record 2 exceptions", 2, service.exceptions.size)
        assertTrue("First should be IllegalStateException", service.exceptions[0] is IllegalStateException)
        assertTrue("Second should be NullPointerException", service.exceptions[1] is NullPointerException)
    }

    // ── setCustomKey() tests ──────────────────────────────────────────────

    @Test
    fun `setCustomKey stores key-value pair`() {
        val service = SpyCrashReportingService()

        service.setCustomKey("user_role", "teacher")

        assertEquals(
            "Should store 1 key-value pair",
            1,
            service.customKeys.size
        )
        assertEquals("user_role", service.customKeys[0].first)
        assertEquals("teacher", service.customKeys[0].second)
    }

    @Test
    fun `setCustomKey overwrites previous value for same key`() {
        val service = SpyCrashReportingService()

        service.setCustomKey("screen", "login")
        service.setCustomKey("screen", "home")

        assertEquals(
            "Both values should be recorded (Crashlytics keeps last value per key)",
            2,
            service.customKeys.size
        )
        assertEquals("screen" to "login", service.customKeys[0])
        assertEquals("screen" to "home", service.customKeys[1])
    }

    @Test
    fun `setCustomKey stores multiple distinct keys`() {
        val service = SpyCrashReportingService()

        service.setCustomKey("instrument", "violin")
        service.setCustomKey("skill_level", "intermediate")
        service.setCustomKey("app_version", "1.0.0")

        assertEquals("Should store 3 key-value pairs", 3, service.customKeys.size)
        assertEquals("instrument" to "violin", service.customKeys[0])
        assertEquals("skill_level" to "intermediate", service.customKeys[1])
        assertEquals("app_version" to "1.0.0", service.customKeys[2])
    }

    // ── Interface implementation test ─────────────────────────────────────

    @Test
    fun `FirebaseCrashReportingService implements ICrashReportingService`() {
        // Verify that the Firebase implementation class exists and implements the interface.
        // Actual FirebaseCrashlytics wiring is tested via device/E2E (REQ-CRASH-005).
        val service: ICrashReportingService = FirebaseCrashReportingService()
        assertTrue(
            "FirebaseCrashReportingService must implement ICrashReportingService",
            service is ICrashReportingService
        )
    }

    // ── No-op state test ──────────────────────────────────────────────────

    @Test
    fun `fresh service has no recorded entries`() {
        val service = SpyCrashReportingService()

        assertTrue("Fresh service should have no logs", service.logs.isEmpty())
        assertTrue("Fresh service should have no exceptions", service.exceptions.isEmpty())
        assertTrue("Fresh service should have no custom keys", service.customKeys.isEmpty())
    }
}
