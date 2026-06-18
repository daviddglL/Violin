package com.violinmaster.app.domain.usecase

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.violinmaster.app.data.UserAccount
import com.violinmaster.app.di.AuthManager
import com.violinmaster.app.di.UserPreferencesManager
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class DataDeletionUseCaseTest {

    private lateinit var context: Context
    private lateinit var authManager: AuthManager
    private lateinit var prefsManager: UserPreferencesManager

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        authManager = AuthManager(context)
        prefsManager = UserPreferencesManager(context)
    }

    @After
    fun tearDown() {
        authManager.clearSession()
    }

    @Test
    fun `DeletionResult success has all stores OK`() {
        val statuses = mapOf(
            "firestore" to Result.success(Unit),
            "room" to Result.success(Unit),
            "prefs" to Result.success(Unit),
            "auth" to Result.success(Unit)
        )
        val result = DeletionResult(storeStatuses = statuses, authDeleted = true)

        assertTrue(result.isComplete)
        assertTrue(result.failedStores.isEmpty())
    }

    @Test
    fun `DeletionResult partial failure reports failed stores`() {
        val statuses = mapOf(
            "firestore" to Result.success(Unit),
            "room" to Result.failure(RuntimeException("Room error")),
            "prefs" to Result.success(Unit),
            "auth" to Result.success(Unit)
        )
        val result = DeletionResult(storeStatuses = statuses, authDeleted = true)

        assertFalse(result.isComplete)
        assertEquals(listOf("room"), result.failedStores)
    }

    @Test
    fun `DeletionResult with auth not deleted reports incomplete`() {
        val statuses = mapOf(
            "firestore" to Result.success(Unit),
            "room" to Result.success(Unit),
            "prefs" to Result.success(Unit),
            "auth" to Result.success(Unit)
        )
        val result = DeletionResult(storeStatuses = statuses, authDeleted = false)

        assertFalse(result.isComplete)
    }

    @Test
    fun `use case constructs without crash`() = runTest {
        val useCase = DataDeletionUseCase(
            authManager = authManager,
            userPrefsManager = prefsManager
        )
        assertNotNull(useCase)
    }

    @Test
    fun `authManager clearAllData clears saved user`() = runTest {
        val user = UserAccount(username = "test", role = "STUDENT", hashedPassword = "h", salt = "s")
        authManager.saveCurrentUser(user)
        assertNotNull(authManager.currentUser.value)

        authManager.clearAllData()
        assertEquals(null, authManager.currentUser.value)
    }

    @Test
    fun `prefsManager clearAll resets to defaults`() = runTest {
        prefsManager.setAppLanguage(com.violinmaster.app.ui.theme.AppLanguage.SPANISH)
        assertEquals(com.violinmaster.app.ui.theme.AppLanguage.SPANISH, prefsManager.appLanguage.value)

        prefsManager.clearAll()
        assertEquals(com.violinmaster.app.ui.theme.AppLanguage.ENGLISH, prefsManager.appLanguage.value)
    }
}
