package com.violinmaster.app.di

import com.violinmaster.app.data.PracticeDao
import com.violinmaster.app.data.PracticeRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun providePracticeRepository(dao: PracticeDao): PracticeRepository {
        return PracticeRepository(dao)
    }
}
