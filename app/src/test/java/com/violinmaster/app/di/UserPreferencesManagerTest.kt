package com.violinmaster.app.di

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.violinmaster.app.domain.model.Instrument
import com.violinmaster.app.ui.theme.AppLanguage
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * TDD tests for UserPreferencesManager.
 *
 * Manages user preferences: app language and daily tasks completion.
 * Backed by SharedPreferences.
 *
 * RED phase: UserPreferencesManager.kt does not exist yet.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class UserPreferencesManagerTest {

  private lateinit var context: Context
  private lateinit var userPreferencesManager: UserPreferencesManager

  @Before
  fun setup() {
    context = ApplicationProvider.getApplicationContext<Context>()
    // Clean prefs before each test
    context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
      .edit().clear().commit()
    userPreferencesManager = UserPreferencesManager(context)
  }

  @After
  fun tearDown() {
    context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
      .edit().clear().commit()
  }

  // ═══════════════════════════════════════════════════════════════════
  // appLanguage tests
  // ═══════════════════════════════════════════════════════════════════

  @Test
  fun `appLanguage defaults to ENGLISH`() = runTest {
    assertEquals(
      "Default language should be ENGLISH",
      AppLanguage.ENGLISH,
      userPreferencesManager.appLanguage.value
    )
  }

  @Test
  fun `setAppLanguage changes language and persists`() = runTest {
    userPreferencesManager.setAppLanguage(AppLanguage.SPANISH)
    assertEquals(
      "Language should be SPANISH after setting",
      AppLanguage.SPANISH,
      userPreferencesManager.appLanguage.value
    )

    // Verify persistence by creating a new manager with same prefs
    val secondManager = UserPreferencesManager(context)
    assertEquals(
      "Language should persist across instances",
      AppLanguage.SPANISH,
      secondManager.appLanguage.value
    )
  }

  @Test
  fun `setAppLanguage changes from SPANISH to ENGLISH`() = runTest {
    userPreferencesManager.setAppLanguage(AppLanguage.SPANISH)
    assertEquals(AppLanguage.SPANISH, userPreferencesManager.appLanguage.value)

    userPreferencesManager.setAppLanguage(AppLanguage.ENGLISH)
    assertEquals(AppLanguage.ENGLISH, userPreferencesManager.appLanguage.value)
  }

  // ═══════════════════════════════════════════════════════════════════
  // selectedInstrument tests
  // ═══════════════════════════════════════════════════════════════════

  @Test
  fun `selectedInstrument defaults to VIOLIN on first launch`() = runTest {
    assertEquals(
      "Default instrument should be VIOLIN",
      Instrument.VIOLIN,
      userPreferencesManager.selectedInstrument.value
    )
  }

  @Test
  fun `setSelectedInstrument changes instrument and persists across instances`() = runTest {
    userPreferencesManager.setSelectedInstrument(Instrument.CELLO)
    assertEquals(
      "Instrument should be CELLO after setting",
      Instrument.CELLO,
      userPreferencesManager.selectedInstrument.value
    )

    // Verify persistence by creating a new manager with same prefs
    val secondManager = UserPreferencesManager(context)
    assertEquals(
      "Instrument should persist across instances",
      Instrument.CELLO,
      secondManager.selectedInstrument.value
    )
  }

  @Test
  fun `selectedInstrument StateFlow emits correctly`() = runTest {
    val emissions = mutableListOf<Instrument>()
    val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
      userPreferencesManager.selectedInstrument.collect { emissions.add(it) }
    }

    // First emission should be the default
    assertEquals(
      "First emission should be VIOLIN",
      Instrument.VIOLIN,
      emissions.first()
    )

    // Set to VIOLA — flow should emit
    userPreferencesManager.setSelectedInstrument(Instrument.VIOLA)
    assertEquals(
      "Should emit VIOLA after setSelectedInstrument",
      Instrument.VIOLA,
      emissions.last()
    )

    job.cancel()
  }

  @Test
  fun `setSelectedInstrument overwrites previous value`() = runTest {
    userPreferencesManager.setSelectedInstrument(Instrument.VIOLA)
    assertEquals(
      "Should be VIOLA after first set",
      Instrument.VIOLA,
      userPreferencesManager.selectedInstrument.value
    )

    userPreferencesManager.setSelectedInstrument(Instrument.CELLO)
    assertEquals(
      "Should be CELLO after overwrite",
      Instrument.CELLO,
      userPreferencesManager.selectedInstrument.value
    )
  }

  @Test
  fun `selectedInstrument name survives process death`() = runTest {
    // Set instrument to VIOLA
    userPreferencesManager.setSelectedInstrument(Instrument.VIOLA)

    // Simulate process death: create two new instances
    val instance1 = UserPreferencesManager(context)
    val instance2 = UserPreferencesManager(context)

    assertEquals(
      "Instance 1 should see VIOLA after simulated process death",
      Instrument.VIOLA,
      instance1.selectedInstrument.value
    )
    assertEquals(
      "Instance 2 should see VIOLA after simulated process death",
      Instrument.VIOLA,
      instance2.selectedInstrument.value
    )
  }

  // ═══════════════════════════════════════════════════════════════════
  // Daily tasks tests
  // ═══════════════════════════════════════════════════════════════════

  @Test
  fun `getDailyTasksCompleted returns empty set for new date`() = runTest {
    val tasks = userPreferencesManager.getDailyTasksCompleted("2026-06-01")
    assertTrue("Daily tasks should be empty for new date", tasks.isEmpty())
  }

  @Test
  fun `saveDailyTaskCompleted persists task set`() = runTest {
    val today = "2026-06-01"
    val tasks = setOf("task_scale", "task_bow_hold")

    userPreferencesManager.saveDailyTaskCompleted(today, tasks)
    val retrieved = userPreferencesManager.getDailyTasksCompleted(today)

    assertEquals("Should have 2 tasks", 2, retrieved.size)
    assertTrue("Should contain task_scale", retrieved.contains("task_scale"))
    assertTrue("Should contain task_bow_hold", retrieved.contains("task_bow_hold"))
  }

  @Test
  fun `saveDailyTaskCompleted overwrites previous set`() = runTest {
    val today = "2026-06-01"

    userPreferencesManager.saveDailyTaskCompleted(today, setOf("task_one"))
    var retrieved = userPreferencesManager.getDailyTasksCompleted(today)
    assertEquals("First save should have 1 task", 1, retrieved.size)
    assertTrue(retrieved.contains("task_one"))

    // Overwrite with new set
    userPreferencesManager.saveDailyTaskCompleted(today, setOf("task_two", "task_three"))
    retrieved = userPreferencesManager.getDailyTasksCompleted(today)
    assertEquals("Second save should have 2 tasks", 2, retrieved.size)
    assertFalse("Should not contain task_one anymore", retrieved.contains("task_one"))
    assertTrue("Should contain task_two", retrieved.contains("task_two"))
    assertTrue("Should contain task_three", retrieved.contains("task_three"))
  }

  @Test
  fun `daily tasks are scoped by date`() = runTest {
    val date1 = "2026-06-01"
    val date2 = "2026-06-02"

    userPreferencesManager.saveDailyTaskCompleted(date1, setOf("task_a"))
    userPreferencesManager.saveDailyTaskCompleted(date2, setOf("task_b"))

    val tasksDate1 = userPreferencesManager.getDailyTasksCompleted(date1)
    val tasksDate2 = userPreferencesManager.getDailyTasksCompleted(date2)

    assertEquals("Date1 should have 1 task", 1, tasksDate1.size)
    assertTrue("Date1 should contain task_a", tasksDate1.contains("task_a"))

    assertEquals("Date2 should have 1 task", 1, tasksDate2.size)
    assertTrue("Date2 should contain task_b", tasksDate2.contains("task_b"))
  }
}
