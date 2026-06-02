package com.violinmaster.app.domain.usecase

import com.violinmaster.app.data.Assignment
import com.violinmaster.app.data.IPracticeRepository
import com.violinmaster.app.data.LessonProgress
import com.violinmaster.app.data.PracticeSession
import com.violinmaster.app.data.UserAccount
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class GetLeaderboardUseCaseTest {

  private lateinit var repo: FakeLeaderboardRepo
  private lateinit var useCase: GetLeaderboardUseCase

  @Before
  fun setup() {
    repo = FakeLeaderboardRepo()
    useCase = GetLeaderboardUseCase(repo)
  }

  @Test
  fun `returns users sorted by points descending`() = runTest {
    val users = listOf(
      UserAccount(username = "a", role = "STUDENT", hashedPassword = "h", salt = "s", points = 100),
      UserAccount(username = "b", role = "STUDENT", hashedPassword = "h", salt = "s", points = 500),
      UserAccount(username = "c", role = "STUDENT", hashedPassword = "h", salt = "s", points = 300)
    )
    repo.usersFlow.value = users

    val result = useCase().first()

    assertEquals(3, result.size)
    assertEquals("b", result[0].username)
    assertEquals("c", result[1].username)
    assertEquals("a", result[2].username)
  }

  @Test
  fun `empty repository returns empty list`() = runTest {
    repo.usersFlow.value = emptyList()

    val result = useCase().first()

    assertEquals(0, result.size)
  }

  class FakeLeaderboardRepo : IPracticeRepository {
    val usersFlow = MutableStateFlow<List<UserAccount>>(emptyList())
    override val allSessions: Flow<List<PracticeSession>> = MutableStateFlow(emptyList())
    override val allLevelProgress: Flow<List<LessonProgress>> = MutableStateFlow(emptyList())
    override fun getSessionsByDate(dateString: String): Flow<List<PracticeSession>> = MutableStateFlow(emptyList())
    override suspend fun insertSession(session: PracticeSession) {}
    override suspend fun deleteSession(id: Int) {}
    override suspend fun clearSessions() {}
    override suspend fun insertLessonProgress(progress: LessonProgress) {}
    override suspend fun updateLessonCompletion(lessonId: String, completed: Boolean) {}
    override suspend fun getLessonProgressById(lessonId: String): LessonProgress? = null
    override val allUsers: Flow<List<UserAccount>> = usersFlow
    override suspend fun insertUser(user: UserAccount) {}
    override suspend fun getUserByUsername(username: String): UserAccount? = null
    override val allAssignments: Flow<List<Assignment>> = MutableStateFlow(emptyList())
    override fun getAssignmentsForStudent(studentUsername: String): Flow<List<Assignment>> = MutableStateFlow(emptyList())
    override fun getAssignmentsByTeacher(teacherUsername: String): Flow<List<Assignment>> = MutableStateFlow(emptyList())
    override suspend fun insertAssignment(assignment: Assignment) {}
    override suspend fun updateAssignmentCompletion(id: Int, completed: Boolean) {}
    override suspend fun deleteAssignmentById(id: Int) {}
  }
}
