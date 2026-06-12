package com.violinmaster.app.data

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Test

/**
 * Validates CloudConfig feature flag behavior for safe cloud rollout.
 *
 * CloudConfig wraps the cloudSyncEnabled feature flag. Default is false
 * for safe rollout. App operates Room-only until flag is enabled.
 *
 * Currently uses a simple in-code default (no Firebase RemoteConfig dependency).
 * When firebase-config is added to the project, CloudConfig can be backed by
 * RemoteConfig without changing the public API.
 *
 * REQ-DI-009: Feature flag controls whether cloud sync is enabled.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class CloudConfigTest {

    // ── Default Value ──────────────────────────────────────────────────────

    @Test
    fun `cloudSyncEnabled defaults to false`() {
        val cloudConfig = CloudConfig()

        val enabled = cloudConfig.cloudSyncEnabled

        assertFalse("cloudSyncEnabled should default to false for safe rollout", enabled)
    }

    @Test
    fun `CloudConfig is instantiable without dependencies`() {
        val cloudConfig = CloudConfig()

        assertNotNull("CloudConfig should be instantiable", cloudConfig)
    }

    @Test
    fun `cloudSyncEnabled returns consistent value`() {
        val cloudConfig = CloudConfig()

        val first = cloudConfig.cloudSyncEnabled
        val second = cloudConfig.cloudSyncEnabled

        assertEquals("cloudSyncEnabled should be consistent", first, second)
    }

    // ── Feature Flag Key Constant ──────────────────────────────────────────

    @Test
    fun `KEY_CLOUD_SYNC_ENABLED contains expected flag name`() {
        assertEquals("cloudSyncEnabled", CloudConfig.KEY_CLOUD_SYNC_ENABLED)
    }

    // ── fetch and activate are safe no-ops ─────────────────────────────────

    @Test
    fun `fetch does not throw`() = runTest {
        val cloudConfig = CloudConfig()

        // Should not throw even without RemoteConfig
        cloudConfig.fetch()
    }

    @Test
    fun `activate does not throw`() = runTest {
        val cloudConfig = CloudConfig()

        // Should not throw even without RemoteConfig
        cloudConfig.activate()
    }
}
