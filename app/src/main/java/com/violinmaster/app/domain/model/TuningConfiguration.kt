package com.violinmaster.app.domain.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Represents a saved tuning preset configuration.
 *
 * Each preset stores a label, reference pitch (A4 frequency in Hz),
 * and maximum cents range for the tuning gauge. Used by
 * [com.violinmaster.app.di.TuningPreferencesManager] for per-user
 * CRUD persistence via SharedPreferences.
 *
 * @param label Human-readable preset name (e.g., "Baroque 415", "Orchestra 442").
 *              Must be non-empty and unique per user.
 * @param referencePitch Reference A4 frequency in Hz. Valid range: 350–500.
 * @param maxCents Maximum cents offset displayed on the tuning gauge.
 *                 Valid values: 25, 50, 100, 150, 200.
 */
@JsonClass(generateAdapter = true)
data class TuningConfiguration(
    @Json(name = "label")
    val label: String,
    @Json(name = "referencePitch")
    val referencePitch: Int = 440,
    @Json(name = "maxCents")
    val maxCents: Int = 50
)
