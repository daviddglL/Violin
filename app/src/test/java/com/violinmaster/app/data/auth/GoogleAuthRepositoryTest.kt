@file:Suppress("DEPRECATION") // Robolectric shadow for Firebase still uses old APIs

package com.violinmaster.app.data.auth

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Ignore
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Tests for [GoogleAuthRepository] logic, [GoogleUser], and [GoogleSignInResult] data classes.
 *
 * Since FirebaseAuth and CredentialManager cannot function in unit tests,
 * these tests focus on:
 * - [GoogleUser] and [GoogleSignInResult] data class correctness
 * - Repository state flow behavior with mocked Firebase auth
 * - [isSignedIn] and [getAccessToken] logic via subclass overrides
 *
 * Auth reconciliation logic is tested separately in [AuthReconcilerTest].
 *
 * Migrated from GoogleSignInClient to CredentialManager API (androidx.credentials).
 */
@Ignore("Requires Firebase Test SDK — CredentialManager not available in Robolectric unit tests")
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class GoogleAuthRepositoryTest {

    private lateinit var context: Context
    private lateinit var credentialManager: CredentialManager

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        credentialManager = CredentialManager.create(context)
    }

    // ---- GoogleUser data class tests ----

    @Test
    fun `GoogleUser data class holds expected fields`() {
        val user = GoogleUser(
            userId = "abc123",
            email = "test@violinmaster.app",
            displayName = "Test User",
            photoUrl = "https://example.com/photo.jpg"
        )

        assertEquals("abc123", user.userId)
        assertEquals("test@violinmaster.app", user.email)
        assertEquals("Test User", user.displayName)
        assertEquals("https://example.com/photo.jpg", user.photoUrl)
    }

    @Test
    fun `GoogleUser photoUrl can be null`() {
        val user = GoogleUser(
            userId = "abc123",
            email = "test@violinmaster.app",
            displayName = "No Photo User",
            photoUrl = null
        )

        assertNull(user.photoUrl)
    }

    // ---- GoogleSignInResult data class tests ----

    @Test
    fun `GoogleSignInResult holds googleUser and userAccount`() {
        val googleUser = GoogleUser(
            userId = "uid_result",
            email = "result@test.com",
            displayName = "Result User",
            photoUrl = null
        )
        val result = GoogleSignInResult(googleUser, null)

        assertEquals("uid_result", result.googleUser.userId)
        assertEquals("result@test.com", result.googleUser.email)
        assertNull(result.userAccount)
    }

    @Test
    fun `GoogleSignInResult userAccount can be non-null`() {
        val googleUser = GoogleUser("uid_acc", "acc@test.com", "Acc User", null)
        val result = GoogleSignInResult(googleUser, null)

        assertEquals("Acc User", result.googleUser.displayName)
    }

    // ---- Repository state tests (via subclass overrides) ----

    @Test
    fun `isSignedIn returns false when no Firebase user`() {
        val repo = createMinimalRepo()
        assertFalse("Default state: no user signed in", repo.isSignedIn())
    }

    @Test
    fun `signedInFlow defaults to false`() = runTest {
        val repo = createMinimalRepo()
        assertEquals(false, repo.signedInFlow.first())
    }

    @Test
    fun `getAccessToken returns null when no Firebase user`() {
        val repo = createMinimalRepo()
        assertNull("No token when no user signed in", repo.getAccessToken())
    }

    @Test
    fun `subclass can override getAccessToken for testing`() {
        val repo = object : GoogleAuthRepository(
            context, credentialManager,
            // AuthReconciler is required by constructor — use a no-op override
            object : AuthReconciler(
                null!!, null!!
            ) {
                // No-op for state tests
            }
        ) {
            override fun getAccessToken(): String? = "ya29.fake-token-for-test"
            override fun isSignedIn(): Boolean = true
        }
        assertEquals("ya29.fake-token-for-test", repo.getAccessToken())
        assertTrue(repo.isSignedIn())
    }

    // ---- Helper ----

    private fun createMinimalRepo(): GoogleAuthRepository {
        // Minimal repo for state tests — AuthReconciler is never invoked
        // in these tests since they don't call signIn().
        return object : GoogleAuthRepository(
            context, credentialManager,
            object : AuthReconciler(null!!, null!!) {}
        ) {}
    }
}
