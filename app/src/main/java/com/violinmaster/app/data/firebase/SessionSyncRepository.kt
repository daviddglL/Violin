package com.violinmaster.app.data.firebase

import com.violinmaster.app.data.PracticeSession
import com.violinmaster.app.data.SessionDao
import kotlinx.coroutines.flow.Flow

/**
 * Sync repository for practice sessions.
 *
 * Collection path: `users/{uid}/sessions/{id}` — subcollection under user document.
 * Firestore document ID: Room auto-generated Int → String.
 *
 * REQ-CSYNC-004: Session subcollection sync with date range filtering via Room DAO.
 *
 * @param collection The Firestore collection abstraction (injected for testability).
 * @param sessionDao Room DAO for session cache operations.
 */
class SessionSyncRepository(
    collection: IFirestoreCollection<SessionDoc>,
    private val sessionDao: SessionDao
) : FirestoreSyncRepository<PracticeSession, SessionDoc>(collection) {

    override fun PracticeSession.toFirestoreDoc(): SessionDoc = SessionDoc.fromEntity(this)
    override fun SessionDoc.toEntity(): PracticeSession = this.toEntity()
    override fun SessionDoc.docId(): String = id

    override suspend fun insertCache(entity: PracticeSession) {
        sessionDao.insertSession(entity)
    }

    override suspend fun deleteCache(docId: String) {
        sessionDao.deleteSessionById(docId.toIntOrNull() ?: 0)
    }

    override fun observeCache(): Flow<List<PracticeSession>> = sessionDao.getAllSessions()
}
