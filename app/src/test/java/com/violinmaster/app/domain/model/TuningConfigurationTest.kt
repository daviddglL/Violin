package com.violinmaster.app.domain.model

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * TDD tests for TuningConfiguration domain model.
 *
 * RED phase: TuningConfiguration.kt does not exist yet.
 * These tests define the contract before implementation.
 */
class TuningConfigurationTest {

    private val moshi: Moshi = Moshi.Builder().build()

    // ── Construction ──────────────────────────────────────────────────

    @Test
    fun `default construction uses referencePitch 440 and maxCents 50`() {
        val config = TuningConfiguration(label = "Default")
        assertEquals("Default", config.label)
        assertEquals(440, config.referencePitch)
        assertEquals(50, config.maxCents)
    }

    @Test
    fun `custom construction stores provided values`() {
        val config = TuningConfiguration(
            label = "Baroque 415",
            referencePitch = 415,
            maxCents = 100
        )
        assertEquals("Baroque 415", config.label)
        assertEquals(415, config.referencePitch)
        assertEquals(100, config.maxCents)
    }

    // ── JSON Single Object Serialization ──────────────────────────────

    @Test
    fun `serialize single TuningConfiguration to JSON`() {
        val config = TuningConfiguration(
            label = "Orchestra 442",
            referencePitch = 442,
            maxCents = 25
        )
        val adapter: JsonAdapter<TuningConfiguration> =
            moshi.adapter(TuningConfiguration::class.java)
        val json = adapter.toJson(config)

        assertTrue("JSON should contain label", "\"label\":\"Orchestra 442\"" in json)
        assertTrue("JSON should contain referencePitch", "\"referencePitch\":442" in json)
        assertTrue("JSON should contain maxCents", "\"maxCents\":25" in json)
    }

    @Test
    fun `deserialize single TuningConfiguration from JSON`() {
        val json = """{"label":"Baroque 415","referencePitch":415,"maxCents":50}"""
        val adapter: JsonAdapter<TuningConfiguration> =
            moshi.adapter(TuningConfiguration::class.java)
        val config = adapter.fromJson(json)

        assertEquals("Baroque 415", config?.label)
        assertEquals(415, config?.referencePitch)
        assertEquals(50, config?.maxCents)
    }

    // ── JSON List Serialization (for presets array) ───────────────────

    @Test
    fun `serialize list of TuningConfiguration to JSON array`() {
        val configs = listOf(
            TuningConfiguration(label = "Default", referencePitch = 440, maxCents = 50),
            TuningConfiguration(label = "Baroque", referencePitch = 415, maxCents = 25)
        )
        val listType = Types.newParameterizedType(
            List::class.java, TuningConfiguration::class.java
        )
        val adapter: JsonAdapter<List<TuningConfiguration>> = moshi.adapter(listType)
        val json = adapter.toJson(configs)

        assertTrue("JSON array should contain Default", "\"label\":\"Default\"" in json)
        assertTrue("JSON array should contain Baroque", "\"label\":\"Baroque\"" in json)
    }

    @Test
    fun `deserialize list of TuningConfiguration from JSON array`() {
        val json = """[{"label":"Default","referencePitch":440,"maxCents":50},{"label":"Baroque","referencePitch":415,"maxCents":25}]"""
        val listType = Types.newParameterizedType(
            List::class.java, TuningConfiguration::class.java
        )
        val adapter: JsonAdapter<List<TuningConfiguration>> = moshi.adapter(listType)
        val configs = adapter.fromJson(json)

        assertEquals(2, configs?.size)
        assertEquals("Default", configs?.get(0)?.label)
        assertEquals(440, configs?.get(0)?.referencePitch)
        assertEquals("Baroque", configs?.get(1)?.label)
        assertEquals(415, configs?.get(1)?.referencePitch)
    }

    @Test
    fun `deserialize empty JSON array returns empty list`() {
        val json = "[]"
        val listType = Types.newParameterizedType(
            List::class.java, TuningConfiguration::class.java
        )
        val adapter: JsonAdapter<List<TuningConfiguration>> = moshi.adapter(listType)
        val configs = adapter.fromJson(json)

        assertTrue("Empty JSON array should deserialize to empty list", configs?.isEmpty() ?: false)
    }

    // ── Round-trip ────────────────────────────────────────────────────

    @Test
    fun `round-trip serialize then deserialize preserves data`() {
        val original = TuningConfiguration(
            label = "Custom 432",
            referencePitch = 432,
            maxCents = 150
        )
        val adapter: JsonAdapter<TuningConfiguration> =
            moshi.adapter(TuningConfiguration::class.java)
        val json = adapter.toJson(original)
        val restored = adapter.fromJson(json)

        assertEquals(original.label, restored?.label)
        assertEquals(original.referencePitch, restored?.referencePitch)
        assertEquals(original.maxCents, restored?.maxCents)
    }

    // ── Equality ──────────────────────────────────────────────────────

    @Test
    fun `two configurations with same values are equal`() {
        val a = TuningConfiguration(label = "Test", referencePitch = 440, maxCents = 50)
        val b = TuningConfiguration(label = "Test", referencePitch = 440, maxCents = 50)
        assertEquals(a, b)
    }

    @Test
    fun `two configurations with different labels are not equal`() {
        val a = TuningConfiguration(label = "A", referencePitch = 440, maxCents = 50)
        val b = TuningConfiguration(label = "B", referencePitch = 440, maxCents = 50)
        assertTrue(a != b)
    }
}
