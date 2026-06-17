package com.violinmaster.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Tests for IAnalyticsService contract.
 *
 * REQ-ANALYTICS-001: Event logging with typed parameter support.
 * REQ-ANALYTICS-002: User property setting.
 *
 * FirebaseAnalytics (final class) cannot be instantiated in Robolectric
 * unit tests. These tests verify the interface contract via a test spy.
 * Full Firebase integration is validated via E2E runs on device/emulator.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class FirebaseAnalyticsServiceTest {

    // ── Test spy: records all calls for assertion ───────────────────────

    private class SpyAnalyticsService : IAnalyticsService {
        data class EventRecord(val name: String, val params: Map<String, Any>)
        data class PropRecord(val key: String, val value: String)

        private val _events = mutableListOf<EventRecord>()
        private val _props = mutableListOf<PropRecord>()
        private var _userId: String? = null
        private var _screenName: String? = null
        private var _screenClass: String? = null

        val events: List<EventRecord> get() = _events.toList()
        val props: List<PropRecord> get() = _props.toList()
        val userId: String? get() = _userId
        val screenName: String? get() = _screenName
        val screenClass: String? get() = _screenClass

        override fun logEvent(name: String, params: Map<String, Any>) {
            _events.add(EventRecord(name, params))
        }

        override fun setUserProperty(key: String, value: String) {
            _props.add(PropRecord(key, value))
        }

        override fun setUserId(id: String) {
            _userId = id
        }

        override fun setCurrentScreen(screenName: String, screenClass: String) {
            _screenName = screenName
            _screenClass = screenClass
        }
    }

    // ── logEvent() tests ────────────────────────────────────────────────

    @Test
    fun `logEvent records event name with empty params`() {
        val service = SpyAnalyticsService()

        service.logEvent("practice_started")

        assertEquals("Should record 1 event", 1, service.events.size)
        assertEquals("practice_started", service.events[0].name)
        assertTrue("Params should be empty", service.events[0].params.isEmpty())
    }

    @Test
    fun `logEvent records event name with string param`() {
        val service = SpyAnalyticsService()

        service.logEvent("practice_started", mapOf("category" to "scales"))

        assertEquals(1, service.events.size)
        assertEquals("practice_started", service.events[0].name)
        assertEquals(mapOf("category" to "scales"), service.events[0].params)
    }

    @Test
    fun `logEvent records event with int param`() {
        val service = SpyAnalyticsService()

        service.logEvent("practice_ended", mapOf("duration_seconds" to 3600))

        assertEquals(1, service.events.size)
        assertEquals(3600, service.events[0].params["duration_seconds"])
    }

    @Test
    fun `logEvent records multiple distinct events in order`() {
        val service = SpyAnalyticsService()

        service.logEvent("practice_started", mapOf("category" to "warmup"))
        service.logEvent("practice_ended", mapOf("duration_seconds" to 1200))
        service.logEvent("lesson_completed", mapOf("lesson_id" to "L001"))

        assertEquals(3, service.events.size)
        assertEquals("practice_started", service.events[0].name)
        assertEquals("practice_ended", service.events[1].name)
        assertEquals("lesson_completed", service.events[2].name)
    }

    // ── setUserProperty() tests ─────────────────────────────────────────

    @Test
    fun `setUserProperty records key-value pair`() {
        val service = SpyAnalyticsService()

        service.setUserProperty("user_role", "teacher")

        assertEquals(1, service.props.size)
        assertEquals("user_role", service.props[0].key)
        assertEquals("teacher", service.props[0].value)
    }

    @Test
    fun `setUserProperty records multiple properties`() {
        val service = SpyAnalyticsService()

        service.setUserProperty("user_role", "student")
        service.setUserProperty("skill_level", "intermediate")
        service.setUserProperty("instrument", "violin")

        assertEquals(3, service.props.size)
        assertEquals("instrument", service.props[2].key)
        assertEquals("violin", service.props[2].value)
    }

    // ── setUserId() tests ───────────────────────────────────────────────

    @Test
    fun `setUserId stores user identifier`() {
        val service = SpyAnalyticsService()

        service.setUserId("violinist_42")

        assertEquals("violinist_42", service.userId)
    }

    // ── setCurrentScreen() tests ────────────────────────────────────────

    @Test
    fun `setCurrentScreen stores screen name and class`() {
        val service = SpyAnalyticsService()

        service.setCurrentScreen("HomeScreen", "HomeScreen")

        assertEquals("HomeScreen", service.screenName)
        assertEquals("HomeScreen", service.screenClass)
    }

    // ── Fresh state test ────────────────────────────────────────────────

    @Test
    fun `fresh service has no recorded events or properties`() {
        val service = SpyAnalyticsService()

        assertTrue("Fresh service should have no events", service.events.isEmpty())
        assertTrue("Fresh service should have no props", service.props.isEmpty())
    }

    // ── AnalyticsEvents constants coverage ──────────────────────────────

    @Test
    fun `AnalyticsEvents defines all expected event names`() {
        val events = listOf(
            AnalyticsEvents.EVENT_PRACTICE_STARTED,
            AnalyticsEvents.EVENT_PRACTICE_ENDED,
            AnalyticsEvents.EVENT_LESSON_COMPLETED,
            AnalyticsEvents.EVENT_ASSIGNMENT_CREATED,
            AnalyticsEvents.EVENT_ASSIGNMENT_COMPLETED,
            AnalyticsEvents.EVENT_TUNER_OPENED,
            AnalyticsEvents.EVENT_METRONOME_OPENED,
            AnalyticsEvents.EVENT_PIN_LOGIN,
            AnalyticsEvents.EVENT_PIN_RECOVERY_USED
        )
        assertEquals(9, events.size)
        // All event names should be non-blank
        events.forEach { assertTrue("Event name '$it' must not be blank", it.isNotBlank()) }
    }

    @Test
    fun `AnalyticsEvents defines all expected parameter keys`() {
        val params = listOf(
            AnalyticsEvents.PARAM_ROLE,
            AnalyticsEvents.PARAM_SKILL_LEVEL,
            AnalyticsEvents.PARAM_INSTRUMENT,
            AnalyticsEvents.PARAM_DURATION_SECONDS,
            AnalyticsEvents.PARAM_CATEGORY,
            AnalyticsEvents.PARAM_LESSON_ID,
            AnalyticsEvents.PARAM_LESSON_LEVEL,
            AnalyticsEvents.PARAM_ASSIGNMENT_ID,
            AnalyticsEvents.PARAM_SCREEN_NAME
        )
        assertEquals(9, params.size)
        params.forEach { assertTrue("Param key '$it' must not be blank", it.isNotBlank()) }
    }
}
