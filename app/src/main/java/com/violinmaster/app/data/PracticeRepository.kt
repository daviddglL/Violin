package com.violinmaster.app.data

import kotlinx.coroutines.flow.Flow

class PracticeRepository(
    private val sessionDao: SessionDao,
    private val lessonDao: LessonDao,
    private val userDao: UserDao,
    private val assignmentDao: AssignmentDao
) : IPracticeRepository {

    override val allSessions: Flow<List<PracticeSession>> = sessionDao.getAllSessions()
    override val allLevelProgress: Flow<List<LessonProgress>> = lessonDao.getAllLessons()

    override fun getSessionsByDate(dateString: String): Flow<List<PracticeSession>> {
        return sessionDao.getSessionsByDate(dateString)
    }

    override suspend fun insertSession(session: PracticeSession) {
        sessionDao.insertSession(session)
    }

    override suspend fun deleteSession(id: Int) {
        sessionDao.deleteSessionById(id)
    }

    override suspend fun clearSessions() {
        sessionDao.clearAllSessions()
    }

    override suspend fun insertLessonProgress(progress: LessonProgress) {
        lessonDao.insertLessonProgress(progress)
    }

    override suspend fun updateLessonCompletion(lessonId: String, completed: Boolean) {
        lessonDao.updateLessonCompletion(lessonId, completed)
    }

    override suspend fun getLessonProgressById(lessonId: String): LessonProgress? {
        return lessonDao.getLessonProgressById(lessonId)
    }

    // --- User Management ---
    override val allUsers: Flow<List<UserAccount>> = userDao.getAllUsers()

    override suspend fun insertUser(user: UserAccount) {
        userDao.insertUser(user)
    }

    override suspend fun getUserByUsername(username: String): UserAccount? {
        return userDao.getUserByUsername(username)
    }

    // --- Assignments Management ---
    override val allAssignments: Flow<List<Assignment>> = assignmentDao.getAllAssignments()

    override fun getAssignmentsForStudent(studentUsername: String): Flow<List<Assignment>> {
        return assignmentDao.getAssignmentsForStudent(studentUsername)
    }

    override fun getAssignmentsByTeacher(teacherUsername: String): Flow<List<Assignment>> {
        return assignmentDao.getAssignmentsByTeacher(teacherUsername)
    }

    override suspend fun insertAssignment(assignment: Assignment) {
        assignmentDao.insertAssignment(assignment)
    }

    override suspend fun updateAssignmentCompletion(id: Int, completed: Boolean) {
        assignmentDao.updateAssignmentCompletion(id, completed)
    }

    override suspend fun deleteAssignmentById(id: Int) {
        assignmentDao.deleteAssignmentById(id)
    }
}
