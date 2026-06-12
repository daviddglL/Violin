package com.violinmaster.app.data

/**
 * Test helper providing CloudConfig with cloud sync enabled for integration testing.
 *
 * Used by CloudSyncIntegrationTest and PracticeRepositoryFacadeTest to test
 * the cloud-enabled code path through PracticeRepository.
 */
open class CloudConfigWithSync : CloudConfig() {
    override val cloudSyncEnabled: Boolean = true
}
