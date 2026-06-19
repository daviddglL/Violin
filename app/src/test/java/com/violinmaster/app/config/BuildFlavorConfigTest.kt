package com.violinmaster.app.config

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Validates product flavor configuration in app/build.gradle.kts against
 * REQ-STAGING-001 through REQ-STAGING-006.
 *
 * These are structural/config validation tests. The "production code" under
 * test is the Gradle build file itself.
 */
class BuildFlavorConfigTest {

    private val buildFile: File by lazy {
        val projectRoot = findProjectRoot()
        File(projectRoot, "app/build.gradle.kts")
    }

    private val content: String by lazy {
        require(buildFile.exists()) { "build.gradle.kts not found at ${buildFile.absolutePath}" }
        buildFile.readText()
    }

    // ── REQ-STAGING-001: Product flavors dev and prod MUST be defined ─────────

    @Test
    fun `REQ-STAGING-001 flavorDimensions environment is defined`() {
        assertTrue(
            "build.gradle.kts must define flavorDimensions with 'environment'",
            content.contains("flavorDimensions") && content.contains("environment")
        )
    }

    @Test
    fun `REQ-STAGING-001 productFlavors dev is defined`() {
        assertTrue(
            "build.gradle.kts must define productFlavors with 'dev'",
            content.contains("productFlavors") && content.contains("dev")
        )
    }

    @Test
    fun `REQ-STAGING-001 productFlavors prod is defined`() {
        assertTrue(
            "build.gradle.kts must define productFlavors with 'prod'",
            content.contains("productFlavors") && content.contains("prod")
        )
    }

    // ── REQ-STAGING-002: Dev flavor uses applicationIdSuffix ".dev" ───────────

    @Test
    fun `REQ-STAGING-002 dev flavor has applicationIdSuffix dev`() {
        assertTrue(
            "dev flavor must have applicationIdSuffix = \".dev\"",
            content.contains("applicationIdSuffix") && content.contains(".dev")
        )
    }

    // ── REQ-STAGING-003: Per-flavor google-services.json ──────────────────────

    @Test
    fun `REQ-STAGING-003 google-services plugin resolves per-flavor`() {
        // The Firebase Gradle plugin auto-resolves google-services.json from
        // app/src/{flavor}/. The build config just needs flavor source sets.
        // This is tested implicitly by the existence of flavor directories.
        val projectRoot = findProjectRoot()
        val devJson = File(projectRoot, "app/src/dev/google-services.json")
        val prodJson = File(projectRoot, "app/src/prod/google-services.json")
        assertTrue(
            "app/src/dev/google-services.json must exist for dev flavor",
            devJson.exists()
        )
        assertTrue(
            "app/src/prod/google-services.json must exist for prod flavor",
            prodJson.exists()
        )
    }

    // ── REQ-STAGING-005: Secrets from .env.{flavor} ───────────────────────────

    @Test
    fun `REQ-STAGING-005 secrets block uses env files`() {
        assertTrue(
            "build.gradle.kts must configure secrets plugin with .env files",
            content.contains("secrets") && content.contains(".env")
        )
    }

    // ── REQ-STAGING-006: Dev flavor debug logging ────────────────────────────

    @Test
    fun `REQ-STAGING-006 dev flavor has DEBUG buildConfigField`() {
        assertTrue(
            "dev flavor must have buildConfigField with DEBUG = true",
            content.contains("buildConfigField") && content.contains("DEBUG")
        )
    }

    @Test
    fun `REQ-STAGING-006 dev flavor has analytics debug enabled`() {
        assertTrue(
            "dev flavor must have ANALYTICS_DEBUG_ENABLED buildConfigField",
            content.contains("ANALYTICS_DEBUG_ENABLED")
        )
    }

    @Test
    fun `REQ-STAGING-006 dev flavor has IS_STAGING buildConfigField`() {
        assertTrue(
            "dev flavor must have IS_STAGING buildConfigField",
            content.contains("IS_STAGING")
        )
    }

    // ── REQ-STAGING-004: Firebase project separation ──────────────────────────

    @Test
    fun `REQ-STAGING-004 each flavor has FIREBASE_PROJECT_ID buildConfigField`() {
        assertTrue(
            "each flavor must define FIREBASE_PROJECT_ID buildConfigField",
            content.contains("FIREBASE_PROJECT_ID")
        )
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private fun findProjectRoot(): File {
        var dir = File(System.getProperty("user.dir"))
        while (!File(dir, "settings.gradle.kts").exists() && dir.parentFile != null) {
            dir = dir.parentFile
        }
        return dir
    }
}
