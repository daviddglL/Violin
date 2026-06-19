package com.violinmaster.app.data

import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import com.violinmaster.app.R
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Firebase Remote Config-backed feature flag configuration.
 *
 * Provides server-controlled feature flags with local defaults.
 * Flags can be updated in the Firebase Console without an app release.
 *
 * Default values are loaded from res/xml/remote_config_defaults.xml.
 *
 * REQ-DI-009: Cloud sync enabled/disabled via feature flag.
 * REQ-RC-001: Remote Config initialized with developer mode in debug.
 * REQ-RC-002: Minimum fetch interval: 1 hour in prod, 0 in debug.
 */
@Singleton
open class CloudConfig @Inject constructor(
    remoteConfig: FirebaseRemoteConfig? = null
) {
    private val remoteConfig: FirebaseRemoteConfig by lazy {
        remoteConfig ?: FirebaseRemoteConfig.getInstance()
    }

    /**
     * Whether cloud sync is enabled.
     *
     * Backed by RemoteConfig key "cloudSyncEnabled". Defaults to true
     * (cloud sync ON) in remote_config_defaults.xml. Can be toggled
     * server-side for phased rollout or emergency disable.
     *
     * Falls back to true (cloud sync ON) when Firebase is not initialized
     * (e.g., in unit tests without Robolectric Firebase setup).
     */
    open val cloudSyncEnabled: Boolean
        get() = runCatching { remoteConfig.getBoolean(KEY_CLOUD_SYNC_ENABLED) }
            .getOrDefault(true)

    /**
     * Initializes Remote Config with settings appropriate for the build type.
     *
     * Must be called once during app startup. In debug builds, the cache
     * expiration is set to 0 for instant developer feedback.
     *
     * @param isDebug Whether the app is running in debug mode.
     */
    fun initialize(isDebug: Boolean) {
        val configSettings = FirebaseRemoteConfigSettings.Builder()
            .setMinimumFetchIntervalInSeconds(
                if (isDebug) 0 else 3600 // 0 in debug, 1 hour in production
            )
            .build()

        remoteConfig.setConfigSettingsAsync(configSettings)
        remoteConfig.setDefaultsAsync(R.xml.remote_config_defaults)
    }

    /**
     * Fetches and activates the latest Remote Config values from the server.
     *
     * Safe to call multiple times — respects the minimum fetch interval.
     * On failure, cached values (or defaults) are used.
     *
     * @return Result containing success/failure for logging/monitoring.
     */
    suspend fun fetchAndActivate(): Result<Boolean> {
        return try {
            remoteConfig.fetch().await()
            val activated = remoteConfig.activate().await()
            Result.success(activated)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    companion object {
        /** RemoteConfig key for the cloud sync feature flag. */
        const val KEY_CLOUD_SYNC_ENABLED = "cloudSyncEnabled"
    }
}
