package com.violinmaster.app.domain.usecase

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.violinmaster.app.data.Assignment
import com.violinmaster.app.data.IPracticeRepository
import com.violinmaster.app.data.LessonProgress
import com.violinmaster.app.data.PracticeSession
import com.violinmaster.app.data.UserAccount
import com.violinmaster.app.di.AuthManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class EarnPointsUseCaseTest {

  private lateinit var context: Context
  private lateinit var repo: FakeEarnPointsRepo
  private lateinit var authManager: AuthManager
  private lateinit var useCase: EarnPointsUseCase

  @Before
  fun setup() {
    context = ApplicationProvider.getApplicationContext()
    repo = FakeEarnPointsRepo()
    authManager = AuthManager(context)
    useCase = EarnPointsUseCase(repo, authManager)
  }

  @After
  fun tearDown() {
    authManager.clearSession()
  }

  @Test
  fun `adds points to current user`() = runTest {
    val user = UserAccount(
      username = "student1", role = "STUDENT",
      hashedPassword = "hash", salt = "salt", points = 100
    )
    authManager.restoreCurrentUser(user)

    useCase(50)

    assertEquals(1, repo.updatedUsers.size)
    assertEquals(150, repo.updatedUsers[0].points)
    assertEquals(150, authManager.currentUser.value?.points)
  }

  @Test
  fun `no current user is a no op`() = runTest {
    useCase(50)

    assertTrue(repo.updatedUsers.isEmpty())
  }

  @Test
  fun `adds zero points leaves total unchanged`() = runTest {
    val user = UserAccount(
      username = "student1", role = "STUDENT",
      hashedPassword = "hash", salt = "salt", points = 100
    )
    authManager.restoreCurrentUser(user)

    useCase(0)

    assertEquals(100, repo.updatedUsers[0].points)
  }

  class FakeEarnPointsRepo : IPracticeRepository {
    val updatedUsers = mutableListOf<UserAccount>()
    override val allSessions: Flow<List<PracticeSession>> = flowOf(emptyList())
    override val allLevelProgress: Flow<List<LessonProgress>> = flowOf(emptyList())
    override fun getSessionsByDate(dateString: String): Flow<List<PracticeSession>> = flowOf(emptyList())
    override suspend fun insertSession(session: PracticeSession) {}
    override suspend fun deleteSession(id: Int) {}
    override suspend fun clearSessions() {}
    override suspend fun insertLessonProgress(progress: LessonProgress) {}
    override suspend fun updateLessonCompletion(lessonId: String, completed: Boolean) {}
    override suspend fun getLessonProgressById(lessonId: String): LessonProgress? = null
    override val allUsers: Flow<List<UserAccount>> = flowOf(emptyList())
    override suspend fun insertUser(user: UserAccount) { updatedUsers.add(user) }
    override suspend fun getUserByUsername(username: String): UserAccount? = null
    override val allAssignments: Flow<List<Assignment>> = flowOf(emptyList())
    override fun getAssignmentsForStudent(studentUsername: String): Flow<List<Assignment>> = flowOf(emptyList())
    override fun getAssignmentsByTeacher(teacherUsername: String): Flow<List<Assignment>> = flowOf(emptyList())
    override suspend fun insertAssignment(assignment: Assignment) {}
    override suspend fun updateAssignmentCompletion(id: Int, completed: Boolean) {}
    override suspend fun deleteAssignmentById(id: Int) {}
  }
}
