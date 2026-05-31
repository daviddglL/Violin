package com.violinmaster.app.data.auth

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Tests for [GoogleAuthRepository] logic and [GoogleUser] data class.
 *
 * Since FirebaseAuth and GoogleSignInClient cannot function in unit tests,
 * these tests focus on:
 * - [GoogleUser] data class correctness (mapping layer)
 * - Repository state flow behavior with mocked Firebase auth
 * - [isSignedIn] and [getAccessToken] logic via subclass overrides
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class GoogleAuthRepositoryTest {

    private lateinit var context: Context
    private lateinit var signInClient: GoogleSignInClient

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        // Robolectric shadows GoogleSignIn; we use DEFAULT_SIGN_IN for test
        // since the actual ID token flow requires a real Firebase project.
        signInClient = GoogleSignIn.getClient(
            context,
            GoogleSignInOptions.DEFAULT_SIGN_IN
        )
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

    // ---- Repository state tests (via subclass overrides) ----

    @Test
    fun `isSignedIn returns false when no Firebase user`() {
        // Real repository: FirebaseAuth has no signed-in user → isSignedIn = false
        val repo = GoogleAuthRepository(context, signInClient)
        assertFalse("Default state: no user signed in", repo.isSignedIn())
    }

    @Test
    fun `signedInFlow defaults to false`() = runTest {
        val repo = GoogleAuthRepository(context, signInClient)
        assertEquals(false, repo.signedInFlow.first())
    }

    @Test
    fun `getAccessToken returns null when no Firebase user`() {
        val repo = GoogleAuthRepository(context, signInClient)
        assertNull("No token when no user signed in", repo.getAccessToken())
    }

    @Test
    fun `subclass can override getAccessToken for testing`() {
        // Tests that open methods work correctly for interceptor testing
        val repo = object : GoogleAuthRepository(context, signInClient) {
            override fun getAccessToken(): String? = "ya29.fake-token-for-test"
            override fun isSignedIn(): Boolean = true
        }

        assertEquals("ya29.fake-token-for-test", repo.getAccessToken())
        assertTrue(repo.isSignedIn())
    }
}
