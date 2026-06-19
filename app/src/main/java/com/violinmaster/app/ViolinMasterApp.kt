package com.violinmaster.app

import android.app.Application
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.violinmaster.app.push.NotificationHelper
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/**
 * Application entry point.
 *
 * Initialization order matters:
 * 1. Hilt DI (via @HiltAndroidApp annotation)
 * 2. FirebaseApp (via google-services.json ContentProvider — automatic)
 * 3. App Check (must be installed BEFORE any Firebase service is used)
 * 4. Crashlytics (depends on FirebaseApp)
 * 5. Analytics (depends on FirebaseApp)
 * 6. Notification channels (Android O+)
 *
 * REQ-CRASH-001: Crashlytics initialized at app startup.
 * REQ-ANALYTICS-001: Analytics initialized after FirebaseApp.
 * REQ-FCM-002: Notification channels created at app startup.
 * REQ-APPCHECK-001: App Check installed before Firebase services are accessed.
 */
@HiltAndroidApp
class ViolinMasterApp : Application() {

    @Inject lateinit var notificationHelper: NotificationHelper

    override fun onCreate() {
        super.onCreate()

        // FirebaseApp is auto-initialized by the ContentProvider from google-services.json.
        // Crashlytics and Analytics depend on FirebaseApp being ready.
        FirebaseApp.initializeApp(this)

        // ── Firebase App Check ──────────────────────────────────────────
        // App Check must be installed BEFORE any Firebase backend call.
        // Debug/emulator builds use a debug provider (secret from Firebase Console).
        // Production builds use Play Integrity attestation.
        //
        // REQ-APPCHECK-001: Attestation provider installed at app startup.
        // REQ-APPCHECK-002: Debug provider for staging/emulator; Play Integrity for prod.
        if (BuildConfig.DEBUG || BuildConfig.IS_STAGING) {
            FirebaseAppCheck.getInstance().installAppCheckProviderFactory(
                DebugAppCheckProviderFactory.getInstance()
            )
        } else {
            FirebaseAppCheck.getInstance().installAppCheckProviderFactory(
                PlayIntegrityAppCheckProviderFactory.getInstance()
            )
        }

        // Enable Crashlytics collection for all builds.
        // Debug builds can be filtered in the Firebase Console by build type.
        FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(true)

        // Create notification channels for Android O+
        notificationHelper.createNotificationChannels()
    }
}
