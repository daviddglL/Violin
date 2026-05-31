package com.violinmaster.app.di

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.violinmaster.app.data.ChatRepository
import com.violinmaster.app.data.IChatRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module providing Firebase SDK singletons and repository bindings.
 *
 * REQ-CC-001: Provides FirebaseFirestore (with offline persistence enabled)
 * and FirebaseStorage as @Singleton instances.
 *
 * FirebaseApp is auto-initialized via google-services.json ContentProvider.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class FirebaseModule {

    @Binds
    @Singleton
    abstract fun bindChatRepository(impl: ChatRepository): IChatRepository

    companion object {
        @Provides
        @Singleton
        fun provideFirebaseFirestore(): FirebaseFirestore {
            // Firestore enables offline persistence by default on Android.
            // Explicit setPersistenceEnabled is deprecated as of firebase-bom 34.x.
            return FirebaseFirestore.getInstance()
        }

        @Provides
        @Singleton
        fun provideFirebaseStorage(): FirebaseStorage {
            return FirebaseStorage.getInstance()
        }
    }
}
