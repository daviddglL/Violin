package com.violinmaster.app.di

import com.violinmaster.app.data.IChatRepository
import com.violinmaster.app.data.IPracticeRepository
import com.violinmaster.app.domain.usecase.CompleteAssignmentUseCase
import com.violinmaster.app.domain.usecase.GetAssignmentsUseCase
import com.violinmaster.app.domain.usecase.GetLeaderboardUseCase
import com.violinmaster.app.domain.usecase.GetMessagesUseCase
import com.violinmaster.app.domain.usecase.GetPracticeSessionsUseCase
import com.violinmaster.app.domain.usecase.LoginUseCase
import com.violinmaster.app.domain.usecase.RegisterUseCase
import com.violinmaster.app.domain.usecase.SavePracticeSessionUseCase
import com.violinmaster.app.domain.usecase.SendMessageUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module providing domain use cases as singletons.
 *
 * All 9 use cases are provided via @Provides @Singleton methods,
 * injecting their repository and manager dependencies.
 *
 * REQ-ARCH-008-S2: All use cases provided via Hilt.
 */
@Module
@InstallIn(SingletonComponent::class)
object UseCaseModule {

    @Provides
    @Singleton
    fun provideLoginUseCase(
        repository: IPracticeRepository,
        authManager: AuthManager
    ): LoginUseCase = LoginUseCase(repository, authManager)

    @Provides
    @Singleton
    fun provideRegisterUseCase(
        repository: IPracticeRepository
    ): RegisterUseCase = RegisterUseCase(repository)

    @Provides
    @Singleton
    fun provideSavePracticeSessionUseCase(
        repository: IPracticeRepository
    ): SavePracticeSessionUseCase = SavePracticeSessionUseCase(repository)

    @Provides
    @Singleton
    fun provideGetPracticeSessionsUseCase(
        repository: IPracticeRepository
    ): GetPracticeSessionsUseCase = GetPracticeSessionsUseCase(repository)

    @Provides
    @Singleton
    fun provideGetAssignmentsUseCase(
        repository: IPracticeRepository
    ): GetAssignmentsUseCase = GetAssignmentsUseCase(repository)

    @Provides
    @Singleton
    fun provideCompleteAssignmentUseCase(
        repository: IPracticeRepository,
        authManager: AuthManager
    ): CompleteAssignmentUseCase = CompleteAssignmentUseCase(repository, authManager)

    @Provides
    @Singleton
    fun provideSendMessageUseCase(
        chatRepository: IChatRepository,
        authManager: AuthManager
    ): SendMessageUseCase = SendMessageUseCase(chatRepository, authManager)

    @Provides
    @Singleton
    fun provideGetMessagesUseCase(
        chatRepository: IChatRepository
    ): GetMessagesUseCase = GetMessagesUseCase(chatRepository)

    @Provides
    @Singleton
    fun provideGetLeaderboardUseCase(
        repository: IPracticeRepository
    ): GetLeaderboardUseCase = GetLeaderboardUseCase(repository)
}
