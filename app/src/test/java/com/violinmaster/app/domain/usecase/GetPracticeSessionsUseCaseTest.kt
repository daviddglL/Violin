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

class GetPracticeSessionsUseCaseTest {

  private lateinit var repo: FakePracticeSessionsRepo
  private lateinit var useCase: GetPracticeSessionsUseCase

  @Before
  fun setup() {
    repo = FakePracticeSessionsRepo()
    useCase = GetPracticeSessionsUseCase(repo)
  }

  @Test
  fun `returns sessions filtered by date range`() = runTest {
    val s1 = PracticeSession(id = 1, dateString = "2026-06-01", durationSeconds = 120, category = "Scales")
    val s2 = PracticeSession(id = 2, dateString = "2026-06-02", durationSeconds = 300, category = "Arpeggios")
    repo.sessionsFlow.value = listOf(s1, s2)

    val result = useCase("2026-06-01", "2026-06-01").first()

    assertEquals(1, result.size)
    assertEquals("2026-06-01", result[0].dateString)
  }

  @Test
  fun `empty date range returns empty list`() = runTest {
    repo.sessionsFlow.value = emptyList()

    val result = useCase("2026-01-01", "2026-01-01").first()

    assertEquals(0, result.size)
  }

  class FakePracticeSessionsRepo : IPracticeRepository {
    val sessionsFlow = MutableStateFlow<List<PracticeSession>>(emptyList())
    override val allSessions: Flow<List<PracticeSession>> = sessionsFlow
    override val allLevelProgress: Flow<List<LessonProgress>> = MutableStateFlow(emptyList())
    override fun getSessionsByDate(dateString: String): Flow<List<PracticeSession>> = sessionsFlow
    override suspend fun insertSession(session: PracticeSession) {}
    override suspend fun deleteSession(id: Int) {}
    override suspend fun clearSessions() {}
    override suspend fun insertLessonProgress(progress: LessonProgress) {}
    override suspend fun updateLessonCompletion(lessonId: String, completed: Boolean) {}
    override suspend fun getLessonProgressById(lessonId: String): LessonProgress? = null
    override val allUsers: Flow<List<UserAccount>> = MutableStateFlow(emptyList())
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
