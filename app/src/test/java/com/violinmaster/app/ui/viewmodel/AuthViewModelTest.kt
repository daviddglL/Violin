package com.violinmaster.app.ui.viewmodel

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.violinmaster.app.data.AnalyticsHelper
import com.violinmaster.app.data.CloudConfig
import com.violinmaster.app.data.IAnalyticsService
import com.violinmaster.app.data.ICrashReportingService
import com.violinmaster.app.data.PracticeDatabase
import com.violinmaster.app.data.IPracticeRepository
import com.violinmaster.app.data.PracticeRepository
import com.violinmaster.app.data.UserAccount
import com.violinmaster.app.data.firebase.AssignmentDoc
import com.violinmaster.app.data.firebase.AssignmentSyncRepository
import com.violinmaster.app.data.firebase.FakeFirestoreCollection
import com.violinmaster.app.data.firebase.LessonDoc
import com.violinmaster.app.data.firebase.LessonSyncRepository
import com.violinmaster.app.data.firebase.SessionDoc
import com.violinmaster.app.data.firebase.SessionSyncRepository
import com.violinmaster.app.data.firebase.UserDoc
import com.violinmaster.app.data.firebase.UserSyncRepository
import com.violinmaster.app.di.AuthManager
import com.violinmaster.app.domain.usecase.LoginUseCase
import com.violinmaster.app.domain.usecase.RegisterUseCase
import com.violinmaster.app.security.SecurityUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
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
    private lateinit var analyticsHelper: AnalyticsHelper
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
        val sessionSync = SessionSyncRepository(FakeFirestoreCollection<SessionDoc>(), database.sessionDao())
        val lessonSync = LessonSyncRepository(FakeFirestoreCollection<LessonDoc>(), database.lessonDao())
        val userSync = UserSyncRepository(FakeFirestoreCollection<UserDoc>(), database.userDao())
        val assignmentSync = AssignmentSyncRepository(FakeFirestoreCollection<AssignmentDoc>(), database.assignmentDao())
        repository = PracticeRepository(sessionSync, lessonSync, userSync, assignmentSync,
            database.sessionDao(), database.lessonDao(), database.userDao(), database.assignmentDao(), CloudConfig())
        authManager = AuthManager(context)
        securityUtils = SecurityUtils(context)
        analyticsHelper = AnalyticsHelper(
            analyticsService = FakeAnalyticsService(),
            crashReportingService = FakeCrashReportingService()
        )
        loginUseCase = LoginUseCase(repository, authManager)
        registerUseCase = RegisterUseCase(repository)
        createViewModel()
    }

    private fun createViewModel() {
        Dispatchers.setMain(Dispatchers.Unconfined)
        viewModel = AuthViewModel(repository, authManager, securityUtils, analyticsHelper, loginUseCase, registerUseCase)
    }

    /** Creates a ViewModel without touching the test-wide [viewModel] field. Used by [runTest]-based tests. */
    private fun createTestViewModel() = AuthViewModel(
        repository, authManager, securityUtils, analyticsHelper, loginUseCase, registerUseCase
    )

    @After
    fun tearDown() {
        database.close()
        Dispatchers.resetMain()
    }

    @Test
    fun `login with valid credentials sets currentUser`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        val vm = createTestViewModel()
        // Arrange: register a user first
        vm.register("testuser", "1234", "STUDENT", birthYear = 2000)
        advanceUntilIdle()
        // Act: login with valid credentials
        vm.login("testuser", "1234")
        advanceUntilIdle()
        // Assert
        assertNotNull(vm.currentUser.value)
        assertEquals("testuser", vm.currentUser.value?.username)
        assertNull(vm.loginError.value)
        Dispatchers.resetMain()
    }

    @Test
    fun `login with invalid credentials sets loginError`() = runBlocking {
        // Arrange: register a user first
        viewModel.register("testuser", "1234", "STUDENT", birthYear = 2000)


        // Act: login with wrong pin
        viewModel.login("testuser", "5678")


        // Assert
        assertNull(viewModel.currentUser.value)
        assertEquals("error_login_failed", viewModel.loginError.value)
    }

    @Test
    fun `register creates new user and sets signupSuccess`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        val vm = createTestViewModel()
        // Act
        vm.register("newuser", "1234", "STUDENT", birthYear = 2000)
        advanceUntilIdle()
        // Assert
        assertEquals("success_register", vm.signupSuccess.value)
        assertNull(vm.loginError.value)
        Dispatchers.resetMain()
    }

    @Test
    fun `register with existing username sets loginError`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        val vm = createTestViewModel()
        // Arrange
        vm.register("dupeuser", "1234", "STUDENT", birthYear = 2000)
        advanceUntilIdle()
        // Act: register again with same username
        vm.register("dupeuser", "5678", "STUDENT", birthYear = 2000)
        advanceUntilIdle()
        // Assert
        assertEquals("error_user_exists", vm.loginError.value)
        Dispatchers.resetMain()
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
    fun `logout clears currentUser`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        val vm = createTestViewModel()
        // Arrange: register and login
        vm.register("testuser", "1234", "STUDENT", birthYear = 2000)
        advanceUntilIdle()
        vm.login("testuser", "1234")
        advanceUntilIdle()
        assertNotNull(vm.currentUser.value)
        // Act
        vm.logout()
        // Assert
        assertNull(vm.currentUser.value)
        assertNull(vm.loginError.value)
        assertNull(vm.signupSuccess.value)
        Dispatchers.resetMain()
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
    fun `linkTeacherCode updates user teacher code for student`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        val vm = createTestViewModel()
        // Arrange: register and login as student
        vm.register("student1", "1234", "STUDENT", birthYear = 2000)
        advanceUntilIdle()
        vm.login("student1", "1234")
        advanceUntilIdle()
        assertNotNull(vm.currentUser.value)
        // Act
        vm.linkTeacherCode("TEACH-5678")
        advanceUntilIdle()
        // Assert
        assertEquals("TEACH-5678", vm.currentUser.value?.teacherCode)
        Dispatchers.resetMain()
    }

    @Test
    fun `linkTeacherCode does nothing for non-student`() = runBlocking {
        // Arrange: register and login as teacher
        viewModel.register("teacher1", "1234", "TEACHER", birthYear = 2000)

        viewModel.login("teacher1", "1234")

        val originalCode = viewModel.currentUser.value?.teacherCode

        // Act
        viewModel.linkTeacherCode("NEW-CODE")


        // Assert: code unchanged
        assertEquals(originalCode, viewModel.currentUser.value?.teacherCode)
    }

    @Test
    fun `logout clears unlocked area and resets auth state`() = runBlocking {
        // Arrange
        securityUtils.savePasscode("4321")
        viewModel.register("testuser", "1234", "STUDENT", birthYear = 2000)

        viewModel.login("testuser", "1234")

        viewModel.authenticatePasscode("4321")
        assertTrue(viewModel.isUserAuthenticated.value)

        // Act
        viewModel.logoutUnlockedArea()

        // Assert
        assertFalse(viewModel.isUserAuthenticated.value)
        assertNull(viewModel.securityErrorString.value)
    }

    // ── Test doubles for AnalyticsHelper dependencies ──────────────────

    private class FakeAnalyticsService : IAnalyticsService {
        override fun logEvent(name: String, params: Map<String, Any>) {}
        override fun setUserProperty(key: String, value: String) {}
        override fun setUserId(id: String) {}
        override fun setCurrentScreen(screenName: String, screenClass: String) {}
    }

    private class FakeCrashReportingService : ICrashReportingService {
        override fun log(message: String) {}
        override fun recordException(throwable: Throwable) {}
        override fun setCustomKey(key: String, value: String) {}
    }
}
