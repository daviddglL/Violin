package com.violinmaster.app.domain.util

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Tests for PointsCalculator pure utility functions.
 *
 * REQ-ARCH-005: Pure Kotlin, no Android imports.
 */
class PointsCalculatorTest {

    @Test
    fun `calculatePoints returns 100 for first attempt`() {
        assertEquals(100, PointsCalculator.calculatePoints(1, "Beginner"))
    }

    @Test
    fun `calculatePoints decreases with more attempts`() {
        assertEquals(75, PointsCalculator.calculatePoints(2, "Beginner"))
        assertEquals(50, PointsCalculator.calculatePoints(3, "Beginner"))
        assertEquals(25, PointsCalculator.calculatePoints(4, "Beginner"))
    }

    @Test
    fun `calculatePoints returns minimum 10 for many attempts`() {
        assertEquals(10, PointsCalculator.calculatePoints(5, "Beginner"))
        assertEquals(10, PointsCalculator.calculatePoints(10, "Beginner"))
        assertEquals(15, PointsCalculator.calculatePoints(100, "Intermediate")) // 10 * 1.5 = 15
        assertEquals(20, PointsCalculator.calculatePoints(99, "Advanced"))      // 10 * 2.0 = 20
    }

    @Test
    fun `calculatePoints applies skill level multiplier`() {
        // Beginner: base * 1.0
        assertEquals(100, PointsCalculator.calculatePoints(1, "Beginner"))
        // Intermediate: base * 1.5 (rounded to int)
        assertEquals(150, PointsCalculator.calculatePoints(1, "Intermediate"))
        // Advanced: base * 2.0
        assertEquals(200, PointsCalculator.calculatePoints(1, "Advanced"))
    }

    @Test
    fun `calculatePoints with Intermediate skill decaying`() {
        assertEquals(150, PointsCalculator.calculatePoints(1, "Intermediate")) // 100 * 1.5
        assertEquals(112, PointsCalculator.calculatePoints(2, "Intermediate")) // 75 * 1.5 = 112.5 → 112
        assertEquals(75, PointsCalculator.calculatePoints(3, "Intermediate"))  // 50 * 1.5 = 75
    }

    @Test
    fun `calculatePoints unknown skill level treated as Beginner`() {
        assertEquals(100, PointsCalculator.calculatePoints(1, "Expert"))
        assertEquals(50, PointsCalculator.calculatePoints(3, ""))
    }
}
