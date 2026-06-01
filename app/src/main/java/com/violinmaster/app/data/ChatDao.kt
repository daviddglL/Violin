package com.violinmaster.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.violinmaster.app.data.local.CachedMessage
import kotlinx.coroutines.flow.Flow

/**
 * DAO for cached chat messages (Room offline cache).
 *
 * REQ-ARCH-004-S4: ChatDao manages cached_messages table.
 * Extracted from the monolithic PracticeDao.
 */
@Dao
interface ChatDao {

    /**
     * Returns cached messages for a given assignment, ordered by timestamp
     * ascending (oldest first). REQ-CHAT-004: Room cache for offline access.
     */
    @Query("SELECT * FROM cached_messages WHERE assignmentId = :assignmentId ORDER BY timestamp ASC")
    fun getCachedMessagesByAssignment(assignmentId: String): Flow<List<CachedMessage>>

    /**
     * Inserts or replaces cached messages. OnConflictStrategy.REPLACE ensures
     * Firestore snapshot updates overwrite stale cached entries.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCachedMessages(messages: List<CachedMessage>)

    /**
     * Removes all cached messages for a given assignment.
     * Called when assignment is deleted or chat is cleared.
     */
    @Query("DELETE FROM cached_messages WHERE assignmentId = :assignmentId")
    suspend fun clearCachedMessagesForAssignment(assignmentId: String)
}
