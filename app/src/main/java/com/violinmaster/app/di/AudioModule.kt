package com.violinmaster.app.di

import com.violinmaster.app.audio.TunerEngine
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
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
}
