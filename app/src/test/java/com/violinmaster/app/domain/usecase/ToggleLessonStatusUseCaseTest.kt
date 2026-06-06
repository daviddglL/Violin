package com.violinmaster.app.domain.usecase

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.violinmaster.app.data.Assignment
import com.violinmaster.app.data.IPracticeRepository
import com.violinmaster.app.data.LessonProgress
import com.violinmaster.app.data.PracticeSession
import com.violinmaster.app.data.UserAccount
import com.violinmaster.app.di.AuthManager
import com.violinmaster.app.domain.util.ScoringPolicy
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
class ToggleLessonStatusUseCaseTest {

  private lateinit var context: Context
  private lateinit var repo: FakeToggleLessonRepo
  private lateinit var authManager: AuthManager
  private lateinit var useCase: ToggleLessonStatusUseCase

  @Before
  fun setup() {
    context = ApplicationProvider.getApplicationContext()
    repo = FakeToggleLessonRepo()
    authManager = AuthManager(context)
    useCase = ToggleLessonStatusUseCase(repo, authManager)
  }

  @After
  fun tearDown() {
    authManager.clearSession()
  }

  @Test
  fun `marks incomplete lesson as complete and awards points`() = runTest {
    repo.lessons["beg_1"] = LessonProgress("beg_1", "Posture", "Beginner", false, 0)
    val user = UserAccount(
      username = "student1", role = "STUDENT",
      hashedPassword = "hash", salt = "salt", points = 0
    )
    authManager.restoreCurrentUser(user)

    useCase("beg_1", true)

    assertTrue(repo.completedUpdates.contains("beg_1" to true))
    assertEquals(1, repo.updatedUsers.size)
    assertEquals(ScoringPolicy.LESSON_COMPLETION_POINTS, repo.updatedUsers[0].points)
    assertEquals(ScoringPolicy.LESSON_COMPLETION_POINTS, authManager.currentUser.value?.points)
  }

  @Test
  fun `marks lesson incomplete does not award points`() = runTest {
    repo.lessons["beg_1"] = LessonProgress("beg_1", "Posture", "Beginner", true, 0)
    val user = UserAccount(
      username = "student1", role = "STUDENT",
      hashedPassword = "hash", salt = "salt", points = 150
    )
    authManager.restoreCurrentUser(user)

    useCase("beg_1", false)

    assertTrue(repo.completedUpdates.contains("beg_1" to false))
    assertTrue(repo.updatedUsers.isEmpty())
  }

  @Test
  fun `already completed lesson does not award points again`() = runTest {
    repo.lessons["beg_1"] = LessonProgress("beg_1", "Posture", "Beginner", true, 150)
    val user = UserAccount(
      username = "student1", role = "STUDENT",
      hashedPassword = "hash", salt = "salt", points = 150
    )
    authManager.restoreCurrentUser(user)

    useCase("beg_1", true)

    assertTrue(repo.completedUpdates.contains("beg_1" to true))
    assertTrue(repo.updatedUsers.isEmpty())
  }

  @Test
  fun `no current user completes but skips points`() = runTest {
    repo.lessons["beg_1"] = LessonProgress("beg_1", "Posture", "Beginner", false, 0)

    useCase("beg_1", true)

    assertTrue(repo.completedUpdates.contains("beg_1" to true))
    assertTrue(repo.updatedUsers.isEmpty())
  }

  class FakeToggleLessonRepo : IPracticeRepository {
    val lessons = mutableMapOf<String, LessonProgress>()
    val completedUpdates = mutableListOf<Pair<String, Boolean>>()
    val updatedUsers = mutableListOf<UserAccount>()
    override val allSessions: Flow<List<PracticeSession>> = flowOf(emptyList())
    override val allLevelProgress: Flow<List<LessonProgress>> = flowOf(emptyList())
    override fun getSessionsByDate(dateString: String): Flow<List<PracticeSession>> = flowOf(emptyList())
    override suspend fun insertSession(session: PracticeSession) {}
    override suspend fun deleteSession(id: Int) {}
    override suspend fun clearSessions() {}
    override suspend fun insertLessonProgress(progress: LessonProgress) {}
    override suspend fun updateLessonCompletion(lessonId: String, completed: Boolean) {
      completedUpdates.add(lessonId to completed)
    }
    override suspend fun getLessonProgressById(lessonId: String): LessonProgress? = lessons[lessonId]
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
