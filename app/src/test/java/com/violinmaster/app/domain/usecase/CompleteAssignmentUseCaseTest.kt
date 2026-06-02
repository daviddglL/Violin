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
import kotlinx.coroutines.flow.MutableStateFlow
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
class CompleteAssignmentUseCaseTest {

  private lateinit var context: Context
  private lateinit var repo: FakeCompleteAssignmentRepo
  private lateinit var authManager: AuthManager
  private lateinit var useCase: CompleteAssignmentUseCase

  @Before
  fun setup() {
    context = ApplicationProvider.getApplicationContext()
    repo = FakeCompleteAssignmentRepo()
    authManager = AuthManager(context)
    useCase = CompleteAssignmentUseCase(repo, authManager)
  }

  @After
  fun tearDown() {
    authManager.clearSession()
  }

  @Test
  fun `marks assignment complete and awards points`() = runTest {
    val user = UserAccount(
      username = "student1", role = "STUDENT",
      hashedPassword = "hash", salt = "salt", points = 0
    )
    authManager.restoreCurrentUser(user)

    useCase(42)

    assertTrue(repo.completedIds.contains(42))
    assertEquals(1, repo.updatedUsers.size)
    assertEquals(200, repo.updatedUsers[0].points)
    assertEquals(200, authManager.currentUser.value?.points)
  }

  @Test
  fun `no current user marks complete but skips points`() = runTest {
    useCase(42)

    assertTrue(repo.completedIds.contains(42))
    assertTrue(repo.updatedUsers.isEmpty())
  }

  class FakeCompleteAssignmentRepo : IPracticeRepository {
    val completedIds = mutableListOf<Int>()
    val updatedUsers = mutableListOf<UserAccount>()
    override val allSessions: Flow<List<PracticeSession>> = MutableStateFlow(emptyList())
    override val allLevelProgress: Flow<List<LessonProgress>> = MutableStateFlow(emptyList())
    override fun getSessionsByDate(dateString: String): Flow<List<PracticeSession>> = MutableStateFlow(emptyList())
    override suspend fun insertSession(session: PracticeSession) {}
    override suspend fun deleteSession(id: Int) {}
    override suspend fun clearSessions() {}
    override suspend fun insertLessonProgress(progress: LessonProgress) {}
    override suspend fun updateLessonCompletion(lessonId: String, completed: Boolean) {}
    override suspend fun getLessonProgressById(lessonId: String): LessonProgress? = null
    override val allUsers: Flow<List<UserAccount>> = MutableStateFlow(emptyList())
    override suspend fun insertUser(user: UserAccount) { updatedUsers.add(user) }
    override suspend fun getUserByUsername(username: String): UserAccount? = null
    override val allAssignments: Flow<List<Assignment>> = MutableStateFlow(emptyList())
    override fun getAssignmentsForStudent(studentUsername: String): Flow<List<Assignment>> = MutableStateFlow(emptyList())
    override fun getAssignmentsByTeacher(teacherUsername: String): Flow<List<Assignment>> = MutableStateFlow(emptyList())
    override suspend fun insertAssignment(assignment: Assignment) {}
    override suspend fun updateAssignmentCompletion(id: Int, completed: Boolean) { completedIds.add(id) }
    override suspend fun deleteAssignmentById(id: Int) {}
  }
}
