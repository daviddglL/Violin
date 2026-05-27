package com.violinmaster.app.di

import com.violinmaster.app.BuildConfig
import com.violinmaster.app.data.remote.GeminiApiService
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
import javax.inject.Named
import javax.inject.Singleton

/**
 * Hilt module providing network dependencies:
 * - OkHttpClient with logging interceptor
 * - Retrofit configured for generativelanguage.googleapis.com
 * - GeminiApiService (Retrofit-created proxy)
 * - GeminiRepository (wraps the API service)
 *
 * REQ-GEM-002: API key loaded from BuildConfig (Secrets Plugin → .env).
 * REQ-DI-001: All dependencies provided via Hilt module.
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }

        return OkHttpClient.Builder()
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
    ): GeminiRepository {
        return GeminiRepository(api)
    }

    @Provides
    @Singleton
    @Named("gemini_api_key")
    fun provideGeminiApiKey(): String {
        return BuildConfig.GEMINI_API_KEY
    }
}
