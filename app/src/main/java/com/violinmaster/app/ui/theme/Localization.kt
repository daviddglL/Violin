package com.violinmaster.app.ui.theme

sealed class AppLanguage(val name: String) {
  data object ENGLISH : AppLanguage("ENGLISH")
  data object SPANISH : AppLanguage("SPANISH")
  companion object {
    fun values(): Array<AppLanguage> {
      return arrayOf(ENGLISH, SPANISH)
    }

    fun valueOf(value: String): AppLanguage {
      return when (value) {
        "ENGLISH" -> ENGLISH
        "SPANISH" -> SPANISH
        else -> throw IllegalArgumentException("No object com.violinmaster.app.ui.theme.AppLanguage.$value")
      }
    }
  }
}

object Localization {

  private val en: Map<String, String> by lazy {
    StringsHome.en + StringsAuth.en + StringsSettings.en + StringsLessons.en
  }

  private val es: Map<String, String> by lazy {
    StringsHome.es + StringsAuth.es + StringsSettings.es + StringsLessons.es
  }

  fun get(key: String, language: AppLanguage): String {
    val bundle = if (language == AppLanguage.SPANISH) es else en
    return bundle[key] ?: en[key] ?: key
  }
}
