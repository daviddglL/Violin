package com.violinmaster.app.push

import com.google.firebase.messaging.FirebaseMessaging
import com.violinmaster.app.data.firebase.UserSyncRepository
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PushTokenManager @Inject constructor(
    private val firebaseMessaging: FirebaseMessaging,
    private val userSyncRepository: UserSyncRepository
) {
    suspend fun registerToken(userFirebaseUid: String) {
        try {
            val token = firebaseMessaging.token.await()
            userSyncRepository.updateFcmToken(userFirebaseUid, token)
        } catch (e: Exception) {
            // Token registration failed — will retry on next token refresh
            // FirebaseMessagingService.onNewToken handles server-initiated refresh
        }
    }

    suspend fun getCurrentToken(): String? {
        return try {
            firebaseMessaging.token.await()
        } catch (e: Exception) {
            null
        }
    }
}
