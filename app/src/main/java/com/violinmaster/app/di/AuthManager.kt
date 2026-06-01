package com.violinmaster.app.di

import android.content.Context
import com.violinmaster.app.data.UserAccount
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages authentication state: current user, Google sign-in status,
 * and session persistence.
 *
 * Backed by SharedPreferences for session persistence.
 * Separated from SessionManager per Single Responsibility Principle.
 */
@Singleton
class AuthManager @Inject constructor(
  @param:ApplicationContext private val context: Context
) {
  private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

  private val _currentUser = MutableStateFlow<UserAccount?>(null)
  val currentUser: StateFlow<UserAccount?> = _currentUser.asStateFlow()

  private val _isGoogleSignedIn = MutableStateFlow(
    prefs.getBoolean(KEY_GOOGLE_SIGNED_IN, false)
  )
  val isGoogleSignedIn: StateFlow<Boolean> = _isGoogleSignedIn.asStateFlow()

  /** Whether the currently logged-in user is a minor (under 18). */
  val isCurrentUserMinor: Boolean
    get() = _currentUser.value?.isMinor ?: false

  fun saveCurrentUser(user: UserAccount) {
    _currentUser.value = user
    prefs.edit().putString(KEY_CURRENT_USER_ID, user.username).apply()
  }

  fun restoreCurrentUser(user: UserAccount) {
    _currentUser.value = user
  }

  fun getSavedUserId(): String? = prefs.getString(KEY_CURRENT_USER_ID, null)

  fun setGoogleSignedIn(signedIn: Boolean) {
    _isGoogleSignedIn.value = signedIn
    prefs.edit().putBoolean(KEY_GOOGLE_SIGNED_IN, signedIn).apply()
  }

  fun clearSession() {
    _currentUser.value = null
    prefs.edit().remove(KEY_CURRENT_USER_ID).apply()
  }

  companion object {
    private const val PREFS_NAME = "app_settings"
    private const val KEY_CURRENT_USER_ID = "current_user_id"
    private const val KEY_GOOGLE_SIGNED_IN = "google_signed_in"
  }
}
