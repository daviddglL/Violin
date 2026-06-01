package com.violinmaster.app.domain.usecase

import com.violinmaster.app.data.Assignment
import com.violinmaster.app.data.IPracticeRepository
import com.violinmaster.app.data.LessonProgress
import com.violinmaster.app.data.PracticeSession
import com.violinmaster.app.data.UserAccount
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

class SavePracticeSessionUseCaseTest {

  private lateinit var repo: FakeSaveSessionRepo
  private lateinit var useCase: SavePracticeSessionUseCase

  @Before
  fun setup() {
    repo = FakeSaveSessionRepo()
    useCase = SavePracticeSessionUseCase(repo)
  }

  @Test
  fun `saves session to repository`() = runTest {
    val session = PracticeSession(
      dateString = LocalDate.now().toString(),
      durationSeconds = 600,
      category = "Smart Tuner"
    )

    useCase(session)

    assertEquals(1, repo.insertedSessions.size)
    assertEquals("Smart Tuner", repo.insertedSessions[0].category)
    assertEquals(600, repo.insertedSessions[0].durationSeconds)
  }

  class FakeSaveSessionRepo : IPracticeRepository {
    val insertedSessions = mutableListOf<PracticeSession>()
    override val allSessions: Flow<List<PracticeSession>> = MutableStateFlow(emptyList())
    override val allLevelProgress: Flow<List<LessonProgress>> = MutableStateFlow(emptyList())
    override fun getSessionsByDate(dateString: String): Flow<List<PracticeSession>> = MutableStateFlow(emptyList())
    override suspend fun insertSession(session: PracticeSession) { insertedSessions.add(session) }
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
