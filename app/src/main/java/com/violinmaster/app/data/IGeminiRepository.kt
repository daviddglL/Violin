package com.violinmaster.app.data

/**
 * Repository interface for Gemini AI feedback generation.
 *
 * REQ-ARCH-008: Repository interfaces enable dependency inversion,
 * allowing consumers to depend on abstractions instead of concrete classes.
 */
interface IGeminiRepository {

    /**
     * Generates lesson feedback from a practice session prompt.
     *
     * Authentication is handled transparently by [GeminiAuthInterceptor] via OkHttp.
     * If the user is not signed in, Gemini returns 401.
     *
     * @param prompt The user-facing prompt describing the practice context.
     * @return [Result] containing the generated text or an error message.
     */
    suspend fun generateLessonFeedback(prompt: String): Result<String>
}
