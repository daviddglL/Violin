package com.violinmaster.app.data

import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Firebase Analytics implementation of [IAnalyticsService].
 *
 * Delegates to the Firebase Analytics singleton for event logging,
 * user property setting, and screen tracking.
 *
 * REQ-ANALYTICS-001: Event logging wired to Firebase Analytics.
 * REQ-ANALYTICS-002: User properties persisted via Firebase.
 * REQ-ANALYTICS-003: Screen tracking via setCurrentScreen.
 */
@Singleton
class FirebaseAnalyticsService @Inject constructor(
    private val analytics: FirebaseAnalytics
) : IAnalyticsService {

    override fun logEvent(name: String, params: Map<String, Any>) {
        val bundle = Bundle().apply {
            params.forEach { (key, value) ->
                when (value) {
                    is String -> putString(key, value)
                    is Int -> putInt(key, value)
                    is Long -> putLong(key, value)
                    is Double -> putDouble(key, value)
                    is Boolean -> putBoolean(key, value)
                }
            }
        }
        analytics.logEvent(name, bundle)
    }

    override fun setUserProperty(key: String, value: String) {
        analytics.setUserProperty(key, value)
    }

    override fun setUserId(id: String) {
        analytics.setUserId(id)
    }

    override fun setCurrentScreen(screenName: String, screenClass: String) {
        // Uses screen_view event instead of deprecated setCurrentScreen(Activity, ...)
        // to avoid requiring an Activity reference in a service-layer class.
        val bundle = Bundle().apply {
            putString(FirebaseAnalytics.Param.SCREEN_NAME, screenName)
            putString(FirebaseAnalytics.Param.SCREEN_CLASS, screenClass)
        }
        analytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, bundle)
    }
}
