package com.violinmaster.app.di

import com.violinmaster.app.data.FirebaseCrashReportingService
import com.violinmaster.app.data.ICrashReportingService
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module providing crash-reporting dependencies.
 *
 * REQ-CRASH-001: Crash reporting service initialized as DI singleton.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class CrashReportingModule {

    @Binds
    @Singleton
    abstract fun bindCrashReportingService(
        impl: FirebaseCrashReportingService
    ): ICrashReportingService
}
