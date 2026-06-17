package com.violinmaster.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for AnalyticsHelper — the unified facade wrapping
 * IAnalyticsService and ICrashReportingService.
 *
 * REQ-CRASH-003: Crash logs reachable via AnalyticsHelper.
 * REQ-ANALYTICS-001: Analytics events routed correctly.
 */
class AnalyticsHelperTest {

    // ── Test doubles ────────────────────────────────────────────────────

    private class SpyAnalyticsService : IAnalyticsService {
        data class EventRecord(val name: String, val params: Map<String, Any>)
        private val _events = mutableListOf<EventRecord>()
        private var _userId: String? = null
        private val _props = mutableListOf<Pair<String, String>>()

        val events: List<EventRecord> get() = _events.toList()
        val userId: String? get() = _userId
        val props: List<Pair<String, String>> get() = _props.toList()

        override fun logEvent(name: String, params: Map<String, Any>) {
            _events.add(EventRecord(name, params))
        }
        override fun setUserProperty(key: String, value: String) {
            _props.add(key to value)
        }
        override fun setUserId(id: String) { _userId = id }
        override fun setCurrentScreen(screenName: String, screenClass: String) {}
    }

    private class SpyCrashReportingService : ICrashReportingService {
        private val _logs = mutableListOf<String>()
        private val _exceptions = mutableListOf<Throwable>()
        private val _keys = mutableListOf<Pair<String, String>>()

        val logs: List<String> get() = _logs.toList()
        val exceptions: List<Throwable> get() = _exceptions.toList()
        val keys: List<Pair<String, String>> get() = _keys.toList()

        override fun log(message: String) { _logs.add(message) }
        override fun recordException(throwable: Throwable) { _exceptions.add(throwable) }
        override fun setCustomKey(key: String, value: String) { _keys.add(key to value) }
    }

    // ── Analytics delegation ────────────────────────────────────────────

    @Test
    fun `logEvent delegates to analytics service`() {
        val analytics = SpyAnalyticsService()
        val crash = SpyCrashReportingService()
        val helper = AnalyticsHelper(analytics, crash)

        helper.logEvent("practice_started", mapOf("category" to "scales"))

        assertEquals(1, analytics.events.size)
        assertEquals("practice_started", analytics.events[0].name)
        assertEquals(mapOf("category" to "scales"), analytics.events[0].params)
    }

    @Test
    fun `setUserProperty delegates to analytics service`() {
        val analytics = SpyAnalyticsService()
        val crash = SpyCrashReportingService()
        val helper = AnalyticsHelper(analytics, crash)

        helper.setUserProperty("user_role", "teacher")

        assertEquals(1, analytics.props.size)
        assertEquals("user_role", analytics.props[0].first)
        assertEquals("teacher", analytics.props[0].second)
    }

    @Test
    fun `setUserId delegates to analytics service`() {
        val analytics = SpyAnalyticsService()
        val crash = SpyCrashReportingService()
        val helper = AnalyticsHelper(analytics, crash)

        helper.setUserId("violinist_42")

        assertEquals("violinist_42", analytics.userId)
    }

    // ── Crash delegation ────────────────────────────────────────────────

    @Test
    fun `log delegates to crash reporting service`() {
        val analytics = SpyAnalyticsService()
        val crash = SpyCrashReportingService()
        val helper = AnalyticsHelper(analytics, crash)

        helper.log("User navigated to tuner")

        assertEquals(1, crash.logs.size)
        assertEquals("User navigated to tuner", crash.logs[0])
    }

    @Test
    fun `recordException delegates to crash reporting service`() {
        val analytics = SpyAnalyticsService()
        val crash = SpyCrashReportingService()
        val helper = AnalyticsHelper(analytics, crash)
        val exception = RuntimeException("Test error")

        helper.recordException(exception)

        assertEquals(1, crash.exceptions.size)
        assertEquals("Test error", crash.exceptions[0].message)
    }

    @Test
    fun `setCustomKey delegates to crash reporting service`() {
        val analytics = SpyAnalyticsService()
        val crash = SpyCrashReportingService()
        val helper = AnalyticsHelper(analytics, crash)

        helper.setCustomKey("screen", "settings")

        assertEquals(1, crash.keys.size)
        assertEquals("screen" to "settings", crash.keys[0])
    }

    // ── Independence test ───────────────────────────────────────────────

    @Test
    fun `analytics events do not affect crash logs`() {
        val analytics = SpyAnalyticsService()
        val crash = SpyCrashReportingService()
        val helper = AnalyticsHelper(analytics, crash)

        helper.logEvent("tuner_opened", mapOf("instrument" to "violin"))
        helper.log("User opened tuner")

        assertEquals(1, analytics.events.size)
        assertEquals(1, crash.logs.size)
        assertEquals("tuner_opened", analytics.events[0].name)
        assertEquals("User opened tuner", crash.logs[0])
    }

    // ── Constructor test ────────────────────────────────────────────────

    @Test
    fun `AnalyticsHelper can be constructed with test doubles`() {
        val analytics = SpyAnalyticsService()
        val crash = SpyCrashReportingService()
        val helper = AnalyticsHelper(analytics, crash)

        assertNotNull("AnalyticsHelper should be constructed successfully", helper)
    }
}
