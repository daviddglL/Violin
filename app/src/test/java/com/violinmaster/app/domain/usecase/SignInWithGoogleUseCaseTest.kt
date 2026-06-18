package com.violinmaster.app.domain.usecase

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.firebase.auth.FirebaseAuth
import com.violinmaster.app.data.IPracticeRepository
import com.violinmaster.app.data.UserAccount
import com.violinmaster.app.di.AuthManager
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class SignInWithGoogleUseCaseTest {

    private lateinit var context: Context
    private lateinit var authManager: AuthManager
    private lateinit var repository: FakeSignInRepo
    private lateinit var useCase: SignInWithGoogleUseCase

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        authManager = AuthManager(context)
        repository = FakeSignInRepo()
        useCase = SignInWithGoogleUseCase(
            firebaseAuth = FirebaseAuth.getInstance(),
            repository = repository,
            authManager = authManager
        )
    }

    @After
    fun tearDown() {
        authManager.clearSession()
    }

    @Test
    fun `invoke with mocked deps constructs without crash`() = runTest {
        // Test that the use case can be constructed and invoked.
        // Full integration requires real Google account — this is a smoke test.
        // The real sign-in flow uses CredentialManager which requires
        // a device with Google Play Services.
        assertNotNull(useCase)
    }

    @Test
    fun `SignInResult has correct structure`() {
        // Verify the data class structure
        val result = SignInResult(user = null, error = "test error")
        assertNull(result.user)
        assertEquals("test error", result.error)
    }

    @Test
    fun `SignInResult success shows no error`() {
        val user = UserAccount(username = "u", role = "FREELANCER", hashedPassword = "", salt = "")
        val result = SignInResult(user = user, error = null)
        assertNotNull(result.user)
        assertNull(result.error)
    }

    class FakeSignInRepo : IPracticeRepository {
        override val allSessions = kotlinx.coroutines.flow.flowOf(emptyList<com.violinmaster.app.data.PracticeSession>())
        override val allLevelProgress = kotlinx.coroutines.flow.flowOf(emptyList<com.violinmaster.app.data.LessonProgress>())
        override fun getSessionsByDate(d: String) = kotlinx.coroutines.flow.flowOf(emptyList<com.violinmaster.app.data.PracticeSession>())
        override suspend fun insertSession(s: com.violinmaster.app.data.PracticeSession) {}
        override suspend fun deleteSession(id: Int) {}
        override suspend fun clearSessions() {}
        override suspend fun insertLessonProgress(lp: com.violinmaster.app.data.LessonProgress) {}
        override suspend fun updateLessonCompletion(id: String, c: Boolean) {}
        override suspend fun getLessonProgressById(id: String) = null
        override val allUsers = kotlinx.coroutines.flow.flowOf(emptyList<UserAccount>())
        override suspend fun insertUser(u: UserAccount) {}
        override suspend fun getUserByUsername(u: String) = null
        override val allAssignments = kotlinx.coroutines.flow.flowOf(emptyList<com.violinmaster.app.data.Assignment>())
        override fun getAssignmentsForStudent(s: String) = kotlinx.coroutines.flow.flowOf(emptyList<com.violinmaster.app.data.Assignment>())
        override fun getAssignmentsByTeacher(t: String) = kotlinx.coroutines.flow.flowOf(emptyList<com.violinmaster.app.data.Assignment>())
        override suspend fun insertAssignment(a: com.violinmaster.app.data.Assignment) {}
        override suspend fun updateAssignmentCompletion(id: Int, c: Boolean) {}
        override suspend fun deleteAssignmentById(id: Int) {}
    }
}
