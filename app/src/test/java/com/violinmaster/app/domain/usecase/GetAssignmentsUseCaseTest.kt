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

class GetAssignmentsUseCaseTest {

  private lateinit var repo: FakeAssignmentsRepo
  private lateinit var useCase: GetAssignmentsUseCase

  @Before
  fun setup() {
    repo = FakeAssignmentsRepo()
    useCase = GetAssignmentsUseCase(repo)
  }

  @Test
  fun `teacher gets assignments by teacher code`() = runTest {
    val assignments = listOf(
      Assignment(id = 1, title = "Scale Run", description = "", teacherUsername = "TEACH-123", studentUsername = "ALL"),
      Assignment(id = 2, title = "Etude 1", description = "", teacherUsername = "TEACH-123", studentUsername = "bob")
    )
    repo.teacherAssignmentsFlow["TEACH-123"] = MutableStateFlow(assignments)

    val result = useCase("teacher1", "TEACHER", "TEACH-123").first()

    assertEquals(2, result.size)
  }

  @Test
  fun `student gets filtered assignments`() = runTest {
    val assignments = listOf(
      Assignment(id = 1, title = "Scale Run", description = "", teacherUsername = "TEACH-123", studentUsername = "alice"),
      Assignment(id = 2, title = "Etude 1", description = "", teacherUsername = "TEACH-123", studentUsername = "ALL"),
      Assignment(id = 3, title = "For Bob", description = "", teacherUsername = "TEACH-123", studentUsername = "bob")
    )
    repo.allAssignmentsFlow.value = assignments

    val result = useCase("alice", "STUDENT", "TEACH-123").first()

    assertEquals(2, result.size)
    val titles = result.map { it.title }
    assertEquals(true, titles.contains("Scale Run"))
    assertEquals(true, titles.contains("Etude 1"))
  }

  @Test
  fun `unknown role returns empty list`() = runTest {
    val result = useCase("user", "GUEST", "").first()
    assertEquals(0, result.size)
  }

  class FakeAssignmentsRepo : IPracticeRepository {
    val allAssignmentsFlow = MutableStateFlow<List<Assignment>>(emptyList())
    val teacherAssignmentsFlow = mutableMapOf<String, MutableStateFlow<List<Assignment>>>()

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
    override suspend fun insertUser(user: UserAccount) {}
    override suspend fun getUserByUsername(username: String): UserAccount? = null
    override val allAssignments: Flow<List<Assignment>> = allAssignmentsFlow
    override fun getAssignmentsForStudent(studentUsername: String): Flow<List<Assignment>> = allAssignmentsFlow
    override fun getAssignmentsByTeacher(teacherUsername: String): Flow<List<Assignment>> {
      return teacherAssignmentsFlow.getOrPut(teacherUsername) { MutableStateFlow(emptyList()) }
    }
    override suspend fun insertAssignment(assignment: Assignment) {}
    override suspend fun updateAssignmentCompletion(id: Int, completed: Boolean) {}
    override suspend fun deleteAssignmentById(id: Int) {}
  }
}
