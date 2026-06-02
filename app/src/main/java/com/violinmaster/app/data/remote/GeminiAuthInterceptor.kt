package com.violinmaster.app.data.remote

import com.violinmaster.app.data.auth.IGoogleAuthRepository
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

/**
 * OkHttp [Interceptor] that injects the current user's Google OAuth token
 * into Gemini API requests as an `Authorization: Bearer <token>` header.
 *
 * Non-Gemini requests pass through unchanged. Requests to Gemini without
 * a valid token proceed without the header — Gemini will return 401,
 * which [GeminiRepository] already handles.
 *
 * Gemini endpoint: generativelanguage.googleapis.com (HTTPS only).
 */
@Singleton
class GeminiAuthInterceptor @Inject constructor(
    private val googleAuthRepository: IGoogleAuthRepository
) : Interceptor {

    companion object {
        private const val GEMINI_HOST = "generativelanguage.googleapis.com"
        private const val HEADER_AUTHORIZATION = "Authorization"
        private const val BEARER_PREFIX = "Bearer "
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val url = originalRequest.url

        // Only attach OAuth token to Gemini API calls
        if (url.isHttps && GEMINI_HOST in url.host) {
            val token = googleAuthRepository.getAccessToken()
            if (token != null) {
                val authenticatedRequest = originalRequest.newBuilder()
                    .header(HEADER_AUTHORIZATION, "$BEARER_PREFIX$token")
                    .build()
                return chain.proceed(authenticatedRequest)
            }
        }

        // Non-Gemini requests or no token available: proceed unchanged
        return chain.proceed(originalRequest)
    }
}
