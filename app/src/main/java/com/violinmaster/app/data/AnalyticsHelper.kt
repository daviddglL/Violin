package com.violinmaster.app.data

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Unified analytics and crash-reporting facade for the presentation layer.
 *
 * Wraps both [IAnalyticsService] and [ICrashReportingService] behind a
 * single injectable dependency. ViewModels inject this helper instead of
 * the individual services, keeping constructor parameter lists compact.
 *
 * REQ-CRASH-003: Custom keys and logs reachable from any ViewModel.
 * REQ-ANALYTICS-004: Standardized event logging via AnalyticsEvents constants.
 */
@Singleton
class AnalyticsHelper @Inject constructor(
    private val analyticsService: IAnalyticsService,
    private val crashReportingService: ICrashReportingService
) {
    // ── Analytics ───────────────────────────────────────────────────────

    fun logEvent(name: String, params: Map<String, Any> = emptyMap()) {
        analyticsService.logEvent(name, params)
    }

    fun setUserProperty(key: String, value: String) {
        analyticsService.setUserProperty(key, value)
    }

    fun setUserId(id: String) {
        analyticsService.setUserId(id)
    }

    fun setCurrentScreen(screenName: String, screenClass: String) {
        analyticsService.setCurrentScreen(screenName, screenClass)
    }

    // ── Crash Reporting ─────────────────────────────────────────────────

    fun log(message: String) {
        crashReportingService.log(message)
    }

    fun recordException(throwable: Throwable) {
        crashReportingService.recordException(throwable)
    }

    fun setCustomKey(key: String, value: String) {
        crashReportingService.setCustomKey(key, value)
    }
}
