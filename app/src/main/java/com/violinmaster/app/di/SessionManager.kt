package com.violinmaster.app.di

import android.content.Context
import com.violinmaster.app.data.UserAccount
import com.violinmaster.app.ui.theme.AppLanguage
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Facade over the decomposed managers: [AuthManager], [UserPreferencesManager],
 * and [NavigationManager].
 *
 * Maintains backward compatibility — all existing call sites continue to
 * work without changes. New code should inject the specific manager directly.
 *
 * @param authManager Authentication state (currentUser, isGoogleSignedIn, session persistence).
 * @param userPreferencesManager User preferences (appLanguage, daily tasks).
 * @param navigationManager UI navigation state (currentTab, currentOverlay).
 */
@Singleton
class SessionManager @Inject constructor(
  private val authManager: AuthManager,
  private val userPreferencesManager: UserPreferencesManager,
  private val navigationManager: NavigationManager
) {

  /**
   * @deprecated Use the primary constructor with [AuthManager], [UserPreferencesManager],
   *   and [NavigationManager]. This secondary constructor creates the sub-managers
   *   internally for backward compatibility with direct instantiation (e.g., in tests).
   */
  @Suppress("DEPRECATION")
  @Deprecated(
    message = "Use AuthManager, UserPreferencesManager, NavigationManager directly",
    replaceWith = ReplaceWith("SessionManager(AuthManager(context), UserPreferencesManager(context), NavigationManager())")
  )
  constructor(@ApplicationContext context: Context) : this(
    AuthManager(context),
    UserPreferencesManager(context),
    NavigationManager()
  )

  // ── Delegated: UserPreferencesManager ────────────────────────────────

  val appLanguage: StateFlow<AppLanguage>
    get() = userPreferencesManager.appLanguage

  fun setAppLanguage(lang: AppLanguage) {
    userPreferencesManager.setAppLanguage(lang)
  }

  fun getDailyTasksCompleted(today: String): Set<String> {
    return userPreferencesManager.getDailyTasksCompleted(today)
  }

  fun saveDailyTaskCompleted(today: String, tasks: Set<String>) {
    userPreferencesManager.saveDailyTaskCompleted(today, tasks)
  }

  // ── Delegated: AuthManager ───────────────────────────────────────────

  val currentUser: StateFlow<UserAccount?>
    get() = authManager.currentUser

  val isGoogleSignedIn: StateFlow<Boolean>
    get() = authManager.isGoogleSignedIn

  /** Whether the currently logged-in user is a minor (under 18). */
  val isCurrentUserMinor: Boolean
    get() = authManager.isCurrentUserMinor

  fun saveCurrentUser(user: UserAccount) {
    authManager.saveCurrentUser(user)
  }

  fun restoreCurrentUser(user: UserAccount) {
    authManager.restoreCurrentUser(user)
  }

  fun getSavedUserId(): String? = authManager.getSavedUserId()

  fun setGoogleSignedIn(signedIn: Boolean) {
    authManager.setGoogleSignedIn(signedIn)
  }

  fun clearSession() {
    authManager.clearSession()
  }

  // ── Delegated: NavigationManager ─────────────────────────────────────

  val currentTab: StateFlow<Int>
    get() = navigationManager.currentTab

  val currentOverlay: StateFlow<String?>
    get() = navigationManager.currentOverlay

  fun selectTab(index: Int) {
    navigationManager.selectTab(index)
  }

  fun showOverlay(overlay: String?) {
    navigationManager.showOverlay(overlay)
  }
}
