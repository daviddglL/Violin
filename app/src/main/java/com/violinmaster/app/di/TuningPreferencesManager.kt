package com.violinmaster.app.di

import android.content.Context
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.violinmaster.app.domain.model.TuningConfiguration
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages per-user tuning presets via SharedPreferences.
 *
 * Each user has an independent list of [TuningConfiguration] presets
 * stored as a JSON array under the key `tuner_configs_{username}`.
 * When no user is logged in, operations are no-ops and an empty list
 * is emitted.
 *
 * Backed by the same SharedPreferences file as [UserPreferencesManager]
 * and [AuthManager] (`app_settings`) per project convention.
 */
@Singleton
class TuningPreferencesManager @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val authManager: AuthManager
) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val moshi = Moshi.Builder().build()

    private val listType = Types.newParameterizedType(
        List::class.java, TuningConfiguration::class.java
    )
    private val listAdapter = moshi.adapter<List<TuningConfiguration>>(listType)

    private val _presets = MutableStateFlow<List<TuningConfiguration>>(loadFromPrefs())
    val presets: StateFlow<List<TuningConfiguration>> = _presets.asStateFlow()

    // ── Public API ────────────────────────────────────────────────────

    /**
     * Save a [TuningConfiguration] preset for the current user.
     *
     * If a preset with the same [label] already exists, it is overwritten.
     * If no user is logged in, this is a no-op.
     */
    fun saveConfig(config: TuningConfiguration) {
        val username = currentUsername() ?: return
        val current = loadFromPrefs().toMutableList()
        val existingIndex = current.indexOfFirst { it.label == config.label }
        if (existingIndex >= 0) {
            current[existingIndex] = config
        } else {
            current.add(config)
        }
        persist(username, current)
        _presets.value = current
    }

    /**
     * Delete the preset with the given [label] for the current user.
     *
     * If the label does not exist or no user is logged in, this is a no-op.
     */
    fun deleteConfig(label: String) {
        val username = currentUsername() ?: return
        val current = loadFromPrefs().toMutableList()
        current.removeAll { it.label == label }
        persist(username, current)
        _presets.value = current
    }

    /**
     * Load a single preset by [label] for the current user.
     *
     * @return The matching [TuningConfiguration], or null if not found
     *         or no user is logged in.
     */
    fun loadConfig(label: String): TuningConfiguration? {
        val username = currentUsername() ?: return null
        val current = loadFromPrefs()
        return current.firstOrNull { it.label == label }
    }

    // ── Internal ──────────────────────────────────────────────────────

    private fun currentUsername(): String? {
        return authManager.currentUser.value?.username
    }

    private fun prefKey(username: String): String = "${KEY_PREFIX}$username"

    private fun loadFromPrefs(): List<TuningConfiguration> {
        val username = currentUsername() ?: return emptyList()
        val json = prefs.getString(prefKey(username), null) ?: return emptyList()
        return try {
            listAdapter.fromJson(json) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun persist(username: String, configs: List<TuningConfiguration>) {
        val json = listAdapter.toJson(configs)
        prefs.edit().putString(prefKey(username), json).apply()
    }

    companion object {
        private const val PREFS_NAME = "app_settings"
        private const val KEY_PREFIX = "tuner_configs_"
    }
}
