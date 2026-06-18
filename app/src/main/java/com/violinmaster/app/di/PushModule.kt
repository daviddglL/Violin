package com.violinmaster.app.di

import com.google.firebase.messaging.FirebaseMessaging
import com.violinmaster.app.push.NotificationHelper
import com.violinmaster.app.push.PushTokenManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object PushModule {

    @Provides
    @Singleton
    fun provideFirebaseMessaging(): FirebaseMessaging = FirebaseMessaging.getInstance()

    @Provides
    @Singleton
    fun providePushTokenManager(
        firebaseMessaging: FirebaseMessaging,
        userSyncRepository: com.violinmaster.app.data.firebase.UserSyncRepository
    ): PushTokenManager = PushTokenManager(firebaseMessaging, userSyncRepository)
}
