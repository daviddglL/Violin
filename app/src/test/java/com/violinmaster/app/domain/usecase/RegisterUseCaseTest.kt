package com.violinmaster.app.domain.usecase

import com.violinmaster.app.data.IPracticeRepository
import com.violinmaster.app.data.UserAccount
import com.violinmaster.app.data.Assignment
import com.violinmaster.app.data.LessonProgress
import com.violinmaster.app.data.PracticeSession
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class RegisterUseCaseTest {

  private lateinit var repo: FakeRegisterRepo
  private lateinit var useCase: RegisterUseCase

  @Before
  fun setup() {
    repo = FakeRegisterRepo()
    useCase = RegisterUseCase(repo)
  }

  @Test
  fun `valid registration creates user`() = runTest {
    val result = useCase("newuser", "1234", "STUDENT", 2000)

    assertNotNull(result)
    assertEquals("newuser", result!!.username)
    assertEquals("STUDENT", result.role)
    assertTrue(result.hashedPassword.isNotEmpty())
    assertTrue(result.salt.isNotEmpty())
    assertEquals(1, repo.insertedUsers.size)
  }

  @Test
  fun `duplicate username returns null`() = runTest {
    repo.users["dupe"] = UserAccount(username = "dupe", role = "STUDENT", hashedPassword = "h", salt = "s")

    val result = useCase("dupe", "1234", "STUDENT", 2000)

    assertNull(result)
    assertEquals(0, repo.insertedUsers.size)
  }

  @Test
  fun `blank username returns null`() = runTest {
    val result = useCase("", "1234", "STUDENT", 2000)
    assertNull(result)
  }

  @Test
  fun `short pin returns null`() = runTest {
    val result = useCase("testuser", "12", "STUDENT", 2000)
    assertNull(result)
  }

  @Test
  fun `invalid birth year returns null`() = runTest {
    val result = useCase("testuser", "1234", "STUDENT", 1800)
    assertNull(result)
  }

  // -- Fake implementation --

  class FakeRegisterRepo : IPracticeRepository {
    val users = mutableMapOf<String, UserAccount>()
    val insertedUsers = mutableListOf<UserAccount>()

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
    override suspend fun insertUser(user: UserAccount) { users[user.username] = user; insertedUsers.add(user) }
    override suspend fun getUserByUsername(username: String): UserAccount? = users[username]
    override val allAssignments: Flow<List<Assignment>> = flowOf(emptyList())
    override fun getAssignmentsForStudent(studentUsername: String): Flow<List<Assignment>> = flowOf(emptyList())
    override fun getAssignmentsByTeacher(teacherUsername: String): Flow<List<Assignment>> = flowOf(emptyList())
    override suspend fun insertAssignment(assignment: Assignment) {}
    override suspend fun updateAssignmentCompletion(id: Int, completed: Boolean) {}
    override suspend fun deleteAssignmentById(id: Int) {}
  }
}
