package com.violinmaster.app.data

import com.google.firebase.crashlytics.FirebaseCrashlytics
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Firebase Crashlytics implementation of [ICrashReportingService].
 *
 * Delegates to the Firebase Crashlytics singleton for crash reporting,
 * custom key logging, and non-fatal exception recording.
 *
 * REQ-CRASH-001: Initialized via DI singleton.
 * REQ-CRASH-002: Auto-capture of unhandled exceptions by Firebase SDK.
 * REQ-CRASH-003: Custom keys and logs exposed through the interface.
 */
@Singleton
class FirebaseCrashReportingService @Inject constructor() : ICrashReportingService {

    private val crashlytics: FirebaseCrashlytics
        get() = FirebaseCrashlytics.getInstance()

    override fun log(message: String) {
        crashlytics.log(message)
    }

    override fun recordException(throwable: Throwable) {
        crashlytics.recordException(throwable)
    }

    override fun setCustomKey(key: String, value: String) {
        crashlytics.setCustomKey(key, value)
    }
}
