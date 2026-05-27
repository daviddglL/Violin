package com.violinmaster.app.di

import android.content.Context
import androidx.room.Room
import com.violinmaster.app.data.PracticeDao
import com.violinmaster.app.data.PracticeDatabase
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
            .addMigrations(PracticeDatabase.MIGRATION_2_3, PracticeDatabase.MIGRATION_3_4, PracticeDatabase.MIGRATION_4_5)
            .build()
    }

    @Provides
    fun providePracticeDao(database: PracticeDatabase): PracticeDao {
        return database.practiceDao()
    }
}
