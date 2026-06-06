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
class UpdateSkillLevelUseCaseTest {

  private lateinit var context: Context
  private lateinit var repo: FakeUpdateSkillRepo
  private lateinit var authManager: AuthManager
  private lateinit var useCase: UpdateSkillLevelUseCase

  @Before
  fun setup() {
    context = ApplicationProvider.getApplicationContext()
    repo = FakeUpdateSkillRepo()
    authManager = AuthManager(context)
    useCase = UpdateSkillLevelUseCase(repo, authManager)
  }

  @After
  fun tearDown() {
    authManager.clearSession()
  }

  @Test
  fun `updates skill level for current user`() = runTest {
    val user = UserAccount(
      username = "student1", role = "STUDENT",
      hashedPassword = "hash", salt = "salt", skillLevel = "Beginner"
    )
    authManager.restoreCurrentUser(user)

    useCase("Intermediate")

    assertEquals(1, repo.updatedUsers.size)
    assertEquals("Intermediate", repo.updatedUsers[0].skillLevel)
    assertEquals("Intermediate", authManager.currentUser.value?.skillLevel)
  }

  @Test
  fun `updates to advanced level`() = runTest {
    val user = UserAccount(
      username = "student1", role = "STUDENT",
      hashedPassword = "hash", salt = "salt", skillLevel = "Intermediate"
    )
    authManager.restoreCurrentUser(user)

    useCase("Advanced")

    assertEquals("Advanced", repo.updatedUsers[0].skillLevel)
    assertEquals("Advanced", authManager.currentUser.value?.skillLevel)
  }

  @Test
  fun `no current user is a no op`() = runTest {
    useCase("Advanced")

    assertTrue(repo.updatedUsers.isEmpty())
  }

  @Test
  fun `preserves other user fields when updating`() = runTest {
    val user = UserAccount(
      username = "student1", role = "STUDENT",
      hashedPassword = "hash", salt = "salt",
      points = 500, skillLevel = "Beginner"
    )
    authManager.restoreCurrentUser(user)

    useCase("Advanced")

    assertEquals("student1", repo.updatedUsers[0].username)
    assertEquals("STUDENT", repo.updatedUsers[0].role)
    assertEquals(500, repo.updatedUsers[0].points)
    assertEquals("Advanced", repo.updatedUsers[0].skillLevel)
  }

  class FakeUpdateSkillRepo : IPracticeRepository {
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
