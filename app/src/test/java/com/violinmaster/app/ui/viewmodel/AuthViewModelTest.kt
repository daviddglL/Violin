package com.violinmaster.app.ui.viewmodel

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.violinmaster.app.data.PracticeDatabase
import com.violinmaster.app.data.IPracticeRepository
import com.violinmaster.app.data.PracticeRepository
import com.violinmaster.app.data.UserAccount
import com.violinmaster.app.di.AuthManager
import com.violinmaster.app.domain.usecase.LoginUseCase
import com.violinmaster.app.domain.usecase.RegisterUseCase
import com.violinmaster.app.security.SecurityUtils
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
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

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AuthViewModelTest {

    private lateinit var database: PracticeDatabase
    private lateinit var repository: IPracticeRepository
    private lateinit var authManager: AuthManager
    private lateinit var securityUtils: SecurityUtils
    private lateinit var loginUseCase: LoginUseCase
    private lateinit var registerUseCase: RegisterUseCase
    private lateinit var viewModel: AuthViewModel

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        // Run Room queries inline so suspend DAO functions complete synchronously in tests
        val inlineExecutor = java.util.concurrent.Executor { r -> r.run() }
        database = Room.inMemoryDatabaseBuilder(context, PracticeDatabase::class.java)
            .setTransactionExecutor(inlineExecutor)
            .setQueryExecutor(inlineExecutor)
            .build()
        repository = PracticeRepository(database.sessionDao(), database.lessonDao(), database.userDao(), database.assignmentDao())
        authManager = AuthManager(context)
        securityUtils = SecurityUtils(context)
        loginUseCase = LoginUseCase(repository, authManager)
        registerUseCase = RegisterUseCase(repository)
        viewModel = AuthViewModel(repository, authManager, securityUtils, loginUseCase, registerUseCase)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `login with valid credentials sets currentUser`() = runTest {
        // Arrange: register a user first
        viewModel.register("testuser", "1234", "STUDENT")
        advanceUntilIdle()

        // Act: login with valid credentials
        viewModel.login("testuser", "1234")
        advanceUntilIdle()

        // Assert
        assertNotNull(viewModel.currentUser.value)
        assertEquals("testuser", viewModel.currentUser.value?.username)
        assertNull(viewModel.loginError.value)
    }

    @Test
    fun `login with invalid credentials sets loginError`() = runTest {
        // Arrange: register a user first
        viewModel.register("testuser", "1234", "STUDENT")
        advanceUntilIdle()

        // Act: login with wrong pin
        viewModel.login("testuser", "5678")
        advanceUntilIdle()

        // Assert
        assertNull(viewModel.currentUser.value)
        assertEquals("error_login_failed", viewModel.loginError.value)
    }

    @Test
    fun `register creates new user and sets signupSuccess`() = runTest {
        // Act
        viewModel.register("newuser", "1234", "STUDENT")
        advanceUntilIdle()

        // Assert
        assertEquals("success_register", viewModel.signupSuccess.value)
        assertNull(viewModel.loginError.value)
    }

    @Test
    fun `register with existing username sets loginError`() = runTest {
        // Arrange
        viewModel.register("dupeuser", "1234", "STUDENT")
        advanceUntilIdle()

        // Act: register again with same username
        viewModel.register("dupeuser", "5678", "STUDENT")
        advanceUntilIdle()

        // Assert
        assertEquals("error_user_exists", viewModel.loginError.value)
    }

    @Test
    fun `register with blank username sets loginError`() = runTest {
        // Act
        viewModel.register("", "1234", "STUDENT")

        // Assert (sync check — blank username rejects immediately)
        assertEquals("error_username_empty", viewModel.loginError.value)
    }

    @Test
    fun `register with short pin sets loginError`() = runTest {
        // Act
        viewModel.register("someuser", "12", "STUDENT")

        // Assert (sync check — invalid pin rejects immediately)
        assertEquals("error_pin_length", viewModel.loginError.value)
    }

    @Test
    fun `logout clears currentUser`() = runTest {
        // Arrange: register and login
        viewModel.register("testuser", "1234", "STUDENT")
        advanceUntilIdle()
        viewModel.login("testuser", "1234")
        advanceUntilIdle()
        assertNotNull(viewModel.currentUser.value)

        // Act
        viewModel.logout()
        advanceUntilIdle()

        // Assert
        assertNull(viewModel.currentUser.value)
        assertNull(viewModel.loginError.value)
        assertNull(viewModel.signupSuccess.value)
    }

    @Test
    fun `authenticatePasscode with correct pin returns true`() = runTest {
        // Arrange: set a passcode
        securityUtils.savePasscode("7890")
        advanceUntilIdle()

        // Act
        val result = viewModel.authenticatePasscode("7890")

        // Assert
        assertTrue(result)
        assertTrue(viewModel.isUserAuthenticated.value)
        assertNull(viewModel.securityErrorString.value)
    }

    @Test
    fun `authenticatePasscode with wrong pin returns false`() = runTest {
        // Arrange: set a passcode
        securityUtils.savePasscode("7890")
        advanceUntilIdle()

        // Act
        val result = viewModel.authenticatePasscode("1111")

        // Assert
        assertFalse(result)
        assertEquals("Access Denied: Incorrect Passcode Match", viewModel.securityErrorString.value)
    }

    @Test
    fun `setPasscodeLock sets security state`() = runTest {
        // Act
        viewModel.setPasscodeLock("5678")

        // Assert
        assertTrue(viewModel.isSecurityLocked.value)
        assertTrue(viewModel.isUserAuthenticated.value)
        assertNull(viewModel.securityErrorString.value)
    }

    @Test
    fun `removePasscodeLock with correct code removes security`() = runTest {
        // Arrange
        viewModel.setPasscodeLock("5678")

        // Act
        viewModel.removePasscodeLock("5678")

        // Assert
        assertFalse(viewModel.isSecurityLocked.value)
        assertTrue(viewModel.isUserAuthenticated.value)
    }

    @Test
    fun `removePasscodeLock with wrong code sets error`() = runTest {
        // Arrange
        viewModel.setPasscodeLock("5678")

        // Act
        viewModel.removePasscodeLock("0000")

        // Assert
        assertTrue(viewModel.isSecurityLocked.value)
        assertEquals("Incorrect passcode. Security preservation active.", viewModel.securityErrorString.value)
    }

    @Test
    fun `clearAuthMessages clears error and success`() = runTest {
        // Arrange: trigger an error
        viewModel.register("", "1234", "STUDENT")
        assertEquals("error_username_empty", viewModel.loginError.value)

        // Act
        viewModel.clearAuthMessages()

        // Assert
        assertNull(viewModel.loginError.value)
        assertNull(viewModel.signupSuccess.value)
    }

    @Test
    fun `lockSessionPromptly locks when passcode is set`() = runTest {
        // Arrange: set a passcode and authenticate
        viewModel.setPasscodeLock("1234")
        assertTrue(viewModel.isUserAuthenticated.value)

        // Act
        viewModel.lockSessionPromptly()

        // Assert
        assertFalse(viewModel.isUserAuthenticated.value)
    }

    @Test
    fun `lockSessionPromptly does nothing when no passcode`() = runTest {
        // Arrange: no passcode set
        securityUtils.clearPasscode()
        val initialAuth = viewModel.isUserAuthenticated.value

        // Act
        viewModel.lockSessionPromptly()

        // Assert: state unchanged
        assertEquals(initialAuth, viewModel.isUserAuthenticated.value)
    }

    @Test
    fun `linkTeacherCode updates user teacher code for student`() = runTest {
        // Arrange: register and login as student
        viewModel.register("student1", "1234", "STUDENT")
        advanceUntilIdle()
        viewModel.login("student1", "1234")
        advanceUntilIdle()
        assertNotNull(viewModel.currentUser.value)

        // Act
        viewModel.linkTeacherCode("TEACH-5678")
        advanceUntilIdle()

        // Assert
        assertEquals("TEACH-5678", viewModel.currentUser.value?.teacherCode)
    }

    @Test
    fun `linkTeacherCode does nothing for non-student`() = runTest {
        // Arrange: register and login as teacher
        viewModel.register("teacher1", "1234", "TEACHER")
        advanceUntilIdle()
        viewModel.login("teacher1", "1234")
        advanceUntilIdle()
        val originalCode = viewModel.currentUser.value?.teacherCode

        // Act
        viewModel.linkTeacherCode("NEW-CODE")
        advanceUntilIdle()

        // Assert: code unchanged
        assertEquals(originalCode, viewModel.currentUser.value?.teacherCode)
    }

    @Test
    fun `authManager isGoogleSignedIn defaults to false`() {
        // AuthManager.isGoogleSignedIn should default to false
        // (no Google sign-in state persisted in SharedPreferences)
        assertEquals(false, authManager.isGoogleSignedIn.value)
        // Companion key for SharedPreferences should NOT be null (access check)
        assertNotNull(authManager.getSavedUserId())
    }

    @Test
    fun `authManager setGoogleSignedIn updates state`() = runTest {
        // Initially false
        assertEquals(false, authManager.isGoogleSignedIn.value)

        // Act: set Google signed in
        authManager.setGoogleSignedIn(true)
        advanceUntilIdle()

        // Assert: state updated
        assertEquals(true, authManager.isGoogleSignedIn.value)

        // Act: set Google signed out
        authManager.setGoogleSignedIn(false)
        advanceUntilIdle()

        // Assert: state reverted
        assertEquals(false, authManager.isGoogleSignedIn.value)
    }

    @Test
    fun `logout clears unlocked area and resets auth state`() = runTest {
        // Arrange
        securityUtils.savePasscode("4321")
        viewModel.register("testuser", "1234", "STUDENT")
        advanceUntilIdle()
        viewModel.login("testuser", "1234")
        advanceUntilIdle()
        viewModel.authenticatePasscode("4321")
        assertTrue(viewModel.isUserAuthenticated.value)

        // Act
        viewModel.logoutUnlockedArea()

        // Assert
        assertFalse(viewModel.isUserAuthenticated.value)
        assertNull(viewModel.securityErrorString.value)
    }
}
