package com.violinmaster.app.di

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test

/**
 * TDD tests for NavigationManager.
 *
 * NavigationManager is a pure StateFlow-based manager for tab selection
 * and overlay display. It has zero Android dependencies.
 *
 * RED phase: NavigationManager.kt does not exist yet.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class NavigationManagerTest {

  private lateinit var navigationManager: NavigationManager

  @Before
  fun setup() {
    navigationManager = NavigationManager()
  }

  // ═══════════════════════════════════════════════════════════════════
  // currentTab tests
  // ═══════════════════════════════════════════════════════════════════

  @Test
  fun `currentTab defaults to 0`() = runTest {
    assertEquals("Initial tab should be 0", 0, navigationManager.currentTab.value)
  }

  @Test
  fun `selectTab changes currentTab to the given index`() = runTest {
    navigationManager.selectTab(2)
    assertEquals("Tab should be 2 after selection", 2, navigationManager.currentTab.value)
  }

  @Test
  fun `selectTab with same index keeps value unchanged`() = runTest {
    navigationManager.selectTab(1)
    assertEquals(1, navigationManager.currentTab.value)
    navigationManager.selectTab(1)
    assertEquals("Re-selecting same tab should keep value", 1, navigationManager.currentTab.value)
  }

  @Test
  fun `selectTab handles zero index`() = runTest {
    navigationManager.selectTab(3)
    assertEquals(3, navigationManager.currentTab.value)
    navigationManager.selectTab(0)
    assertEquals("Tab should return to 0", 0, navigationManager.currentTab.value)
  }

  // ═══════════════════════════════════════════════════════════════════
  // currentOverlay tests
  // ═══════════════════════════════════════════════════════════════════

  @Test
  fun `currentOverlay defaults to null`() = runTest {
    assertNull("Initial overlay should be null", navigationManager.currentOverlay.value)
  }

  @Test
  fun `showOverlay sets overlay value`() = runTest {
    navigationManager.showOverlay("tuner")
    assertEquals("Overlay should be 'tuner'", "tuner", navigationManager.currentOverlay.value)
  }

  @Test
  fun `showOverlay with null clears overlay`() = runTest {
    navigationManager.showOverlay("metronome")
    assertEquals("Overlay should be set first", "metronome", navigationManager.currentOverlay.value)
    navigationManager.showOverlay(null)
    assertNull("Overlay should be null after clearing", navigationManager.currentOverlay.value)
  }

  @Test
  fun `showOverlay with different values transitions correctly`() = runTest {
    navigationManager.showOverlay("tuner")
    assertEquals("tuner", navigationManager.currentOverlay.value)
    navigationManager.showOverlay("metronome")
    assertEquals("metronome", navigationManager.currentOverlay.value)
    navigationManager.showOverlay("tuner")
    assertEquals("tuner", navigationManager.currentOverlay.value)
  }

  // ═══════════════════════════════════════════════════════════════
  // QA-003: targetLessonsSubTab + navigateToQuizTab
  // ═══════════════════════════════════════════════════════════════

  @Test
  fun `targetLessonsSubTab defaults to -1`() = runTest {
    assertEquals("Default sentinel should be -1", -1, navigationManager.targetLessonsSubTab.value)
  }

  @Test
  fun `navigateToQuizTab sets targetLessonsSubTab to 2`() = runTest {
    navigationManager.navigateToQuizTab()
    assertEquals("Quiz sub-tab index should be 2", 2, navigationManager.targetLessonsSubTab.value)
  }

  @Test
  fun `navigateToQuizTab also sets currentTab to 1 for Lessons tab`() = runTest {
    navigationManager.navigateToQuizTab()
    assertEquals("Should navigate to Lessons tab (index 1)", 1, navigationManager.currentTab.value)
  }

  @Test
  fun `can reset targetLessonsSubTab back to -1`() = runTest {
    navigationManager.navigateToQuizTab()
    assertEquals(2, navigationManager.targetLessonsSubTab.value)
    navigationManager.clearLessonsSubTabTarget()
    assertEquals("Cleared target should be -1", -1, navigationManager.targetLessonsSubTab.value)
  }
}
