package com.violinmaster.app.config

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Validates the google-services.json configuration against REQ-STAGING-003
 * and REQ-STAGING-004.
 *
 * Verifies that:
 * - Root google-services.json has been moved to flavor source sets
 * - Each flavor has its own valid google-services.json
 * - The production JSON contains the correct Firebase project ID
 */
class GoogleServicesConfigTest {

    private val projectRoot: File by lazy {
        var dir = File(System.getProperty("user.dir"))
        while (!File(dir, "settings.gradle.kts").exists() && dir.parentFile != null) {
            dir = dir.parentFile
        }
        dir
    }

    // ── REQ-STAGING-003: Per-flavor google-services.json ─────────────────────

    @Test
    fun `REQ-STAGING-003 root google-services json is removed`() {
        val rootJson = File(projectRoot, "app/google-services.json")
        assertFalse(
            "app/google-services.json must NOT exist (moved to flavor dirs)",
            rootJson.exists()
        )
    }

    @Test
    fun `REQ-STAGING-003 prod google-services json exists`() {
        val prodJson = File(projectRoot, "app/src/prod/google-services.json")
        assertTrue(
            "app/src/prod/google-services.json must exist",
            prodJson.exists()
        )
    }

    @Test
    fun `REQ-STAGING-003 dev google-services json exists`() {
        val devJson = File(projectRoot, "app/src/dev/google-services.json")
        assertTrue(
            "app/src/dev/google-services.json must exist",
            devJson.exists()
        )
    }

    @Test
    fun `REQ-STAGING-003 prod google-services json is valid JSON`() {
        val prodJson = File(projectRoot, "app/src/prod/google-services.json")
        assertTrue("prod google-services.json must exist", prodJson.exists())
        val content = prodJson.readText()
        // Basic JSON validation: must start with { and contain project_info
        assertTrue(
            "prod google-services.json must be valid JSON starting with {",
            content.trimStart().startsWith("{")
        )
        assertTrue(
            "prod google-services.json must contain project_info",
            content.contains("project_info")
        )
    }

    @Test
    fun `REQ-STAGING-003 dev google-services json is valid JSON`() {
        val devJson = File(projectRoot, "app/src/dev/google-services.json")
        assertTrue("dev google-services.json must exist", devJson.exists())
        val content = devJson.readText()
        assertTrue(
            "dev google-services.json must be valid JSON starting with {",
            content.trimStart().startsWith("{")
        )
        assertTrue(
            "dev google-services.json must contain project_info",
            content.contains("project_info")
        )
    }

    // ── REQ-STAGING-004: Firebase project separation ──────────────────────────

    @Test
    fun `REQ-STAGING-004 prod google-services has production project ID`() {
        val prodJson = File(projectRoot, "app/src/prod/google-services.json")
        assertTrue("prod google-services.json must exist", prodJson.exists())
        val content = prodJson.readText()
        assertTrue(
            "prod google-services.json must reference violin-app-795ee",
            content.contains("violin-app-795ee")
        )
    }

    @Test
    fun `REQ-STAGING-004 dev google-services uses different project than prod`() {
        val devJson = File(projectRoot, "app/src/dev/google-services.json")
        val prodJson = File(projectRoot, "app/src/prod/google-services.json")
        assertTrue("dev google-services.json must exist", devJson.exists())
        assertTrue("prod google-services.json must exist", prodJson.exists())
        val devContent = devJson.readText()
        val prodContent = prodJson.readText()
        // Dev should use a different project ID from prod
        assertFalse(
            "dev google-services.json must use different project than prod (violin-app-795ee)",
            devContent.contains("violin-app-795ee")
        )
    }

    // ── Package name validation ──────────────────────────────────────────────

    @Test
    fun `prod google-services has correct package name`() {
        val prodJson = File(projectRoot, "app/src/prod/google-services.json")
        assertTrue("prod google-services.json must exist", prodJson.exists())
        val content = prodJson.readText()
        assertTrue(
            "prod must have package_name com.violinmaster.app",
            content.contains("com.violinmaster.app")
        )
    }

    @Test
    fun `dev google-services has dev package name`() {
        val devJson = File(projectRoot, "app/src/dev/google-services.json")
        assertTrue("dev google-services.json must exist", devJson.exists())
        val content = devJson.readText()
        assertTrue(
            "dev must have package_name com.violinmaster.app.dev",
            content.contains("com.violinmaster.app.dev")
        )
    }
}
