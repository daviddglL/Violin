package com.violinmaster.app.di

import com.violinmaster.app.domain.util.CircuitBreaker
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object CircuitBreakerModule {

    @Provides
    @Singleton
    fun provideGeminiCircuitBreaker(): CircuitBreaker {
        return CircuitBreaker(
            failureThreshold = 3,
            resetTimeoutMs = 30_000L // 30 seconds
        )
    }
}
