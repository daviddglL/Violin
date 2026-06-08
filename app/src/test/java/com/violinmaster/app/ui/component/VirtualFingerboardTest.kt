package com.violinmaster.app.ui.component

import com.violinmaster.app.domain.model.Instrument
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * MI-001: VirtualFingerboard multi-instrument fingering maps.
 *
 * Verifies that fingeringMap is keyed by Instrument enum and contains
 * correct string-to-positions entries for violin, viola, cello, and bass.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class VirtualFingerboardTest {

    // ═══════════════════════════════════════════════════════════════
    // MI-001(a): Each instrument has 4 strings × 6 positions
    // ═══════════════════════════════════════════════════════════════

    @Test
    fun `MI-001 - fingeringMap uses Instrument enum keys`() {
        assertNotNull("fingeringMap must have VIOLIN key", fingeringMap[Instrument.VIOLIN])
        assertNotNull("fingeringMap must have VIOLA key", fingeringMap[Instrument.VIOLA])
        assertNotNull("fingeringMap must have CELLO key", fingeringMap[Instrument.CELLO])
        assertNotNull("fingeringMap must have DOUBLE_BASS key", fingeringMap[Instrument.DOUBLE_BASS])
        assertEquals("fingeringMap must have exactly 4 instrument entries", 4, fingeringMap.size)
    }

    @Test
    fun `MI-001a - VIOLIN has 4 strings G D A E with 6 positions each`() {
        val violin = fingeringMap[Instrument.VIOLIN]
        assertNotNull("VIOLIN key must exist in fingeringMap", violin)
        assertEquals("Violin must have 4 string entries", 4, violin!!.size)
        assertTrue("Violin must have G string", violin.containsKey("G"))
        assertTrue("Violin must have D string", violin.containsKey("D"))
        assertTrue("Violin must have A string", violin.containsKey("A"))
        assertTrue("Violin must have E string", violin.containsKey("E"))
        violin.values.forEach { positions ->
            assertEquals("Each violin string must have 6 fingering positions", 6, positions.size)
        }
    }

    @Test
    fun `MI-001a - VIOLA has 4 strings C G D A with 6 positions each`() {
        val viola = fingeringMap[Instrument.VIOLA]
        assertNotNull("VIOLA key must exist in fingeringMap", viola)
        assertEquals("Viola must have 4 string entries", 4, viola!!.size)
        assertTrue("Viola must have C string", viola.containsKey("C"))
        assertTrue("Viola must have G string", viola.containsKey("G"))
        assertTrue("Viola must have D string", viola.containsKey("D"))
        assertTrue("Viola must have A string", viola.containsKey("A"))
        viola.values.forEach { positions ->
            assertEquals("Each viola string must have 6 fingering positions", 6, positions.size)
        }
    }

    @Test
    fun `MI-001b - CELLO has 4 strings C G D A with 6 positions each`() {
        val cello = fingeringMap[Instrument.CELLO]
        assertNotNull("CELLO key must exist in fingeringMap", cello)
        assertEquals("Cello must have 4 string entries", 4, cello!!.size)
        assertTrue("Cello must have C string", cello.containsKey("C"))
        assertTrue("Cello must have G string", cello.containsKey("G"))
        assertTrue("Cello must have D string", cello.containsKey("D"))
        assertTrue("Cello must have A string", cello.containsKey("A"))
        cello.values.forEach { positions ->
            assertEquals("Each cello string must have 6 fingering positions", 6, positions.size)
        }
    }

    @Test
    fun `MI-001c - DOUBLE_BASS has 4 strings E A D G with 6 positions each`() {
        val bass = fingeringMap[Instrument.DOUBLE_BASS]
        assertNotNull("DOUBLE_BASS key must exist in fingeringMap", bass)
        assertEquals("Bass must have 4 string entries", 4, bass!!.size)
        assertTrue("Bass must have E string", bass.containsKey("E"))
        assertTrue("Bass must have A string", bass.containsKey("A"))
        assertTrue("Bass must have D string", bass.containsKey("D"))
        assertTrue("Bass must have G string", bass.containsKey("G"))
        bass.values.forEach { positions ->
            assertEquals("Each bass string must have 6 fingering positions", 6, positions.size)
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // MI-001(b): Cello open-string frequencies are correct
    // ═══════════════════════════════════════════════════════════════

    @Test
    fun `MI-001b - CELLO C2 open string has 65_41 Hz`() {
        val cello = fingeringMap[Instrument.CELLO]
        val cOpen = cello!!["C"]!![0]
        assertEquals("Cello C open should be C2", "C2", cOpen.noteName)
        assertEquals("Cello C open frequency", 65.41, cOpen.frequency, 0.01)
    }

    @Test
    fun `MI-001b - CELLO G2 open string has 98_00 Hz`() {
        val cello = fingeringMap[Instrument.CELLO]
        val gOpen = cello!!["G"]!![0]
        assertEquals("Cello G open should be G2", "G2", gOpen.noteName)
        assertEquals("Cello G open frequency", 98.00, gOpen.frequency, 0.01)
    }

    @Test
    fun `MI-001b - CELLO D3 open string has 146_83 Hz`() {
        val cello = fingeringMap[Instrument.CELLO]
        val dOpen = cello!!["D"]!![0]
        assertEquals("Cello D open should be D3", "D3", dOpen.noteName)
        assertEquals("Cello D open frequency", 146.83, dOpen.frequency, 0.01)
    }

    @Test
    fun `MI-001b - CELLO A3 open string has 220_00 Hz`() {
        val cello = fingeringMap[Instrument.CELLO]
        val aOpen = cello!!["A"]!![0]
        assertEquals("Cello A open should be A3", "A3", aOpen.noteName)
        assertEquals("Cello A open frequency", 220.00, aOpen.frequency, 0.01)
    }

    // ═══════════════════════════════════════════════════════════════
    // MI-001(c): Bass open-string frequencies are correct (E/A/D/G)
    // ═══════════════════════════════════════════════════════════════

    @Test
    fun `MI-001c - DOUBLE_BASS E1 open string has 41_20 Hz`() {
        val bass = fingeringMap[Instrument.DOUBLE_BASS]
        val eOpen = bass!!["E"]!![0]
        assertEquals("Bass E open should be E1", "E1", eOpen.noteName)
        assertEquals("Bass E open frequency", 41.20, eOpen.frequency, 0.01)
    }

    @Test
    fun `MI-001c - DOUBLE_BASS A1 open string has 55_00 Hz`() {
        val bass = fingeringMap[Instrument.DOUBLE_BASS]
        val aOpen = bass!!["A"]!![0]
        assertEquals("Bass A open should be A1", "A1", aOpen.noteName)
        assertEquals("Bass A open frequency", 55.00, aOpen.frequency, 0.01)
    }

    @Test
    fun `MI-001c - DOUBLE_BASS D2 open string has 73_42 Hz`() {
        val bass = fingeringMap[Instrument.DOUBLE_BASS]
        val dOpen = bass!!["D"]!![0]
        assertEquals("Bass D open should be D2", "D2", dOpen.noteName)
        assertEquals("Bass D open frequency", 73.42, dOpen.frequency, 0.01)
    }

    @Test
    fun `MI-001c - DOUBLE_BASS G2 open string has 98_00 Hz`() {
        val bass = fingeringMap[Instrument.DOUBLE_BASS]
        val gOpen = bass!!["G"]!![0]
        assertEquals("Bass G open should be G2", "G2", gOpen.noteName)
        assertEquals("Bass G open frequency", 98.00, gOpen.frequency, 0.01)
    }

    // ═══════════════════════════════════════════════════════════════
    // VIOLA C string
    // ═══════════════════════════════════════════════════════════════

    @Test
    fun `VIOLA C3 open string has 130_81 Hz`() {
        val viola = fingeringMap[Instrument.VIOLA]
        val cOpen = viola!!["C"]!![0]
        assertEquals("Viola C open should be C3", "C3", cOpen.noteName)
        assertEquals("Viola C open frequency", 130.81, cOpen.frequency, 0.01)
    }

    @Test
    fun `VIOLA 4th finger on C string matches open G at 196 Hz`() {
        val viola = fingeringMap[Instrument.VIOLA]
        val c4th = viola!!["C"]!![5] // 4th position
        assertEquals("Viola C 4th finger note should be G3", "G3", c4th.noteName)
        assertEquals("Viola C 4th finger frequency should match open G", 196.00, c4th.frequency, 0.01)
    }
}
