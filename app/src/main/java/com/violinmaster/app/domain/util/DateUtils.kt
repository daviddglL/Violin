package com.violinmaster.app.domain.util

import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Pure utility for date formatting using java.time.*.
 *
 * No Android imports — functions are pure and testable without Robolectric.
 *
 * REQ-ARCH-005-S1: Replace java.text.SimpleDateFormat / java.util.Calendar
 * with java.time.* equivalents.
 */
object DateUtils {

    private val dateFormatter: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    /** Returns today's date as "YYYY-MM-DD" using java.time.LocalDate. */
    fun todayDateString(): String = LocalDate.now().format(dateFormatter)

    /**
     * Formats a duration in seconds as a human-readable string.
     *
     * - Under 1 hour: "MM:SS"
     * - 1 hour or more: "Hh MM:SS"
     * - Negative or zero: "00:00"
     */
    fun formatDuration(seconds: Int): String {
        if (seconds <= 0) return "00:00"
        val totalSeconds = seconds.coerceAtLeast(0)
        val hours = totalSeconds / 3600
        val remainingSeconds = totalSeconds % 3600
        val minutes = remainingSeconds / 60
        val secs = remainingSeconds % 60

        val mmss = "%02d:%02d".format(minutes, secs)
        return if (hours > 0) "${hours}h $mmss" else mmss
    }
}
