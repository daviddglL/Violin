package com.violinmaster.app.di

import com.violinmaster.app.data.AssignmentDao
import com.violinmaster.app.data.CloudConfig
import com.violinmaster.app.data.IGeminiRepository
import com.violinmaster.app.data.IPracticeRepository
import com.violinmaster.app.data.LessonDao
import com.violinmaster.app.data.PracticeRepository
import com.violinmaster.app.data.SessionDao
import com.violinmaster.app.data.UserDao
import com.violinmaster.app.data.auth.GoogleAuthRepository
import com.violinmaster.app.data.auth.IGoogleAuthRepository
import com.violinmaster.app.data.firebase.AssignmentSyncRepository
import com.violinmaster.app.data.firebase.LessonSyncRepository
import com.violinmaster.app.data.firebase.SessionSyncRepository
import com.violinmaster.app.data.firebase.UserSyncRepository
import com.violinmaster.app.data.remote.GeminiRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module providing repository bindings and singleton instances.
 *
 * REQ-DI-003: PracticeRepository receives Firestore sync repos + Room DAOs.
 * REQ-DI-009: IPracticeRepository interface binding preserved.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindPracticeRepository(impl: PracticeRepository): IPracticeRepository

    @Binds
    @Singleton
    abstract fun bindGeminiRepository(impl: GeminiRepository): IGeminiRepository

    @Binds
    @Singleton
    abstract fun bindGoogleAuthRepository(impl: GoogleAuthRepository): IGoogleAuthRepository

    companion object {
        @Provides
        @Singleton
        fun provideCloudConfig(): CloudConfig = CloudConfig()

        @Provides
        @Singleton
        fun providePracticeRepository(
            sessionSync: SessionSyncRepository,
            lessonSync: LessonSyncRepository,
            userSync: UserSyncRepository,
            assignmentSync: AssignmentSyncRepository,
            sessionDao: SessionDao,
            lessonDao: LessonDao,
            userDao: UserDao,
            assignmentDao: AssignmentDao,
            cloudConfig: CloudConfig
        ): PracticeRepository {
            return PracticeRepository(
                sessionSync = sessionSync,
                lessonSync = lessonSync,
                userSync = userSync,
                assignmentSync = assignmentSync,
                sessionDao = sessionDao,
                lessonDao = lessonDao,
                userDao = userDao,
                assignmentDao = assignmentDao,
                cloudConfig = cloudConfig
            )
        }
    }
}
