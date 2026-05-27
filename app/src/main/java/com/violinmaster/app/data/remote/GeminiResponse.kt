package com.violinmaster.app.data.remote

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Gemini API generateContent response body.
 * Maps to: https://ai.google.dev/api/rest/v1beta/models/generateContent#response-body
 */
@JsonClass(generateAdapter = true)
data class GeminiResponse(
    @Json(name = "candidates") val candidates: List<Candidate> = emptyList()
)

@JsonClass(generateAdapter = true)
data class Candidate(
    @Json(name = "content") val content: Content? = null
)
