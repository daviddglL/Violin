package com.violinmaster.app.di

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.violinmaster.app.data.UserAccount
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * TDD tests for AuthManager.
 *
 * Manages authentication state: current user, Google sign-in status,
 * and session persistence. Backed by SharedPreferences.
 *
 * RED phase: AuthManager.kt does not exist yet.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AuthManagerTest {

  private lateinit var context: Context
  private lateinit var authManager: AuthManager

  private val testUser = UserAccount(
    username = "testuser",
    role = "STUDENT",
    hashedPassword = "hash123",
    salt = "salt456",
    teacherCode = "TCH-001",
    points = 100,
    skillLevel = "Beginner",
    birthYear = 2010
  )

  @Before
  fun setup() {
    context = ApplicationProvider.getApplicationContext<Context>()
    context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
      .edit().clear().commit()
    authManager = AuthManager(context)
  }

  @After
  fun tearDown() {
    context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
      .edit().clear().commit()
  }

  // ═══════════════════════════════════════════════════════════════════
  // currentUser tests
  // ═══════════════════════════════════════════════════════════════════

  @Test
  fun `currentUser defaults to null`() = runTest {
    assertNull("Current user should be null initially", authManager.currentUser.value)
  }

  @Test
  fun `saveCurrentUser updates currentUser StateFlow and persists id`() = runTest {
    authManager.saveCurrentUser(testUser)
    val user = authManager.currentUser.value
    assertNotNull("Current user should not be null after save", user)
    assertEquals("testuser", user!!.username)
    assertEquals("STUDENT", user.role)

    // Verify persistence via getSavedUserId
    assertEquals("Saved user id should persist", "testuser", authManager.getSavedUserId())
  }

  @Test
  fun `restoreCurrentUser sets the user in StateFlow without persisting`() = runTest {
    authManager.restoreCurrentUser(testUser)
    val user = authManager.currentUser.value
    assertNotNull("Current user should be restored", user)
    assertEquals("testuser", user!!.username)

    // restoreCurrentUser does NOT persist to prefs
    assertNull("restore should not persist user id", authManager.getSavedUserId())
  }

  @Test
  fun `getSavedUserId returns null when no user saved`() = runTest {
    assertNull("Saved user id should be null initially", authManager.getSavedUserId())
  }

  @Test
  fun `clearSession sets currentUser to null and removes persisted id`() = runTest {
    authManager.saveCurrentUser(testUser)
    assertEquals("testuser", authManager.getSavedUserId())

    authManager.clearSession()
    assertNull("Current user should be null after clear", authManager.currentUser.value)
    assertNull("Saved user id should be null after clear", authManager.getSavedUserId())
  }

  // ═══════════════════════════════════════════════════════════════════
  // isGoogleSignedIn tests
  // ═══════════════════════════════════════════════════════════════════

  @Test
  fun `isGoogleSignedIn defaults to false`() = runTest {
    assertFalse("isGoogleSignedIn should default to false", authManager.isGoogleSignedIn.value)
  }

  @Test
  fun `setGoogleSignedIn updates state and persists`() = runTest {
    authManager.setGoogleSignedIn(true)
    assertTrue("isGoogleSignedIn should be true", authManager.isGoogleSignedIn.value)

    // Verify persistence across instances
    val secondManager = AuthManager(context)
    assertTrue("isGoogleSignedIn should persist", secondManager.isGoogleSignedIn.value)
  }

  @Test
  fun `setGoogleSignedIn false clears the flag`() = runTest {
    authManager.setGoogleSignedIn(true)
    assertTrue(authManager.isGoogleSignedIn.value)

    authManager.setGoogleSignedIn(false)
    assertFalse("isGoogleSignedIn should be false after clearing", authManager.isGoogleSignedIn.value)
  }

  // ═══════════════════════════════════════════════════════════════════
  // isCurrentUserMinor tests
  // ═══════════════════════════════════════════════════════════════════

  @Test
  fun `isCurrentUserMinor returns false when no user`() = runTest {
    assertFalse("isCurrentUserMinor should be false with no user", authManager.isCurrentUserMinor)
  }

  @Test
  fun `isCurrentUserMinor returns true for user under 18`() = runTest {
    // birthYear 2010, current year ~2026 = 16 years old
    authManager.restoreCurrentUser(testUser)
    assertTrue("User born in 2010 should be a minor", authManager.isCurrentUserMinor)
  }

  @Test
  fun `isCurrentUserMinor returns false for adult user`() = runTest {
    val adult = testUser.copy(birthYear = 1990)
    authManager.restoreCurrentUser(adult)
    assertFalse("User born in 1990 should not be a minor", authManager.isCurrentUserMinor)
  }

  @Test
  fun `isCurrentUserMinor returns false for birthYear zero`() = runTest {
    val legacy = testUser.copy(birthYear = 0)
    authManager.restoreCurrentUser(legacy)
    assertFalse("User with birthYear 0 should not be a minor", authManager.isCurrentUserMinor)
  }
}
