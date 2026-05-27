package com.violinmaster.app.di

import android.content.Context
import com.violinmaster.app.data.UserAccount
import com.violinmaster.app.ui.theme.AppLanguage
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _appLanguage = MutableStateFlow(loadSavedLanguage())
    val appLanguage: StateFlow<AppLanguage> = _appLanguage.asStateFlow()

    private val _currentUser = MutableStateFlow<UserAccount?>(null)
    val currentUser: StateFlow<UserAccount?> = _currentUser.asStateFlow()

    private val _currentTab = MutableStateFlow(0)
    val currentTab: StateFlow<Int> = _currentTab.asStateFlow()

    private val _currentOverlay = MutableStateFlow<String?>(null)
    val currentOverlay: StateFlow<String?> = _currentOverlay.asStateFlow()

    private fun loadSavedLanguage(): AppLanguage {
        val savedLangStr = prefs.getString(KEY_APP_LANG, AppLanguage.ENGLISH.name)
        return try {
            AppLanguage.valueOf(savedLangStr ?: "ENGLISH")
        } catch (e: Exception) {
            AppLanguage.ENGLISH
        }
    }

    fun setAppLanguage(lang: AppLanguage) {
        _appLanguage.value = lang
        prefs.edit().putString(KEY_APP_LANG, lang.name).apply()
    }

    fun saveCurrentUser(user: UserAccount) {
        _currentUser.value = user
        prefs.edit().putString(KEY_CURRENT_USER_ID, user.username).apply()
    }

    fun restoreCurrentUser(user: UserAccount) {
        _currentUser.value = user
    }

    fun getSavedUserId(): String? = prefs.getString(KEY_CURRENT_USER_ID, null)

    fun clearSession() {
        _currentUser.value = null
        prefs.edit().remove(KEY_CURRENT_USER_ID).apply()
    }

    fun selectTab(index: Int) {
        _currentTab.value = index
    }

    fun showOverlay(overlay: String?) {
        _currentOverlay.value = overlay
    }

    fun getDailyTasksCompleted(today: String): Set<String> {
        return prefs.getStringSet("daily_completed_$today", emptySet()) ?: emptySet()
    }

    fun saveDailyTaskCompleted(today: String, tasks: Set<String>) {
        prefs.edit().putStringSet("daily_completed_$today", tasks).apply()
    }

    companion object {
        private const val PREFS_NAME = "app_settings"
        private const val KEY_APP_LANG = "app_lang"
        private const val KEY_CURRENT_USER_ID = "current_user_id"
    }
}
