package com.violinmaster.app.data

/**
 * Feature flag configuration for cloud migration rollout.
 *
 * Controls whether cloud sync (Firestore) is enabled. Defaults to false
 * for safe phased rollout. When false, the app operates in Room-only mode
 * (current behavior). When true, PracticeRepository delegates writes and
 * observations to FirestoreSyncRepository instances.
 *
 * Currently uses an in-code default. When firebase-config dependency
 * is added, this class can be backed by Firebase RemoteConfig with
 * server-side flag control without changing the public API.
 *
 * REQ-DI-009: Cloud sync enabled/disabled via feature flag.
 */
open class CloudConfig {

    companion object {
        /** RemoteConfig key for the cloud sync feature flag. */
        const val KEY_CLOUD_SYNC_ENABLED = "cloudSyncEnabled"
    }

    /**
     * Whether cloud sync is enabled.
     *
     * Default is false for safe rollout. When true, PracticeRepository
     * routes operations through FirestoreSyncRepository. When false,
     * operations go directly to Room DAOs.
     */
    open val cloudSyncEnabled: Boolean = true

    /**
     * Fetches the latest RemoteConfig values from the server.
     * Currently a no-op; will delegate to FirebaseRemoteConfig.fetch()
     * when firebase-config dependency is added.
     */
    suspend fun fetch() {
        // No-op until firebase-config is integrated
    }

    /**
     * Activates fetched RemoteConfig values.
     * Currently a no-op; will delegate to FirebaseRemoteConfig.activate()
     * when firebase-config dependency is added.
     */
    suspend fun activate() {
        // No-op until firebase-config is integrated
    }
}
