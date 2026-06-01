package com.violinmaster.app.domain.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Tests for DateUtils pure utility functions.
 *
 * REQ-ARCH-005: No android.* imports. Uses only java.time.*.
 */
class DateUtilsTest {

    @Test
    fun `todayDateString returns current date in yyyy-MM-dd format`() {
        val result = DateUtils.todayDateString()
        val expected = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)

        assertEquals(expected, result)
        // Verify format matches YYYY-MM-DD pattern
        assertTrue(result.matches(Regex("\\d{4}-\\d{2}-\\d{2}")))
    }

    @Test
    fun `formatDuration returns mm ss for under one hour`() {
        assertEquals("00:30", DateUtils.formatDuration(30))
        assertEquals("05:00", DateUtils.formatDuration(300))
        assertEquals("15:45", DateUtils.formatDuration(945))
        assertEquals("59:59", DateUtils.formatDuration(3599))
    }

    @Test
    fun `formatDuration returns h mm ss for one hour or more`() {
        assertEquals("1h 00:00", DateUtils.formatDuration(3600))
        assertEquals("2h 30:00", DateUtils.formatDuration(9000))
        assertEquals("10h 05:30", DateUtils.formatDuration(36330))
    }

    @Test
    fun `formatDuration handles zero seconds`() {
        assertEquals("00:00", DateUtils.formatDuration(0))
    }

    @Test
    fun `formatDuration handles negative gracefully`() {
        assertEquals("00:00", DateUtils.formatDuration(-5))
    }
}
