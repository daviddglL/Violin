package com.violinmaster.app.di

import android.content.Context
import androidx.room.Room
import com.violinmaster.app.data.AssignmentDao
import com.violinmaster.app.data.ChatDao
import com.violinmaster.app.data.LessonDao
import com.violinmaster.app.data.PracticeDatabase
import com.violinmaster.app.data.SessionDao
import com.violinmaster.app.data.UserDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): PracticeDatabase {
        return Room.databaseBuilder(
            context.applicationContext,
            PracticeDatabase::class.java,
            "violin_master_database"
        )
            .addMigrations(
                PracticeDatabase.MIGRATION_2_3,
                PracticeDatabase.MIGRATION_3_4,
                PracticeDatabase.MIGRATION_4_5,
                PracticeDatabase.MIGRATION_5_6,
                PracticeDatabase.MIGRATION_6_7
            )
            .build()
    }

    @Provides
    fun provideSessionDao(database: PracticeDatabase): SessionDao {
        return database.sessionDao()
    }

    @Provides
    fun provideLessonDao(database: PracticeDatabase): LessonDao {
        return database.lessonDao()
    }

    @Provides
    fun provideUserDao(database: PracticeDatabase): UserDao {
        return database.userDao()
    }

    @Provides
    fun provideAssignmentDao(database: PracticeDatabase): AssignmentDao {
        return database.assignmentDao()
    }

    @Provides
    fun provideChatDao(database: PracticeDatabase): ChatDao {
        return database.chatDao()
    }
}
