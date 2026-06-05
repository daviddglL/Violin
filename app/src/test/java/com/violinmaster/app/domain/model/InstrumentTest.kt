package com.violinmaster.app.domain.model

import com.violinmaster.app.ui.component.octaveLabel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * TDD tests for Instrument enum and InstrumentString data class.
 *
 * RED phase: Instrument.kt does not exist yet.
 * These tests define the contract before implementation.
 *
 * Spec: instrument-selection — Instrument Data Model
 */
class InstrumentTest {

  // ── InstrumentString data class ─────────────────────────────────────

  @Test
  fun `InstrumentString stores name and frequency`() {
    val str = InstrumentString(name = "G", frequency = 196.0)
    assertEquals("G", str.name)
    assertEquals(196.0, str.frequency, 0.001)
  }

  @Test
  fun `InstrumentString equality by value`() {
    val a = InstrumentString(name = "A", frequency = 440.0)
    val b = InstrumentString(name = "A", frequency = 440.0)
    assertEquals("Same values should be equal", a, b)
  }

  // ── Enum values ─────────────────────────────────────────────────────

  @Test
  fun `Instrument has exactly four values`() {
    val values = Instrument.values()
    assertEquals("Should have 4 instrument values", 4, values.size)
    assertTrue("Should contain VIOLIN", values.contains(Instrument.VIOLIN))
    assertTrue("Should contain VIOLA", values.contains(Instrument.VIOLA))
    assertTrue("Should contain CELLO", values.contains(Instrument.CELLO))
    assertTrue("Should contain DOUBLE_BASS", values.contains(Instrument.DOUBLE_BASS))
  }

  // ── Label keys ──────────────────────────────────────────────────────

  @Test
  fun `VIOLIN labelKey is instrument_violin`() {
    assertEquals("instrument_violin", Instrument.VIOLIN.labelKey)
  }

  @Test
  fun `VIOLA labelKey is instrument_viola`() {
    assertEquals("instrument_viola", Instrument.VIOLA.labelKey)
  }

  @Test
  fun `CELLO labelKey is instrument_cello`() {
    assertEquals("instrument_cello", Instrument.CELLO.labelKey)
  }

  // ── Violin strings ──────────────────────────────────────────────────

  @Test
  fun `VIOLIN has four strings G3 D4 A4 E5`() {
    val strings = Instrument.VIOLIN.strings
    assertEquals("Violin should have 4 strings", 4, strings.size)

    assertEquals("G", strings[0].name)
    assertEquals(196.0, strings[0].frequency, 0.01)

        assertEquals("D", strings[1].name)
        assertEquals(293.66, strings[1].frequency, 0.01)

        assertEquals("A", strings[2].name)
        assertEquals(440.0, strings[2].frequency, 0.01)

        assertEquals("E", strings[3].name)
        assertEquals(659.25, strings[3].frequency, 0.01)
  }

  // ── Viola strings ───────────────────────────────────────────────────

  @Test
  fun `VIOLA has four strings C3 G3 D4 A4`() {
    val strings = Instrument.VIOLA.strings
    assertEquals("Viola should have 4 strings", 4, strings.size)

    assertEquals("C", strings[0].name)
    assertEquals(130.8, strings[0].frequency, 0.01)

    assertEquals("G", strings[1].name)
    assertEquals(196.0, strings[1].frequency, 0.01)

        assertEquals("D", strings[2].name)
        assertEquals(293.66, strings[2].frequency, 0.01)

    assertEquals("A", strings[3].name)
    assertEquals(440.0, strings[3].frequency, 0.01)
  }

  // ── Cello strings ───────────────────────────────────────────────────

  @Test
  fun `CELLO has four strings C2 G2 D3 A3`() {
    val strings = Instrument.CELLO.strings
    assertEquals("Cello should have 4 strings", 4, strings.size)

    assertEquals("C", strings[0].name)
    assertEquals(65.4, strings[0].frequency, 0.01)

    assertEquals("G", strings[1].name)
    assertEquals(98.0, strings[1].frequency, 0.01)

    assertEquals("D", strings[2].name)
    assertEquals(146.8, strings[2].frequency, 0.01)

    assertEquals("A", strings[3].name)
    assertEquals(220.0, strings[3].frequency, 0.01)
  }

  // ── Double Bass strings ──────────────────────────────────────────────

  @Test
  fun `DOUBLE_BASS has four strings E1 A1 D2 G2`() {
    val strings = Instrument.DOUBLE_BASS.strings
    assertEquals("Double Bass should have 4 strings", 4, strings.size)

    assertEquals("E", strings[0].name)
    assertEquals(41.2, strings[0].frequency, 0.01)

    assertEquals("A", strings[1].name)
    assertEquals(55.0, strings[1].frequency, 0.01)

    assertEquals("D", strings[2].name)
    assertEquals(73.4, strings[2].frequency, 0.01)

    assertEquals("G", strings[3].name)
    assertEquals(98.0, strings[3].frequency, 0.01)
  }

  @Test
  fun `DOUBLE_BASS labelKey is instrument_double_bass`() {
    assertEquals("instrument_double_bass", Instrument.DOUBLE_BASS.labelKey)
  }

  // ── valueOf roundtrip ───────────────────────────────────────────────

  @Test
  fun `valueOf CELLO returns Instrument CELLO`() {
    assertEquals(Instrument.CELLO, Instrument.valueOf("CELLO"))
    assertEquals("CELLO", Instrument.CELLO.name)
  }

  @Test
  fun `valueOf VIOLIN roundtrip`() {
    assertEquals(Instrument.VIOLIN, Instrument.valueOf("VIOLIN"))
  }

  @Test
  fun `valueOf DOUBLE_BASS roundtrip`() {
    assertEquals(Instrument.DOUBLE_BASS, Instrument.valueOf("DOUBLE_BASS"))
    assertEquals("DOUBLE_BASS", Instrument.DOUBLE_BASS.name)
  }

  @Test
  fun `valueOf VIOLA roundtrip`() {
    assertEquals(Instrument.VIOLA, Instrument.valueOf("VIOLA"))
  }

  // ── name property ───────────────────────────────────────────────────

  @Test
  fun `Instrument name matches enum constant name`() {
    assertEquals("VIOLIN", Instrument.VIOLIN.name)
    assertEquals("VIOLA", Instrument.VIOLA.name)
    assertEquals("CELLO", Instrument.CELLO.name)
  }

    // ── No Android dependencies ─────────────────────────────────────────

    @Test
    fun `Instrument is pure Kotlin — no Android imports needed`() {
        // Verify Instrument can be used without any Android context
        val instrument: Instrument = Instrument.VIOLIN
        assertNotNull(instrument)
        assertEquals(4, instrument.strings.size)
    }

    // ── octaveLabel ─────────────────────────────────────────────────────

    @Test
    fun `octaveLabel — violin G3 (196 Hz)`() {
        assertEquals("G3", octaveLabel(InstrumentString("G", 196.0)))
    }

    @Test
    fun `octaveLabel — violin D4 (293_66 Hz)`() {
        assertEquals("D4", octaveLabel(InstrumentString("D", 293.66)))
    }

    @Test
    fun `octaveLabel — A4 reference (440 Hz)`() {
        assertEquals("A4", octaveLabel(InstrumentString("A", 440.0)))
    }

    @Test
    fun `octaveLabel — violin E5 (659_25 Hz)`() {
        assertEquals("E5", octaveLabel(InstrumentString("E", 659.25)))
    }

    @Test
    fun `octaveLabel — viola C3 (130_8 Hz)`() {
        assertEquals("C3", octaveLabel(InstrumentString("C", 130.8)))
    }

    @Test
    fun `octaveLabel — cello C2 (65_4 Hz)`() {
        assertEquals("C2", octaveLabel(InstrumentString("C", 65.4)))
    }

    @Test
    fun `octaveLabel — cello G2 (98 Hz)`() {
        assertEquals("G2", octaveLabel(InstrumentString("G", 98.0)))
    }

    @Test
    fun `octaveLabel — cello D3 (146_8 Hz)`() {
        assertEquals("D3", octaveLabel(InstrumentString("D", 146.8)))
    }

    @Test
    fun `octaveLabel — cello A3 (220 Hz)`() {
        assertEquals("A3", octaveLabel(InstrumentString("A", 220.0)))
    }

    @Test
    fun `octaveLabel — viola G3 (196 Hz) same octave as violin G`() {
        assertEquals("G3", octaveLabel(InstrumentString("G", 196.0)))
    }
}
