package com.violinmaster.app.domain.usecase

import com.violinmaster.app.data.IPracticeRepository
import com.violinmaster.app.data.Assignment
import com.violinmaster.app.data.LessonProgress
import com.violinmaster.app.data.PracticeSession
import com.violinmaster.app.data.UserAccount
import com.violinmaster.app.domain.model.RecoveryQuestion
import com.violinmaster.app.domain.util.Base64Encoder
import com.violinmaster.app.security.SecurityUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test

/**
 * Tests for SetRecoveryQuestionUseCase — REQ-PINREC-001.
 */
class SetRecoveryQuestionUseCaseTest {

    private lateinit var repo: FakeRecoveryRepo
    private lateinit var useCase: SetRecoveryQuestionUseCase

    @Before
    fun setup() {
        repo = FakeRecoveryRepo()
        useCase = SetRecoveryQuestionUseCase(repo)
    }

    @Test
    fun `sets recovery question for existing user`() = runTest {
        // Arrange: create existing user without recovery
        repo.users["testuser"] = UserAccount(
            username = "testuser", role = "STUDENT",
            hashedPassword = "hash", salt = "salt_pin"
        )

        // Act
        val result = useCase("testuser", RecoveryQuestion.FIRST_PET, "Fluffy")

        // Assert
        assertTrue(result)
        val updatedUser = repo.users["testuser"]
        assertNotNull(updatedUser)
        assertEquals("recovery_q_first_pet", updatedUser!!.securityQuestion)
        assertTrue(updatedUser.securityAnswerSalt.isNotEmpty())
        assertTrue(updatedUser.securityAnswerHash.isNotEmpty())
        // Verify answer can be verified using the same algorithm
        val salt = Base64Encoder.decode(updatedUser.securityAnswerSalt)
        val expectedHash = SecurityUtils.hashPasscode("Fluffy".toCharArray(), salt)
        assertEquals(expectedHash, updatedUser.securityAnswerHash)
    }

    @Test
    fun `returns false for non-existent user`() = runTest {
        val result = useCase("nobody", RecoveryQuestion.BIRTH_CITY, "Springfield")
        assertFalse(result)
    }

    @Test
    fun `overwrites existing recovery question`() = runTest {
        // Arrange
        repo.users["testuser"] = UserAccount(
            username = "testuser", role = "STUDENT",
            hashedPassword = "hash", salt = "salt_pin",
            securityQuestion = "recovery_q_first_pet",
            securityAnswerSalt = "old_salt",
            securityAnswerHash = "old_hash"
        )

        // Act
        val result = useCase("testuser", RecoveryQuestion.BIRTH_CITY, "Springfield")

        // Assert
        assertTrue(result)
        assertEquals("recovery_q_birth_city", repo.users["testuser"]!!.securityQuestion)
        assertTrue(repo.users["testuser"]!!.securityAnswerSalt != "old_salt")
    }

    @Test
    fun `different questions for different users produce different hashes`() = runTest {
        repo.users["user_a"] = UserAccount(
            username = "user_a", role = "STUDENT",
            hashedPassword = "h1", salt = "s1"
        )
        repo.users["user_b"] = UserAccount(
            username = "user_b", role = "STUDENT",
            hashedPassword = "h2", salt = "s2"
        )

        useCase("user_a", RecoveryQuestion.FIRST_PET, "Fluffy")
        useCase("user_b", RecoveryQuestion.FIRST_PET, "Fluffy")

        // Same answer, different salts → different hashes
        assertTrue(
            repo.users["user_a"]!!.securityAnswerHash != repo.users["user_b"]!!.securityAnswerHash
        )
    }

    class FakeRecoveryRepo : IPracticeRepository {
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
