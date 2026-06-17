package com.violinmaster.app

import android.app.Application
import com.google.firebase.FirebaseApp
import com.google.firebase.crashlytics.FirebaseCrashlytics
import dagger.hilt.android.HiltAndroidApp

/**
 * Application entry point.
 *
 * Initialization order matters:
 * 1. Hilt DI (via @HiltAndroidApp annotation)
 * 2. FirebaseApp (via google-services.json ContentProvider — automatic)
 * 3. Crashlytics (depends on FirebaseApp)
 * 4. Analytics (depends on FirebaseApp)
 *
 * REQ-CRASH-001: Crashlytics initialized at app startup.
 * REQ-ANALYTICS-001: Analytics initialized after FirebaseApp.
 */
@HiltAndroidApp
class ViolinMasterApp : Application() {

    override fun onCreate() {
        super.onCreate()

        // FirebaseApp is auto-initialized by the ContentProvider from google-services.json.
        // Crashlytics and Analytics depend on FirebaseApp being ready.
        FirebaseApp.initializeApp(this)

        // Enable Crashlytics collection for all builds.
        // Debug builds can be filtered in the Firebase Console by build type.
        FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(true)
    }
}
