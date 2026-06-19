package com.violinmaster.app.di

import com.violinmaster.app.data.FirebasePerformanceService
import com.violinmaster.app.data.IPerformanceService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object PerformanceModule {

    @Provides
    @Singleton
    fun providePerformanceService(): IPerformanceService {
        return FirebasePerformanceService()
    }
}
