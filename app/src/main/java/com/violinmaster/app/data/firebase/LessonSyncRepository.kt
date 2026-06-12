package com.violinmaster.app.data.firebase

import com.violinmaster.app.data.LessonDao
import com.violinmaster.app.data.LessonProgress
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow

/**
 * Sync repository for lesson progress.
 *
 * Collection path: `lesson_progress/{firebaseUid}_{lessonId}` — flat collection
 * with compound document ID for per-user scoping.
 *
 * Firestore document ID: the lessonId (which is unique per lesson).
 *
 * REQ-CSYNC-004: Flat collection with compound docId for per-user scoping.
 *
 * @param collection The Firestore collection abstraction (injected for testability).
 * @param lessonDao Room DAO for lesson cache operations.
 * @param dispatcher Coroutine dispatcher. Defaults to [Dispatchers.IO].
 */
class LessonSyncRepository(
    collection: IFirestoreCollection<LessonDoc>,
    private val lessonDao: LessonDao,
    dispatcher: CoroutineDispatcher = Dispatchers.IO
) : FirestoreSyncRepository<LessonProgress, LessonDoc>(collection, dispatcher) {

    override fun LessonProgress.toFirestoreDoc(): LessonDoc = LessonDoc.fromEntity(this)
    override fun LessonDoc.toEntity(): LessonProgress = this.toEntity()
    override fun LessonDoc.docId(): String = lessonId

    override suspend fun insertCache(entity: LessonProgress) {
        lessonDao.insertLessonProgress(entity)
    }

    override suspend fun deleteCache(docId: String) {
        lessonDao.deleteLessonProgress(docId)
    }

    override fun observeCache(): Flow<List<LessonProgress>> = lessonDao.getAllLessons()
}
