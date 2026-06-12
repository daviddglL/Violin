package com.violinmaster.app.data.firebase

import com.violinmaster.app.data.UserAccount
import com.violinmaster.app.data.UserDao
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow

/**
 * Sync repository for user accounts.
 *
 * Collection path: `users/{firebaseUid}` — firebaseUid is the document ID.
 * Sensitive fields (hashedPassword, salt) are excluded from Firestore via
 * the [UserDoc.fromEntity] mapping.
 *
 * REQ-CSYNC-004: Users collection with firebaseUid as docId.
 * REQ-AUTH-001: hashedPassword and salt NEVER stored in Firestore.
 *
 * @param collection The Firestore collection abstraction (injected for testability).
 * @param userDao Room DAO for user cache operations.
 * @param dispatcher Coroutine dispatcher. Defaults to [Dispatchers.IO].
 */
class UserSyncRepository(
    collection: IFirestoreCollection<UserDoc>,
    private val userDao: UserDao,
    dispatcher: CoroutineDispatcher = Dispatchers.IO
) : FirestoreSyncRepository<UserAccount, UserDoc>(collection, dispatcher) {

    override fun UserAccount.toFirestoreDoc(): UserDoc = UserDoc.fromEntity(this)
    override fun UserDoc.toEntity(): UserAccount = this.toEntity()
    override fun UserDoc.docId(): String = firebaseUid

    override suspend fun insertCache(entity: UserAccount) {
        userDao.insertUser(entity)
    }

    override suspend fun deleteCache(docId: String) {
        val user = userDao.getUserByFirebaseUid(docId)
        if (user != null) {
            userDao.deleteUser(user)
        }
    }

    override fun observeCache(): Flow<List<UserAccount>> = userDao.getAllUsers()
}
