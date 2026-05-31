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
 *
 * Authentication is handled transparently by [GeminiAuthInterceptor] —
 * the repository no longer needs to pass an API key.
 */
@Singleton
class GeminiRepository @Inject constructor(
    private val api: GeminiApiService
) {

    /**
     * Generates lesson feedback from a practice session prompt.
     *
     * Authentication is handled by [GeminiAuthInterceptor] via OkHttp.
     * If the user is not signed in, Gemini returns 401 (handled below).
     *
     * @param prompt The user-facing prompt describing the practice context.
     * @return [Result] containing the generated text or an error message.
     */
    suspend fun generateLessonFeedback(prompt: String): Result<String> {
        return try{
            val request = GeminiRequest(
                contents = listOf(
                    Content(
                        parts = listOf(Part(text = prompt))
                    )
                )
            )
            val response = api.generateContent(request)
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
                401, 403 -> "Authentication failed. Sign in with Google to use AI features."
                429 -> "Too many requests. Try again in a moment."
                else -> "Gemini API error (HTTP ${e.code()}): ${e.message()}"
            }
            Result.failure(IOException(message))
        } catch (e: IOException) {
            Result.failure(IOException("Network unavailable. Check your connection."))
        }
    }
}
