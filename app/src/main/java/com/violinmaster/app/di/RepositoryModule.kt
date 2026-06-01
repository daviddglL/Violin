package com.violinmaster.app.di

import com.violinmaster.app.data.AssignmentDao
import com.violinmaster.app.data.IPracticeRepository
import com.violinmaster.app.data.LessonDao
import com.violinmaster.app.data.PracticeRepository
import com.violinmaster.app.data.SessionDao
import com.violinmaster.app.data.UserDao
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindPracticeRepository(impl: PracticeRepository): IPracticeRepository

    companion object {
        @Provides
        @Singleton
        fun providePracticeRepository(
            sessionDao: SessionDao,
            lessonDao: LessonDao,
            userDao: UserDao,
            assignmentDao: AssignmentDao
        ): PracticeRepository {
            return PracticeRepository(sessionDao, lessonDao, userDao, assignmentDao)
        }
    }
}
