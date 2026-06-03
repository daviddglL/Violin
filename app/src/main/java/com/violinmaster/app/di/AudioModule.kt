package com.violinmaster.app.di

import android.content.Context
import com.violinmaster.app.audio.TunerEngine
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AudioModule {

    @Provides
    @Singleton
    fun provideTunerEngine(): TunerEngine {
        return TunerEngine()
    }

    @Provides
    @Singleton
    fun provideTuningPreferencesManager(
        @ApplicationContext context: Context,
        authManager: AuthManager
    ): TuningPreferencesManager {
        return TuningPreferencesManager(context, authManager)
    }
}
