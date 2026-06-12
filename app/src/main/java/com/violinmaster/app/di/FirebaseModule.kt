package com.violinmaster.app.di

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.violinmaster.app.data.AssignmentDao
import com.violinmaster.app.data.ChatRepository
import com.violinmaster.app.data.IChatRepository
import com.violinmaster.app.data.LessonDao
import com.violinmaster.app.data.SessionDao
import com.violinmaster.app.data.UserDao
import com.violinmaster.app.data.firebase.AssignmentDoc
import com.violinmaster.app.data.firebase.AssignmentSyncRepository
import com.violinmaster.app.data.firebase.FirebaseCollections
import com.violinmaster.app.data.firebase.FirestoreCollection
import com.violinmaster.app.data.firebase.FirestoreUsers
import com.violinmaster.app.data.firebase.IFirestoreCollection
import com.violinmaster.app.data.firebase.IFirestoreUsers
import com.violinmaster.app.data.firebase.LessonDoc
import com.violinmaster.app.data.firebase.LessonSyncRepository
import com.violinmaster.app.data.firebase.SessionDoc
import com.violinmaster.app.data.firebase.SessionSyncRepository
import com.violinmaster.app.data.firebase.UserDoc
import com.violinmaster.app.data.firebase.UserSyncRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module providing Firebase SDK singletons, Firestore collection
 * abstractions, sync repositories, and repository bindings.
 *
 * REQ-CSYNC-001: IFirestoreCollection<T> bindings for test substitution.
 * REQ-DI-009: FirestoreSyncRepository<T> singletons for all 4 entities.
 *
 * FirebaseApp is auto-initialized via google-services.json ContentProvider.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class FirebaseModule {

    @Binds
    @Singleton
    abstract fun bindChatRepository(impl: ChatRepository): IChatRepository

    @Binds
    @Singleton
    abstract fun bindFirestoreUsers(impl: FirestoreUsers): IFirestoreUsers

    companion object {
        // ── Firebase SDK Singletons ────────────────────────────────────────

        @Provides
        @Singleton
        fun provideFirebaseFirestore(): FirebaseFirestore {
            // Firestore enables offline persistence by default on Android.
            return FirebaseFirestore.getInstance()
        }

        @Provides
        @Singleton
        fun provideFirebaseStorage(): FirebaseStorage {
            return FirebaseStorage.getInstance()
        }

        // ── IFirestoreCollection<T> Bindings ───────────────────────────────

        @Provides
        @Singleton
        fun provideSessionCollection(
            firestore: FirebaseFirestore,
            authManager: AuthManager
        ): IFirestoreCollection<SessionDoc> {
            return FirestoreCollection(
                firestore = firestore,
                pathProvider = {
                    val uid = authManager.currentUser.value?.firebaseUid ?: "unknown"
                    "users/$uid/sessions"
                },
                docClass = SessionDoc::class.java
            )
        }

        @Provides
        @Singleton
        fun provideLessonCollection(
            firestore: FirebaseFirestore
        ): IFirestoreCollection<LessonDoc> {
            return FirestoreCollection(
                firestore = firestore,
                pathProvider = { "lesson_progress" },
                docClass = LessonDoc::class.java
            )
        }

        @Provides
        @Singleton
        fun provideUserCollection(
            firestore: FirebaseFirestore
        ): IFirestoreCollection<UserDoc> {
            return FirestoreCollection(
                firestore = firestore,
                pathProvider = { "users" },
                docClass = UserDoc::class.java
            )
        }

        @Provides
        @Singleton
        fun provideAssignmentCollection(
            firestore: FirebaseFirestore
        ): IFirestoreCollection<AssignmentDoc> {
            return FirestoreCollection(
                firestore = firestore,
                pathProvider = { FirebaseCollections.ASSIGNMENTS },
                docClass = AssignmentDoc::class.java
            )
        }

        // ── FirestoreSyncRepository<T> Singletons ──────────────────────────

        @Provides
        @Singleton
        fun provideSessionSyncRepository(
            collection: IFirestoreCollection<SessionDoc>,
            sessionDao: SessionDao
        ): SessionSyncRepository {
            return SessionSyncRepository(collection, sessionDao)
        }

        @Provides
        @Singleton
        fun provideLessonSyncRepository(
            collection: IFirestoreCollection<LessonDoc>,
            lessonDao: LessonDao
        ): LessonSyncRepository {
            return LessonSyncRepository(collection, lessonDao)
        }

        @Provides
        @Singleton
        fun provideUserSyncRepository(
            collection: IFirestoreCollection<UserDoc>,
            userDao: UserDao
        ): UserSyncRepository {
            return UserSyncRepository(collection, userDao)
        }

        @Provides
        @Singleton
        fun provideAssignmentSyncRepository(
            collection: IFirestoreCollection<AssignmentDoc>,
            assignmentDao: AssignmentDao
        ): AssignmentSyncRepository {
            return AssignmentSyncRepository(collection, assignmentDao)
        }
    }
}
