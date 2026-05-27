package com.violinmaster.app.data.remote

import retrofit2.http.Body
import retrofit2.http.POST

/**
 * Retrofit interface for the Gemini API (generativelanguage.googleapis.com).
 *
 * REQ-GEM-001: At least one callable endpoint with Moshi serialization.
 * REQ-GEM-003: generateContent for lesson feedback / practice suggestions.
 * REQ-GEM-004: All methods are suspend functions (coroutine-based, Dispatchers.IO).
 *
 * Authentication is handled by [GeminiAuthInterceptor] via OkHttp,
 * which injects the user's OAuth Bearer token into outgoing requests.
 */
interface GeminiApiService {

    @POST("v1beta/models/gemini-2.5-flash:generateContent")
    suspend fun generateContent(
        @Body request: GeminiRequest
    ): GeminiResponse
}
