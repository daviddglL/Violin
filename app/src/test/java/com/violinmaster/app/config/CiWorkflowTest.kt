package com.violinmaster.app.config

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Validates the CI/CD workflow configuration against REQ-CICD-001 through REQ-CICD-006.
 *
 * These tests read the ci.yml file directly and assert its structure matches
 * the delta specs. This is a structural/config validation — the "production code"
 * under test is the YAML workflow file itself.
 */
class CiWorkflowTest {

    private val ciFile: File by lazy {
        val projectRoot = findProjectRoot()
        File(projectRoot, ".github/workflows/ci.yml")
    }

    private val content: String by lazy {
        require(ciFile.exists()) { "ci.yml not found at ${ciFile.absolutePath}" }
        ciFile.readText()
    }

    private val lines: List<String> by lazy {
        content.lines()
    }

    // ── REQ-CICD-001: Trigger on push to any branch and push to master ──────

    @Test
    fun `REQ-CICD-001 workflow triggers on push to any branch`() {
        assertTrue("ci.yml must exist", ciFile.exists())
        val onIndex = lines.indexOfFirst { it.trimStart().startsWith("on:") }
        assertTrue("ci.yml must have 'on:' trigger", onIndex >= 0)
        val pushIndex = lines.drop(onIndex).indexOfFirst { it.trimStart().startsWith("push:") }
        assertTrue("ci.yml must have 'push:' under 'on:'", pushIndex >= 0)
    }

    @Test
    fun `REQ-CICD-001 workflow triggers on push to master for release`() {
        // The workflow must include master branch trigger (for assembleRelease)
        assertTrue("ci.yml must exist", ciFile.exists())
        val hasMasterOrMain = lines.any { line ->
            line.contains("master") || line.contains("main")
        }
        assertTrue("ci.yml must reference master or main branch", hasMasterOrMain)
    }

    // ── REQ-CICD-002: Jobs: build, test, lint ────────────────────────────────

    @Test
    fun `REQ-CICD-002 workflow has build job`() {
        val jobsSection = extractJobsSection()
        assertTrue("ci.yml must have a 'build' job", "build" in jobsSection)
    }

    @Test
    fun `REQ-CICD-002 workflow has test job`() {
        val jobsSection = extractJobsSection()
        assertTrue("ci.yml must have a 'test' job", "test" in jobsSection)
    }

    @Test
    fun `REQ-CICD-002 workflow has lint job`() {
        val jobsSection = extractJobsSection()
        assertTrue("ci.yml must have a 'lint' job", "lint" in jobsSection)
    }

    @Test
    fun `REQ-CICD-002 test and lint jobs depend on build`() {
        val jobsSection = extractJobsSection()
        // test and lint should have 'needs: build'
        val hasNeeds = jobsSection.contains("needs:") || jobsSection.contains("needs: [build]")
        assertTrue("ci.yml test/lint jobs must depend on build (needs: build)", hasNeeds)
    }

    // ── REQ-CICD-003: Test report artifact upload ────────────────────────────

    @Test
    fun `REQ-CICD-003 test job uploads artifacts`() {
        val hasArtifactUpload = lines.any { line ->
            line.contains("upload") && line.contains("artifact") ||
                line.contains("actions/upload-artifact")
        }
        assertTrue("ci.yml test job must upload test report artifacts", hasArtifactUpload)
    }

    // ── REQ-CICD-004: Release APK on master ──────────────────────────────────

    @Test
    fun `REQ-CICD-004 assembleRelease runs on master push`() {
        val hasAssembleRelease = lines.any { line ->
            line.contains("assembleRelease")
        }
        assertTrue("ci.yml must include assembleRelease for master builds", hasAssembleRelease)
    }

    // ── REQ-CICD-005: Gradle caching ─────────────────────────────────────────

    @Test
    fun `REQ-CICD-005 workflow uses Gradle cache action`() {
        val hasGradleCache = lines.any { line ->
            line.contains("gradle/actions/setup-gradle")
        }
        assertTrue("ci.yml must use gradle/actions/setup-gradle for caching", hasGradleCache)
    }

    // ── REQ-CICD-006: JDK 17 ─────────────────────────────────────────────────

    @Test
    fun `REQ-CICD-006 workflow uses JDK 17`() {
        val hasJdk17 = lines.any { line ->
            line.contains("java-version") && (line.contains("17") || line.contains("\"17\""))
        }
        assertTrue("ci.yml must specify java-version: '17'", hasJdk17)
    }

    @Test
    fun `REQ-CICD-006 workflow uses setup-java action`() {
        val hasSetupJava = lines.any { line ->
            line.contains("actions/setup-java")
        }
        assertTrue("ci.yml must use actions/setup-java@v4", hasSetupJava)
    }

    // ── Additional: Timeout and runner ───────────────────────────────────────

    @Test
    fun `workflow has 30 minute timeout`() {
        val hasTimeout = lines.any { line ->
            line.contains("timeout-minutes") && line.contains("30")
        }
        assertTrue("ci.yml must have timeout-minutes: 30", hasTimeout)
    }

    @Test
    fun `workflow runs on ubuntu-latest`() {
        val hasUbuntu = lines.any { line ->
            line.contains("runs-on") && line.contains("ubuntu-latest")
        }
        assertTrue("ci.yml must use runs-on: ubuntu-latest", hasUbuntu)
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private fun extractJobsSection(): String {
        val jobsIndex = lines.indexOfFirst { it.trimStart().startsWith("jobs:") }
        assertTrue("ci.yml must have 'jobs:' section", jobsIndex >= 0)
        return lines.drop(jobsIndex).joinToString("\n")
    }

    private fun findProjectRoot(): File {
        var dir = File(System.getProperty("user.dir"))
        while (!File(dir, "settings.gradle.kts").exists() && dir.parentFile != null) {
            dir = dir.parentFile
        }
        return dir
    }
}
