package com.violinmaster.app.domain.usecase

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.violinmaster.app.data.IPracticeRepository
import com.violinmaster.app.data.UserAccount
import com.violinmaster.app.data.Assignment
import com.violinmaster.app.data.LessonProgress
import com.violinmaster.app.data.PracticeSession
import com.violinmaster.app.di.AuthManager
import com.violinmaster.app.domain.util.Base64Encoder
import com.violinmaster.app.security.SecurityUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class LoginUseCaseTest {

  private lateinit var context: Context
  private lateinit var repo: FakeLoginRepository
  private lateinit var authManager: AuthManager
  private lateinit var useCase: LoginUseCase

  @Before
  fun setup() {
    context = ApplicationProvider.getApplicationContext()
    repo = FakeLoginRepository()
    authManager = AuthManager(context)
    useCase = LoginUseCase(repo, authManager)
  }

  @After
  fun tearDown() {
    authManager.clearSession()
  }

  @Test
  fun `valid login returns user and saves session`() = runTest {
    val salt = SecurityUtils.generateSalt()
    val hashed = SecurityUtils.hashPasscode("1234".toCharArray(), salt)
    val user = UserAccount(
      username = "testuser", role = "STUDENT",
      hashedPassword = hashed,
      salt = Base64Encoder.encodeToString(salt)
    )
    repo.users["testuser"] = user

    val result = useCase("testuser", "1234")

    assertNotNull(result)
    assertEquals("testuser", result!!.username)
    assertEquals("testuser", authManager.currentUser.value?.username)
  }

  @Test
  fun `wrong pin returns null`() = runTest {
    val salt = SecurityUtils.generateSalt()
    val hashed = SecurityUtils.hashPasscode("1234".toCharArray(), salt)
    val user = UserAccount(
      username = "testuser", role = "STUDENT",
      hashedPassword = hashed,
      salt = Base64Encoder.encodeToString(salt)
    )
    repo.users["testuser"] = user

    val result = useCase("testuser", "9999")
    assertNull(result)
    assertNull(authManager.currentUser.value)
  }

  @Test
  fun `user not found returns null`() = runTest {
    val result = useCase("nobody", "1234")
    assertNull(result)
  }

  @Test
  fun `blank username returns null`() = runTest {
    val result = useCase("  ", "1234")
    assertNull(result)
  }

  @Test
  fun `short pin returns null`() = runTest {
    val result = useCase("testuser", "12")
    assertNull(result)
  }

  class FakeLoginRepository : IPracticeRepository {
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
