package com.violinmaster.app.domain.usecase

import com.google.firebase.auth.FirebaseAuth
import com.violinmaster.app.data.IPracticeRepository
import com.violinmaster.app.data.UserAccount
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class LinkGoogleAccountUseCaseTest {

    private lateinit var repository: FakeLinkRepo
    private lateinit var useCase: LinkGoogleAccountUseCase

    @Before
    fun setup() {
        repository = FakeLinkRepo()
        useCase = LinkGoogleAccountUseCase(
            firebaseAuth = FirebaseAuth.getInstance(),
            repository = repository
        )
    }

    @Test
    fun `use case constructs without crash`() = runTest {
        assertNotNull(useCase)
    }

    @Test
    fun `link succeeds when firebaseUid is unique`() = runTest {
        val user = UserAccount(
            username = "pin_user", role = "STUDENT",
            hashedPassword = "h", salt = "s", firebaseUid = null
        )
        repository.users["pin_user"] = user

        // Simulate: firebaseUid lookup returns null (not linked to anyone yet)
        val result = useCase.checkUniqueness("new_firebase_uid")
        assertTrue(result)
    }

    @Test
    fun `link fails when firebaseUid already exists`() = runTest {
        val existingUser = UserAccount(
            username = "other_user", role = "FREELANCER",
            hashedPassword = "h", salt = "s", firebaseUid = "existing_uid"
        )
        repository.users["other_user"] = existingUser

        val result = useCase.checkUniqueness("existing_uid")
        assertEquals(false, result)
    }

    class FakeLinkRepo : IPracticeRepository {
        val users = mutableMapOf<String, UserAccount>()
        override val allSessions = kotlinx.coroutines.flow.flowOf(emptyList<com.violinmaster.app.data.PracticeSession>())
        override val allLevelProgress = kotlinx.coroutines.flow.flowOf(emptyList<com.violinmaster.app.data.LessonProgress>())
        override fun getSessionsByDate(d: String) = kotlinx.coroutines.flow.flowOf(emptyList<com.violinmaster.app.data.PracticeSession>())
        override suspend fun insertSession(s: com.violinmaster.app.data.PracticeSession) {}
        override suspend fun deleteSession(id: Int) {}
        override suspend fun clearSessions() {}
        override suspend fun insertLessonProgress(lp: com.violinmaster.app.data.LessonProgress) {}
        override suspend fun updateLessonCompletion(id: String, c: Boolean) {}
        override suspend fun getLessonProgressById(id: String) = null
        override val allUsers = kotlinx.coroutines.flow.flowOf(users.values.toList())
        override suspend fun insertUser(u: UserAccount) { users[u.username] = u }
        override suspend fun getUserByUsername(u: String) = users[u]
        override suspend fun getUserByFirebaseUid(uid: String) = users.values.find { it.firebaseUid == uid }
        override suspend fun updateFirebaseUid(username: String, uid: String) {
            val existing = users[username] ?: return
            users[username] = existing.copy(firebaseUid = uid)
        }
        override val allAssignments = kotlinx.coroutines.flow.flowOf(emptyList<com.violinmaster.app.data.Assignment>())
        override fun getAssignmentsForStudent(s: String) = kotlinx.coroutines.flow.flowOf(emptyList<com.violinmaster.app.data.Assignment>())
        override fun getAssignmentsByTeacher(t: String) = kotlinx.coroutines.flow.flowOf(emptyList<com.violinmaster.app.data.Assignment>())
        override suspend fun insertAssignment(a: com.violinmaster.app.data.Assignment) {}
        override suspend fun updateAssignmentCompletion(id: Int, c: Boolean) {}
        override suspend fun deleteAssignmentById(id: Int) {}
    }
}
