package com.violinmaster.app.di

import com.violinmaster.app.BuildConfig
import com.violinmaster.app.data.IGeminiRepository
import com.violinmaster.app.data.remote.GeminiApiService
import com.violinmaster.app.data.remote.GeminiAuthInterceptor
import com.violinmaster.app.data.remote.GeminiRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

/**
 * Hilt module providing network dependencies:
 * - OkHttpClient with logging + Gemini OAuth interceptors
 * - Retrofit configured for generativelanguage.googleapis.com
 * - GeminiApiService (Retrofit-created proxy)
 * - GeminiRepository (wraps the API service)
 *
 * REQ-GEM-002: Gemini auth via user OAuth token (GeminiAuthInterceptor).
 * REQ-DI-001: All dependencies provided via Hilt module.
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(
        geminiAuthInterceptor: GeminiAuthInterceptor
    ): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }

        return OkHttpClient.Builder()
            .addInterceptor(geminiAuthInterceptor)
            .addInterceptor(logging)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://generativelanguage.googleapis.com/")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideGeminiApiService(retrofit: Retrofit): GeminiApiService {
        return retrofit.create(GeminiApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideGeminiRepository(
        api: GeminiApiService
    ): IGeminiRepository {
        return GeminiRepository(api)
    }
}
