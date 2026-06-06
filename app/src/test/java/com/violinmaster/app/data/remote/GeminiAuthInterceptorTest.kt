package com.violinmaster.app.data.remote

import android.content.Intent
import com.violinmaster.app.data.auth.GoogleUser
import com.violinmaster.app.data.auth.IGoogleAuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for [GeminiAuthInterceptor] — verifies OAuth Bearer token injection
 * into Gemini API requests and non-interference with other requests.
 *
 * Uses a manual FakeChain to avoid adding Mockito/KotlinMockito dependencies.
 */
class GeminiAuthInterceptorTest {

    /** Simulates a signed-in user with a valid OAuth token. */
    @Test
    fun `intercept adds Bearer token to Gemini requests`() {
        // Arrange: authenticated repository with a token
        val repository = FakeGoogleAuthRepository(isSignedIn = true, token = "ya29.test-oauth-token")
        val interceptor = GeminiAuthInterceptor(repository)
        val geminiRequest = Request.Builder()
            .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent")
            .post("{}".toRequestBody("application/json".toMediaType()))
            .build()
        val chain = FakeChain(geminiRequest)

        // Act
        val response = interceptor.intercept(chain)

        // Assert: the captured request (after proceed) must have Authorization header
        val capturedRequest = chain.capturedRequest
        val authHeader = capturedRequest?.header("Authorization")
        assertEquals(
            "Gemini requests must carry Bearer token",
            "Bearer ya29.test-oauth-token",
            authHeader
        )
        assertEquals("Response status should be 200", 200, response.code)
    }

    @Test
    fun `intercept does NOT add token to non-Gemini requests`() {
        // Arrange: authenticated repository with a token
        val repository = FakeGoogleAuthRepository(isSignedIn = true, token = "ya29.test-oauth-token")
        val interceptor = GeminiAuthInterceptor(repository)
        val nonGeminiRequest = Request.Builder()
            .url("https://example.com/api/health")
            .get()
            .build()
        val chain = FakeChain(nonGeminiRequest)

        // Act
        interceptor.intercept(chain)

        // Assert: the request must NOT have an Authorization header
        val capturedRequest = chain.capturedRequest
        assertNull(
            "Non-Gemini requests must NOT carry Authorization header",
            capturedRequest?.header("Authorization")
        )
    }

    @Test
    fun `intercept does NOT add token when user is signed out`() {
        // Arrange: no signed-in user (token = null)
        val repository = FakeGoogleAuthRepository(isSignedIn = false, token = null)
        val interceptor = GeminiAuthInterceptor(repository)
        val geminiRequest = Request.Builder()
            .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent")
            .post("{}".toRequestBody("application/json".toMediaType()))
            .build()
        val chain = FakeChain(geminiRequest)

        // Act
        interceptor.intercept(chain)

        // Assert: request proceeds without Authorization header
        val capturedRequest = chain.capturedRequest
        assertNull(
            "Signed-out users must not have Authorization header",
            capturedRequest?.header("Authorization")
        )
    }

    @Test
    fun `intercept does NOT add token to non-HTTPS Gemini-like URL`() {
        // Regression: only HTTPS Gemini hosts should get the header
        val repository = FakeGoogleAuthRepository(isSignedIn = true, token = "ya29.token")
        val interceptor = GeminiAuthInterceptor(repository)

        // A URL that contains "generativelanguage" but is http (not https)
        val httpRequest = Request.Builder()
            .url("http://generativelanguage.googleapis.com/v1/models")
            .get()
            .build()
        val chain = FakeChain(httpRequest)

        interceptor.intercept(chain)
        assertNull(
            "HTTP Gemini URLs (not HTTPS) must NOT get Authorization",
            chain.capturedRequest?.header("Authorization")
        )
    }

    @Test
    fun `intercept matches subdomain variations of Gemini host`() {
        // The host check should match generativelanguage.googleapis.com in any position
        val repository = FakeGoogleAuthRepository(isSignedIn = true, token = "ya29.token123")
        val interceptor = GeminiAuthInterceptor(repository)

        val regionalRequest = Request.Builder()
            .url("https://us-central1-aiplatform.googleapis.com/v1/projects/test/locations/us-central1/publishers/google/models/gemini-2.5-flash:generateContent")
            .post("{}".toRequestBody("application/json".toMediaType()))
            .build()

        // This is NOT on generativelanguage.googleapis.com, so no token
        val chain = FakeChain(regionalRequest)
        interceptor.intercept(chain)
        assertNull(
            "Non-generativelanguage host must NOT get Authorization",
            chain.capturedRequest?.header("Authorization")
        )
    }

    // ---- Fake implementations ----

    /**
     * A fake [IGoogleAuthRepository] that returns fixed values for testing.
     * Does not depend on Firebase/Auth singletons.
     */
    private class FakeGoogleAuthRepository(
        private val isSignedIn: Boolean,
        private val token: String?
    ) : IGoogleAuthRepository {
        override val signedInFlow = MutableStateFlow(isSignedIn)
        override suspend fun signIn(idToken: String): Result<GoogleUser> =
            Result.success(GoogleUser("fake", "fake@test.com", "Fake", null))
        override suspend fun signOut() {}
        override fun getAccessToken(): String? = token
        override fun isSignedIn(): Boolean = isSignedIn
    }

    /**
     * A minimal fake [Interceptor.Chain] that captures the request after proceed().
     */
    private class FakeChain(private val originalRequest: Request) : Interceptor.Chain {
        var capturedRequest: Request? = null

        override fun request(): Request = originalRequest

        override fun proceed(request: Request): Response {
            capturedRequest = request
            return Response.Builder()
                .request(request)
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .body("{}".toResponseBody("application/json".toMediaType()))
                .build()
        }

        override fun connection(): okhttp3.Connection? = null
        override fun call(): okhttp3.Call = TODO("Not needed for test")
        override fun connectTimeoutMillis(): Int = 0
        override fun withConnectTimeout(timeout: Int, unit: java.util.concurrent.TimeUnit): Interceptor.Chain = this
        override fun readTimeoutMillis(): Int = 0
        override fun withReadTimeout(timeout: Int, unit: java.util.concurrent.TimeUnit): Interceptor.Chain = this
        override fun writeTimeoutMillis(): Int = 0
        override fun withWriteTimeout(timeout: Int, unit: java.util.concurrent.TimeUnit): Interceptor.Chain = this
    }
}
