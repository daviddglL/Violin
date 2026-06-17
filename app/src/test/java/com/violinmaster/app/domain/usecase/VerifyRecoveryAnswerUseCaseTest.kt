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
import org.junit.Before
import org.junit.Test

/**
 * Tests for VerifyRecoveryAnswerUseCase — REQ-PINREC-002.
 */
class VerifyRecoveryAnswerUseCaseTest {

    private lateinit var repo: FakeVerifyRepo
    private lateinit var useCase: VerifyRecoveryAnswerUseCase

    @Before
    fun setup() {
        repo = FakeVerifyRepo()
        useCase = VerifyRecoveryAnswerUseCase(repo)
    }

    @Test
    fun `correct answer returns true`() = runTest {
        // Arrange: set up user with recovery answer
        val answer = "Fluffy"
        val salt = SecurityUtils.generateSalt()
        val hash = SecurityUtils.hashPasscode(answer.toCharArray(), salt)
        repo.users["testuser"] = UserAccount(
            username = "testuser", role = "STUDENT",
            hashedPassword = "hash", salt = "pin_salt",
            securityQuestion = RecoveryQuestion.FIRST_PET.questionKey,
            securityAnswerSalt = Base64Encoder.encodeToString(salt),
            securityAnswerHash = hash
        )

        // Act
        val result = useCase("testuser", answer)

        // Assert
        assertTrue(result)
        assertEquals(0, repo.users["testuser"]!!.points) // No state change on success
    }

    @Test
    fun `wrong answer returns false`() = runTest {
        val salt = SecurityUtils.generateSalt()
        val hash = SecurityUtils.hashPasscode("Fluffy".toCharArray(), salt)
        repo.users["testuser"] = UserAccount(
            username = "testuser", role = "STUDENT",
            hashedPassword = "hash", salt = "pin_salt",
            securityQuestion = RecoveryQuestion.FIRST_PET.questionKey,
            securityAnswerSalt = Base64Encoder.encodeToString(salt),
            securityAnswerHash = hash
        )

        val result = useCase("testuser", "WrongAnswer")

        assertFalse(result)
    }

    @Test
    fun `non-existent user returns false`() = runTest {
        val result = useCase("nobody", "anything")
        assertFalse(result)
    }

    @Test
    fun `user without recovery question returns false`() = runTest {
        repo.users["legacy"] = UserAccount(
            username = "legacy", role = "FREELANCER",
            hashedPassword = "hash", salt = "salt",
            securityQuestion = "",
            securityAnswerSalt = "",
            securityAnswerHash = ""
        )

        val result = useCase("legacy", "anything")

        assertFalse(result)
    }

    @Test
    fun `case-sensitive answer — lowercase mismatch returns false`() = runTest {
        val answer = "Fluffy"
        val salt = SecurityUtils.generateSalt()
        val hash = SecurityUtils.hashPasscode(answer.toCharArray(), salt)
        repo.users["testuser"] = UserAccount(
            username = "testuser", role = "STUDENT",
            hashedPassword = "hash", salt = "pin_salt",
            securityQuestion = RecoveryQuestion.FIRST_PET.questionKey,
            securityAnswerSalt = Base64Encoder.encodeToString(salt),
            securityAnswerHash = hash
        )

        val result = useCase("testuser", "fluffy") // lowercase

        assertFalse(result)
    }

    class FakeVerifyRepo : IPracticeRepository {
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
