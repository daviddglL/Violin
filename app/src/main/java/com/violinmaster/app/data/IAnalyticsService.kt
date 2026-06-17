package com.violinmaster.app.data

/**
 * Interface for analytics event logging, enabling test doubles and abstracting
 * Firebase Analytics from the rest of the app.
 *
 * REQ-ANALYTICS-001: Event logging with parameters.
 * REQ-ANALYTICS-002: User property setting.
 * REQ-ANALYTICS-003: Screen tracking.
 */
interface IAnalyticsService {
    fun logEvent(name: String, params: Map<String, Any> = emptyMap())
    fun setUserProperty(key: String, value: String)
    fun setUserId(id: String)
    fun setCurrentScreen(screenName: String, screenClass: String)
}
