package com.violinmaster.app.data

import com.violinmaster.app.data.firebase.AssignmentSyncRepository
import com.violinmaster.app.data.firebase.LessonSyncRepository
import com.violinmaster.app.data.firebase.SessionSyncRepository
import com.violinmaster.app.data.firebase.UserSyncRepository
import kotlinx.coroutines.flow.Flow

/**
 * Facade repository implementing [IPracticeRepository] by routing operations
 * based on the [CloudConfig.cloudSyncEnabled] feature flag.
 *
 * **Cloud mode** (cloudSyncEnabled = true):
 * - Writes → FirestoreSyncRepository.write() (Firestore-first, Room cache)
 * - Full reads → FirestoreSyncRepository.observe() (Room Flow + Firestore listener)
 * - Filtered/single reads → DAO directly (Room cache is kept in sync by observe flows)
 * - Deletes → FirestoreSyncRepository.delete()
 *
 * **Local mode** (cloudSyncEnabled = false, default):
 * - All operations → Room DAOs directly (current behavior)
 *
 * This preserves the [IPracticeRepository] interface contract (18 methods)
 * so ViewModels and UseCases are completely unaffected.
 *
 * REQ-DI-003: PracticeRepository receives Firestore sync repos + Room DAOs.
 * REQ-DI-009: IPracticeRepository interface preserved; all 18 methods unchanged.
 */
class PracticeRepository(
    private val sessionSync: SessionSyncRepository,
    private val lessonSync: LessonSyncRepository,
    private val userSync: UserSyncRepository,
    private val assignmentSync: AssignmentSyncRepository,
    private val sessionDao: SessionDao,
    private val lessonDao: LessonDao,
    private val userDao: UserDao,
    private val assignmentDao: AssignmentDao,
    private val cloudConfig: CloudConfig
) : IPracticeRepository {

    // ── Session Operations ─────────────────────────────────────────────────

    override val allSessions: Flow<List<PracticeSession>>
        get() = if (cloudConfig.cloudSyncEnabled) {
            sessionSync.observe()
        } else {
            sessionDao.getAllSessions()
        }

    override fun getSessionsByDate(dateString: String): Flow<List<PracticeSession>> {
        // Filtered read always uses DAO — Room cache is kept in sync
        // by the observe() Flow when cloud is enabled.
        return sessionDao.getSessionsByDate(dateString)
    }

    override suspend fun insertSession(session: PracticeSession) {
        if (cloudConfig.cloudSyncEnabled) {
            sessionSync.write(session)
        } else {
            sessionDao.insertSession(session)
        }
    }

    override suspend fun deleteSession(id: Int) {
        if (cloudConfig.cloudSyncEnabled) {
            sessionSync.delete(id.toString())
        } else {
            sessionDao.deleteSessionById(id)
        }
    }

    override suspend fun clearSessions() {
        // clearSessions is a batch operation — always uses DAO.
        // In cloud mode, individual deletions would be too chatty.
        // This is typically used for testing/demo data reset.
        sessionDao.clearAllSessions()
    }

    // ── Lesson Operations ──────────────────────────────────────────────────

    override val allLevelProgress: Flow<List<LessonProgress>>
        get() = if (cloudConfig.cloudSyncEnabled) {
            lessonSync.observe()
        } else {
            lessonDao.getAllLessons()
        }

    override suspend fun insertLessonProgress(progress: LessonProgress) {
        if (cloudConfig.cloudSyncEnabled) {
            lessonSync.write(progress)
        } else {
            lessonDao.insertLessonProgress(progress)
        }
    }

    override suspend fun updateLessonCompletion(lessonId: String, completed: Boolean) {
        // Update the Room cache (will be picked up by snapshot listener
        // which re-reads from cache on next Firestore change).
        // In cloud mode, we need to write the updated entity to trigger sync.
        if (cloudConfig.cloudSyncEnabled) {
            val existing = lessonDao.getLessonProgressById(lessonId)
            if (existing != null) {
                val updated = existing.copy(completed = completed)
                lessonSync.write(updated)
            } else {
                lessonDao.updateLessonCompletion(lessonId, completed)
            }
        } else {
            lessonDao.updateLessonCompletion(lessonId, completed)
        }
    }

    override suspend fun getLessonProgressById(lessonId: String): LessonProgress? {
        // Single-read always uses DAO — Room cache is kept in sync
        return lessonDao.getLessonProgressById(lessonId)
    }

    // ── User Operations ────────────────────────────────────────────────────

    override val allUsers: Flow<List<UserAccount>>
        get() = if (cloudConfig.cloudSyncEnabled) {
            userSync.observe()
        } else {
            userDao.getAllUsers()
        }

    override suspend fun insertUser(user: UserAccount) {
        if (cloudConfig.cloudSyncEnabled) {
            userSync.write(user)
        } else {
            userDao.insertUser(user)
        }
    }

    override suspend fun getUserByUsername(username: String): UserAccount? {
        // Single-read always uses DAO
        return userDao.getUserByUsername(username)
    }

    override suspend fun getUserByFirebaseUid(uid: String): UserAccount? {
        return userDao.getUserByFirebaseUid(uid)
    }

    override suspend fun updateFirebaseUid(username: String, uid: String) {
        userDao.updateFirebaseUid(username, uid)
    }

    override suspend fun deleteAllUserData(user: UserAccount) {
        userDao.deleteUser(user)
        sessionDao.clearAllSessions()
        lessonDao.clearAllLessons()
        assignmentDao.clearAllAssignments()
    }

    // ── Assignment Operations ──────────────────────────────────────────────

    override val allAssignments: Flow<List<Assignment>>
        get() = if (cloudConfig.cloudSyncEnabled) {
            assignmentSync.observe()
        } else {
            assignmentDao.getAllAssignments()
        }

    override fun getAssignmentsForStudent(studentUsername: String): Flow<List<Assignment>> {
        // Filtered read always uses DAO — Room cache is kept in sync
        return assignmentDao.getAssignmentsForStudent(studentUsername)
    }

    override fun getAssignmentsByTeacher(teacherUsername: String): Flow<List<Assignment>> {
        // Filtered read always uses DAO
        return assignmentDao.getAssignmentsByTeacher(teacherUsername)
    }

    override suspend fun insertAssignment(assignment: Assignment) {
        if (cloudConfig.cloudSyncEnabled) {
            assignmentSync.write(assignment)
        } else {
            assignmentDao.insertAssignment(assignment)
        }
    }

    override suspend fun updateAssignmentCompletion(id: Int, completed: Boolean) {
        // Always updates Room cache. In cloud mode, the observe() Flow
        // provides eventual consistency between Firestore and Room.
        // Full bidirectional sync for update operations is deferred.
        assignmentDao.updateAssignmentCompletion(id, completed)
    }

    override suspend fun deleteAssignmentById(id: Int) {
        if (cloudConfig.cloudSyncEnabled) {
            assignmentSync.delete(id.toString())
        } else {
            assignmentDao.deleteAssignmentById(id)
        }
    }
}
