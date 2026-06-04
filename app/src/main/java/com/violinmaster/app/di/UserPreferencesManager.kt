package com.violinmaster.app.di

import android.content.Context
import com.violinmaster.app.domain.model.Instrument
import com.violinmaster.app.ui.theme.AppLanguage
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages user preferences: app language and daily tasks completion.
 *
 * Backed by SharedPreferences. Separated from SessionManager per
 * Single Responsibility Principle.
 */
@Singleton
class UserPreferencesManager @Inject constructor(
  @param:ApplicationContext private val context: Context
) {
  private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

  private val _appLanguage = MutableStateFlow(loadSavedLanguage())
  val appLanguage: StateFlow<AppLanguage> = _appLanguage.asStateFlow()

  private val _selectedInstrument = MutableStateFlow(loadSavedInstrument())
  val selectedInstrument: StateFlow<Instrument> = _selectedInstrument.asStateFlow()

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

  private fun loadSavedInstrument(): Instrument {
    val savedName = prefs.getString(KEY_SELECTED_INSTRUMENT, Instrument.VIOLIN.name)
    return try {
      Instrument.valueOf(savedName ?: "VIOLIN")
    } catch (e: Exception) {
      Instrument.VIOLIN
    }
  }

  fun setSelectedInstrument(instrument: Instrument) {
    _selectedInstrument.value = instrument
    prefs.edit().putString(KEY_SELECTED_INSTRUMENT, instrument.name).apply()
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
    private const val KEY_SELECTED_INSTRUMENT = "selected_instrument"
  }
}
