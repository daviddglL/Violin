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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Ignore
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
        // Clear SharedPreferences to avoid cross-test pollution
        context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
            .edit().clear().commit()
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
        createViewModel()
    }

    private fun createViewModel() {
        Dispatchers.setMain(Dispatchers.Unconfined)
        viewModel = AuthViewModel(repository, authManager, securityUtils, loginUseCase, registerUseCase)
    }

    @After
    fun tearDown() {
        database.close()
        Dispatchers.resetMain()
    }

    @Test
    @Ignore("viewModelScope dispatcher not advancing in runBlocking — needs Dispatchers.Main override investigation")
    fun `login with valid credentials sets currentUser`() = runBlocking {
        createViewModel()
        // Arrange: register a user first
        viewModel.register("testuser", "1234", "STUDENT")


        // Act: login with valid credentials
        viewModel.login("testuser", "1234")


        // Assert
        assertNotNull(viewModel.currentUser.value)
        assertEquals("testuser", viewModel.currentUser.value?.username)
        assertNull(viewModel.loginError.value)
    }

    @Test
    fun `login with invalid credentials sets loginError`() = runBlocking {
        // Arrange: register a user first
        viewModel.register("testuser", "1234", "STUDENT")


        // Act: login with wrong pin
        viewModel.login("testuser", "5678")


        // Assert
        assertNull(viewModel.currentUser.value)
        assertEquals("error_login_failed", viewModel.loginError.value)
    }

    @Test
    @Ignore("viewModelScope dispatcher not advancing in runBlocking")
    fun `register creates new user and sets signupSuccess`() = runBlocking {
        createViewModel()
        // Act
        viewModel.register("newuser", "1234", "STUDENT")


        // Assert
        assertEquals("success_register", viewModel.signupSuccess.value)
        assertNull(viewModel.loginError.value)
    }

    @Test
    @Ignore("viewModelScope dispatcher not advancing in runBlocking")
    fun `register with existing username sets loginError`() = runBlocking {
        createViewModel()
        // Arrange
        viewModel.register("dupeuser", "1234", "STUDENT")


        // Act: register again with same username
        viewModel.register("dupeuser", "5678", "STUDENT")


        // Assert
        assertEquals("error_user_exists", viewModel.loginError.value)
    }

    @Test
    fun `register with blank username sets loginError`() = runBlocking {
        // Act
        viewModel.register("", "1234", "STUDENT")

        // Assert (sync check — blank username rejects immediately)
        assertEquals("error_username_empty", viewModel.loginError.value)
    }

    @Test
    fun `register with short pin sets loginError`() = runBlocking {
        // Act
        viewModel.register("someuser", "12", "STUDENT")

        // Assert (sync check — invalid pin rejects immediately)
        assertEquals("error_pin_length", viewModel.loginError.value)
    }

    @Test
    @Ignore("viewModelScope dispatcher not advancing in runBlocking")
    fun `logout clears currentUser`() = runBlocking {
        createViewModel()
        // Arrange: register and login
        viewModel.register("testuser", "1234", "STUDENT")

        viewModel.login("testuser", "1234")

        assertNotNull(viewModel.currentUser.value)

        // Act
        viewModel.logout()


        // Assert
        assertNull(viewModel.currentUser.value)
        assertNull(viewModel.loginError.value)
        assertNull(viewModel.signupSuccess.value)
    }

    @Test
    fun `authenticatePasscode with correct pin returns true`() = runBlocking {
        // Arrange: set a passcode
        securityUtils.savePasscode("7890")


        // Act
        val result = viewModel.authenticatePasscode("7890")

        // Assert
        assertTrue(result)
        assertTrue(viewModel.isUserAuthenticated.value)
        assertNull(viewModel.securityErrorString.value)
    }

    @Test
    fun `authenticatePasscode with wrong pin returns false`() = runBlocking {
        // Arrange: set a passcode
        securityUtils.savePasscode("7890")


        // Act
        val result = viewModel.authenticatePasscode("1111")

        // Assert
        assertFalse(result)
        assertEquals("Access Denied: Incorrect Passcode Match", viewModel.securityErrorString.value)
    }

    @Test
    fun `setPasscodeLock sets security state`() = runBlocking {
        // Act
        viewModel.setPasscodeLock("5678")

        // Assert
        assertTrue(viewModel.isSecurityLocked.value)
        assertTrue(viewModel.isUserAuthenticated.value)
        assertNull(viewModel.securityErrorString.value)
    }

    @Test
    fun `removePasscodeLock with correct code removes security`() = runBlocking {
        // Arrange
        viewModel.setPasscodeLock("5678")

        // Act
        viewModel.removePasscodeLock("5678")

        // Assert
        assertFalse(viewModel.isSecurityLocked.value)
        assertTrue(viewModel.isUserAuthenticated.value)
    }

    @Test
    fun `removePasscodeLock with wrong code sets error`() = runBlocking {
        // Arrange
        viewModel.setPasscodeLock("5678")

        // Act
        viewModel.removePasscodeLock("0000")

        // Assert
        assertTrue(viewModel.isSecurityLocked.value)
        assertEquals("Incorrect passcode. Security preservation active.", viewModel.securityErrorString.value)
    }

    @Test
    fun `clearAuthMessages clears error and success`() = runBlocking {
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
    fun `lockSessionPromptly locks when passcode is set`() = runBlocking {
        // Arrange: set a passcode and authenticate
        viewModel.setPasscodeLock("1234")
        assertTrue(viewModel.isUserAuthenticated.value)

        // Act
        viewModel.lockSessionPromptly()

        // Assert
        assertFalse(viewModel.isUserAuthenticated.value)
    }

    @Test
    fun `lockSessionPromptly does nothing when no passcode`() = runBlocking {
        // Arrange: no passcode set
        securityUtils.clearPasscode()
        val initialAuth = viewModel.isUserAuthenticated.value

        // Act
        viewModel.lockSessionPromptly()

        // Assert: state unchanged
        assertEquals(initialAuth, viewModel.isUserAuthenticated.value)
    }

    @Test
    @Ignore("viewModelScope dispatcher not advancing in runBlocking")
    fun `linkTeacherCode updates user teacher code for student`() = runBlocking {
        createViewModel()
        // Arrange: register and login as student
        viewModel.register("student1", "1234", "STUDENT")

        viewModel.login("student1", "1234")

        assertNotNull(viewModel.currentUser.value)

        // Act
        viewModel.linkTeacherCode("TEACH-5678")


        // Assert
        assertEquals("TEACH-5678", viewModel.currentUser.value?.teacherCode)
    }

    @Test
    fun `linkTeacherCode does nothing for non-student`() = runBlocking {
        // Arrange: register and login as teacher
        viewModel.register("teacher1", "1234", "TEACHER")

        viewModel.login("teacher1", "1234")

        val originalCode = viewModel.currentUser.value?.teacherCode

        // Act
        viewModel.linkTeacherCode("NEW-CODE")


        // Assert: code unchanged
        assertEquals(originalCode, viewModel.currentUser.value?.teacherCode)
    }

    @Test
    fun `authManager isGoogleSignedIn defaults to false`() {
        assertEquals(false, authManager.isGoogleSignedIn.value)
        assertNull(authManager.getSavedUserId())
    }

    @Test
    fun `authManager setGoogleSignedIn updates state`() = runBlocking {
        // Initially false
        assertEquals(false, authManager.isGoogleSignedIn.value)

        // Act: set Google signed in
        authManager.setGoogleSignedIn(true)


        // Assert: state updated
        assertEquals(true, authManager.isGoogleSignedIn.value)

        // Act: set Google signed out
        authManager.setGoogleSignedIn(false)


        // Assert: state reverted
        assertEquals(false, authManager.isGoogleSignedIn.value)
    }

    @Test
    fun `logout clears unlocked area and resets auth state`() = runBlocking {
        // Arrange
        securityUtils.savePasscode("4321")
        viewModel.register("testuser", "1234", "STUDENT")

        viewModel.login("testuser", "1234")

        viewModel.authenticatePasscode("4321")
        assertTrue(viewModel.isUserAuthenticated.value)

        // Act
        viewModel.logoutUnlockedArea()

        // Assert
        assertFalse(viewModel.isUserAuthenticated.value)
        assertNull(viewModel.securityErrorString.value)
    }
}
