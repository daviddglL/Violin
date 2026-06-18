package com.violinmaster.app.domain.usecase

import com.google.firebase.auth.FirebaseAuth
import com.violinmaster.app.data.UserAccount
import com.violinmaster.app.di.AuthManager
import com.violinmaster.app.di.UserPreferencesManager
import kotlinx.coroutines.tasks.await

/**
 * Result of GDPR cascading data deletion with per-store status.
 *
 * Each store provides its own [Result] indicating success or failure.
 * The deletion is idempotent: repeating after full success has no side effects.
 *
 * REQ-GD-003, GD-004, GD-005: Cascading deletion with per-store reporting.
 *
 * @param storeStatuses Map of store name → Result, where stores are:
 *        "firestore", "room", "prefs", "auth".
 * @param authDeleted Whether the FirebaseAuth account was deleted.
 */
data class DeletionResult(
    val storeStatuses: Map<String, Result<Unit>>,
    val authDeleted: Boolean
) {
    val isComplete: Boolean
        get() = storeStatuses.values.all { it.isSuccess } && authDeleted

    val failedStores: List<String>
        get() = storeStatuses.filter { it.value.isFailure }.keys.toList()
}

/**
 * Executes cascading GDPR data deletion across all storage layers.
 *
 * Deletion order (per spec GD-003):
 * 1. Firestore (user doc + subcollections) — handled externally
 * 2. Room (all tables) — handled externally via [PracticeDatabase.wipeAllTables]
 * 3. SharedPreferences — [AuthManager.clearAllData] + [UserPreferencesManager.clearAll]
 * 4. FirebaseAuth — [com.google.firebase.auth.FirebaseAuth.currentUser.delete]
 *
 * This use case handles steps 3 and 4. Steps 1 and 2 are orchestrated
 * by the caller (typically [com.violinmaster.app.ui.viewmodel.AuthViewModel])
 * to enable per-store status reporting.
 *
 * REQ-GD-003, GD-004, GD-005, GD-006: GDPR cascading deletion.
 *
 * @param authManager Clears SharedPreferences and current user session.
 * @param userPrefsManager Clears user preference keys.
 */
class DataDeletionUseCase(
    private val authManager: AuthManager,
    private val userPrefsManager: UserPreferencesManager
) {
    /**
     * Clears SharedPreferences and FirebaseAuth account for the given user.
     *
     * Firestore and Room deletion are handled externally by the orchestrator
     * (e.g., AuthViewModel) so per-store statuses can be collected individually.
     *
     * @param user The user account to delete (used for reference, deletion
     *             is based on current FirebaseAuth session).
     * @return [DeletionResult] with per-store statuses for "prefs" and "auth".
     */
    @Suppress("UNUSED_PARAMETER")
    suspend operator fun invoke(user: UserAccount): DeletionResult {
        val statuses = mutableMapOf<String, Result<Unit>>()

        // Step 3: SharedPreferences
        statuses["prefs"] = try {
            authManager.clearAllData()
            userPrefsManager.clearAll()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }

        // Step 4: FirebaseAuth
        var authDeleted = false
        statuses["auth"] = try {
            val currentUser = FirebaseAuth.getInstance().currentUser
            if (currentUser != null) {
                currentUser.delete().await()
                authDeleted = true
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }

        return DeletionResult(
            storeStatuses = statuses.toMap(),
            authDeleted = authDeleted
        )
    }
}
