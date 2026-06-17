package com.violinmaster.app.domain.usecase

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.violinmaster.app.data.IPracticeRepository
import com.violinmaster.app.data.Assignment
import com.violinmaster.app.data.LessonProgress
import com.violinmaster.app.data.PracticeSession
import com.violinmaster.app.data.UserAccount
import com.violinmaster.app.di.AuthManager
import com.violinmaster.app.domain.util.Base64Encoder
import com.violinmaster.app.security.SecurityUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Tests for ResetPinUseCase — REQ-PINREC-004.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ResetPinUseCaseTest {

    private lateinit var context: Context
    private lateinit var repo: FakeResetRepo
    private lateinit var authManager: AuthManager
    private lateinit var useCase: ResetPinUseCase

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        repo = FakeResetRepo()
        authManager = AuthManager(context)
        useCase = ResetPinUseCase(repo, authManager)
    }

    @After
    fun tearDown() {
        authManager.clearSession()
    }

    @Test
    fun `resets pin for existing user`() = runTest {
        // Arrange
        repo.users["testuser"] = UserAccount(
            username = "testuser", role = "STUDENT",
            hashedPassword = "old_hash", salt = "old_salt"
        )

        // Act
        val result = useCase("testuser", "9876")

        // Assert
        assertTrue(result)
        val updatedUser = repo.users["testuser"]!!
        assertNotEquals("old_hash", updatedUser.hashedPassword)
        assertNotEquals("old_salt", updatedUser.salt)
        assertTrue(updatedUser.hashedPassword.isNotEmpty())
        assertTrue(updatedUser.salt.isNotEmpty())
        // Verify new pin works
        val salt = Base64Encoder.decode(updatedUser.salt)
        val expectedHash = SecurityUtils.hashPasscode("9876".toCharArray(), salt)
        assertEquals(expectedHash, updatedUser.hashedPassword)
        // User should be auto-logged-in
        assertEquals("testuser", authManager.currentUser.value?.username)
    }

    @Test
    fun `returns false for non-existent user`() = runTest {
        val result = useCase("nobody", "1234")
        assertFalse(result)
        assertEquals(null, authManager.currentUser.value)
    }

    @Test
    fun `rejects pin shorter than 4 digits`() = runTest {
        repo.users["testuser"] = UserAccount(
            username = "testuser", role = "STUDENT",
            hashedPassword = "hash", salt = "salt"
        )

        val result = useCase("testuser", "12")

        assertFalse(result)
        assertEquals("hash", repo.users["testuser"]!!.hashedPassword)
    }

    @Test
    fun `rejects pin longer than 4 digits`() = runTest {
        repo.users["testuser"] = UserAccount(
            username = "testuser", role = "STUDENT",
            hashedPassword = "hash", salt = "salt"
        )

        val result = useCase("testuser", "12345")

        assertFalse(result)
    }

    @Test
    fun `resetting pin does not affect recovery fields`() = runTest {
        repo.users["testuser"] = UserAccount(
            username = "testuser", role = "STUDENT",
            hashedPassword = "old", salt = "old_salt",
            securityQuestion = "recovery_q_first_pet",
            securityAnswerSalt = "ans_salt",
            securityAnswerHash = "ans_hash"
        )

        useCase("testuser", "1111")

        assertEquals("recovery_q_first_pet", repo.users["testuser"]!!.securityQuestion)
        assertEquals("ans_salt", repo.users["testuser"]!!.securityAnswerSalt)
        assertEquals("ans_hash", repo.users["testuser"]!!.securityAnswerHash)
    }

    class FakeResetRepo : IPracticeRepository {
        val users = mutableMapOf<String, UserAccount>()
        override val allSessions: Flow<List<PracticeSession>> = flowOf(emptyList())
        override val allLevelProgress: Flow<List<LessonProgress>> = flowOf(emptyList())
        override fun getSessionsByDate(dateString: String): Flow<List<PracticeSession>> = flowOf(emptyList())
        override suspend fun insertSession(session: PracticeSession) {}
        override suspend fun deleteSession(id: Int) {}
        override suspend fun clearSessions() {}
        override suspend fun insertLessonProgress(progress: LessonProgress) {}
        override suspend fun updateLessonCompletion(lessonId: String, completed: Boolean) {}
        override suspend fun getLessonProgressById(lessonId: String): LessonProgress? = null
        override val allUsers: Flow<List<UserAccount>> = flowOf(users.values.toList())
        override suspend fun insertUser(user: UserAccount) { users[user.username] = user }
        override suspend fun getUserByUsername(username: String): UserAccount? = users[username]
        override val allAssignments: Flow<List<Assignment>> = flowOf(emptyList())
        override fun getAssignmentsForStudent(studentUsername: String): Flow<List<Assignment>> = flowOf(emptyList())
        override fun getAssignmentsByTeacher(teacherUsername: String): Flow<List<Assignment>> = flowOf(emptyList())
        override suspend fun insertAssignment(assignment: Assignment) {}
        override suspend fun updateAssignmentCompletion(id: Int, completed: Boolean) {}
        override suspend fun deleteAssignmentById(id: Int) {}
    }
}
