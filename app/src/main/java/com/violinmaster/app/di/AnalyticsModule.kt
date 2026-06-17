package com.violinmaster.app.di

import com.google.firebase.analytics.FirebaseAnalytics
import com.violinmaster.app.BuildConfig
import com.violinmaster.app.data.FirebaseAnalyticsService
import com.violinmaster.app.data.IAnalyticsService
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import android.content.Context
import javax.inject.Singleton

/**
 * Hilt module providing analytics dependencies:
 * - FirebaseAnalytics singleton
 * - IAnalyticsService binding
 *
 * REQ-ANALYTICS-001: Analytics service initialized as DI singleton.
 * REQ-ANALYTICS-005: Analytics collection enabled in debug only.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class AnalyticsModule {

    @Binds
    @Singleton
    abstract fun bindAnalyticsService(
        impl: FirebaseAnalyticsService
    ): IAnalyticsService

    companion object {
        @Provides
        @Singleton
        fun provideFirebaseAnalytics(
            @ApplicationContext context: Context
        ): FirebaseAnalytics {
            val analytics = FirebaseAnalytics.getInstance(context)
            if (BuildConfig.DEBUG) {
                analytics.setAnalyticsCollectionEnabled(true)
            }
            return analytics
        }
    }
}
