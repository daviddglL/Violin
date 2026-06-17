package com.violinmaster.app.data

/**
 * Interface for crash reporting, enabling test doubles and abstracting
 * the Firebase Crashlytics dependency from the rest of the app.
 *
 * REQ-CRASH-002: Automatic capture of unhandled exceptions.
 * REQ-CRASH-003: Custom keys and logs callable from any component.
 */
interface ICrashReportingService {
    fun log(message: String)
    fun recordException(throwable: Throwable)
    fun setCustomKey(key: String, value: String)
}
