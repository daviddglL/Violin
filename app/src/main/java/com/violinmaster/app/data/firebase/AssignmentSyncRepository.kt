package com.violinmaster.app.data.firebase

import com.violinmaster.app.data.Assignment
import com.violinmaster.app.data.AssignmentDao
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow

/**
 * Sync repository for student assignments.
 *
 * Collection path: `assignments/{id}` — top-level collection.
 * Firestore document ID: Room auto-generated Int → String.
 *
 * REQ-CSYNC-004: Assignments collection with role-based filtering via Room DAO.
 *
 * @param collection The Firestore collection abstraction (injected for testability).
 * @param assignmentDao Room DAO for assignment cache operations.
 * @param dispatcher Coroutine dispatcher. Defaults to [Dispatchers.IO].
 */
class AssignmentSyncRepository(
    collection: IFirestoreCollection<AssignmentDoc>,
    private val assignmentDao: AssignmentDao,
    dispatcher: CoroutineDispatcher = Dispatchers.IO
) : FirestoreSyncRepository<Assignment, AssignmentDoc>(collection, dispatcher) {

    override fun Assignment.toFirestoreDoc(): AssignmentDoc = AssignmentDoc.fromEntity(this)
    override fun AssignmentDoc.toEntity(): Assignment = this.toEntity()
    override fun AssignmentDoc.docId(): String = id

    override suspend fun insertCache(entity: Assignment) {
        assignmentDao.insertAssignment(entity)
    }

    override suspend fun deleteCache(docId: String) {
        assignmentDao.deleteAssignmentById(docId.toIntOrNull() ?: 0)
    }

    override fun observeCache(): Flow<List<Assignment>> = assignmentDao.getAllAssignments()
}
