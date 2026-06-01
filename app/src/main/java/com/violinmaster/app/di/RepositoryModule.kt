package com.violinmaster.app.di

import com.violinmaster.app.data.IPracticeRepository
import com.violinmaster.app.data.PracticeDao
import com.violinmaster.app.data.PracticeRepository
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
        fun providePracticeRepository(dao: PracticeDao): PracticeRepository {
            return PracticeRepository(dao)
        }
    }
}
