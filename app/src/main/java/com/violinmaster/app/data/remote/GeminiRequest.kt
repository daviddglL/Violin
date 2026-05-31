package com.violinmaster.app.data.remote

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Gemini API generateContent request body.
 * Maps to: https://ai.google.dev/api/rest/v1beta/models/generateContent
 */
@JsonClass(generateAdapter = true)
data class GeminiRequest(
    @field:Json(name = "contents") val contents: List<Content>
)

@JsonClass(generateAdapter = true)
data class Content(
    @field:Json(name = "parts") val parts: List<Part>
)

@JsonClass(generateAdapter = true)
data class Part(
    @field:Json(name = "text") val text: String
)
