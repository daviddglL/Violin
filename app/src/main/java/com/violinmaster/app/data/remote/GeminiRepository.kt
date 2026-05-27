package com.violinmaster.app.data.remote

import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import retrofit2.HttpException

/**
 * Repository that wraps GeminiApiService calls with error handling.
 *
 * REQ-GEM-005: Handles IOException (network), HttpException 401/403 (auth),
 * and HttpException 429 (rate limiting) with distinct error messages.
 */
@Singleton
class GeminiRepository @Inject constructor(
    private val api: GeminiApiService
) {

    /**
     * Generates lesson feedback from a practice session prompt.
     *
     * @param prompt The user-facing prompt describing the practice context.
     * @param apiKey The Gemini API key (from BuildConfig / Secrets plugin).
     * @return [Result] containing the generated text or an error message.
     */
    suspend fun generateLessonFeedback(prompt: String, apiKey: String): Result<String> {
        return try {
            val request = GeminiRequest(
                contents = listOf(
                    Content(
                        parts = listOf(Part(text = prompt))
                    )
                )
            )
            val response = api.generateContent(apiKey, request)
            val text = response.candidates
                .firstOrNull()
                ?.content
                ?.parts
                ?.firstOrNull()
                ?.text

            if (text != null) {
                Result.success(text)
            } else {
                Result.failure(IOException("Empty response from Gemini API"))
            }
        } catch (e: HttpException) {
            val message = when (e.code()) {
                401, 403 -> "API authentication failed. Check your API key in .env."
                429 -> "Too many requests. Try again in a moment."
                else -> "Gemini API error (HTTP ${e.code()}): ${e.message()}"
            }
            Result.failure(IOException(message))
        } catch (e: IOException) {
            Result.failure(IOException("Network unavailable. Check your connection."))
        }
    }
}
