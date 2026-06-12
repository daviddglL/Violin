package com.violinmaster.app.ui.screens

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.violinmaster.app.data.CloudConfig
import com.violinmaster.app.data.IPracticeRepository
import com.violinmaster.app.data.PracticeDatabase
import com.violinmaster.app.data.PracticeRepository
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
import com.violinmaster.app.ui.viewmodel.AuthViewModel
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class MasterclassTabTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var database: PracticeDatabase
    private lateinit var repository: IPracticeRepository
    private lateinit var authManager: AuthManager
    private lateinit var securityUtils: SecurityUtils
    private lateinit var loginUseCase: LoginUseCase
    private lateinit var registerUseCase: RegisterUseCase

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
            .edit().clear().commit()
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
        loginUseCase = LoginUseCase(repository, authManager)
        registerUseCase = RegisterUseCase(repository)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `VP-002a - no passcode set shows unlocked masterclass hub directly`() {
        // Arrange: clear any existing passcode
        securityUtils.clearPasscode()
        val viewModel = AuthViewModel(repository, authManager, securityUtils, loginUseCase, registerUseCase)

        composeTestRule.setContent {
            MasterclassTab(
                authViewModel = viewModel
            )
        }

        // VP-002(a): Without passcode, content hub renders directly (not passcode setup screen)
        // The hub shows the masterclass video titles
        composeTestRule.onNodeWithTag("masterclass_content_hub").assertIsDisplayed()
    }

    @Test
    fun `VP-002b - passcode set shows authentication lock screen`() {
        // Arrange: set a passcode, but don't authenticate
        val viewModel = AuthViewModel(repository, authManager, securityUtils, loginUseCase, registerUseCase)
        viewModel.setPasscodeLock("1234")
        // Lock the session so isUserAuthenticated becomes false
        viewModel.lockSessionPromptly()

        composeTestRule.setContent {
            MasterclassTab(
                authViewModel = viewModel
            )
        }

        // VP-002(b): With passcode set and not authenticated, lock screen renders
        // The SecureAuthenticationLockScreen contains keypad buttons with test tags
        composeTestRule.onNodeWithTag("keypad_1").assertIsDisplayed()
    }
}
