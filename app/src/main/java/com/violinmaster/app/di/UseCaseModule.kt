package com.violinmaster.app.di

import com.violinmaster.app.data.IChatRepository
import com.violinmaster.app.data.IPracticeRepository
import com.violinmaster.app.domain.usecase.CompleteAssignmentUseCase
import com.violinmaster.app.domain.usecase.DeleteAssignmentUseCase
import com.violinmaster.app.domain.usecase.DeletePracticeSessionUseCase
import com.violinmaster.app.domain.usecase.EarnPointsUseCase
import com.violinmaster.app.domain.usecase.GenerateDemoHistoryUseCase
import com.violinmaster.app.domain.usecase.GetAssignmentsUseCase
import com.violinmaster.app.domain.usecase.GetLeaderboardUseCase
import com.violinmaster.app.domain.usecase.GetMessagesUseCase
import com.violinmaster.app.domain.usecase.GetPracticeSessionsUseCase
import com.violinmaster.app.domain.usecase.LoginUseCase
import com.violinmaster.app.domain.usecase.PublishAssignmentUseCase
import com.violinmaster.app.domain.usecase.RegisterUseCase
import com.violinmaster.app.domain.usecase.ResetPinUseCase
import com.violinmaster.app.domain.usecase.SavePracticeSessionUseCase
import com.violinmaster.app.domain.usecase.SeedDefaultLessonsUseCase
import com.violinmaster.app.domain.usecase.SendMessageUseCase
import com.violinmaster.app.domain.usecase.SetRecoveryQuestionUseCase
import com.violinmaster.app.domain.usecase.ToggleLessonStatusUseCase
import com.violinmaster.app.domain.usecase.UpdateLessonProgressUseCase
import com.violinmaster.app.domain.usecase.UpdateSkillLevelUseCase
import com.violinmaster.app.domain.usecase.VerifyRecoveryAnswerUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module providing domain use cases as singletons.
 *
 * All 18 use cases are provided via @Provides @Singleton methods,
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

    @Provides
    @Singleton
    fun providePublishAssignmentUseCase(
        repository: IPracticeRepository,
        authManager: AuthManager
    ): PublishAssignmentUseCase = PublishAssignmentUseCase(repository, authManager)

    @Provides
    @Singleton
    fun provideDeleteAssignmentUseCase(
        repository: IPracticeRepository
    ): DeleteAssignmentUseCase = DeleteAssignmentUseCase(repository)

    @Provides
    @Singleton
    fun provideUpdateLessonProgressUseCase(
        repository: IPracticeRepository
    ): UpdateLessonProgressUseCase = UpdateLessonProgressUseCase(repository)

    @Provides
    @Singleton
    fun provideDeletePracticeSessionUseCase(
        repository: IPracticeRepository
    ): DeletePracticeSessionUseCase = DeletePracticeSessionUseCase(repository)

    @Provides
    @Singleton
    fun provideSeedDefaultLessonsUseCase(
        repository: IPracticeRepository
    ): SeedDefaultLessonsUseCase = SeedDefaultLessonsUseCase(repository)

    @Provides
    @Singleton
    fun provideGenerateDemoHistoryUseCase(
        repository: IPracticeRepository
    ): GenerateDemoHistoryUseCase = GenerateDemoHistoryUseCase(repository)

    @Provides
    @Singleton
    fun provideEarnPointsUseCase(
        repository: IPracticeRepository,
        authManager: AuthManager
    ): EarnPointsUseCase = EarnPointsUseCase(repository, authManager)

    @Provides
    @Singleton
    fun provideUpdateSkillLevelUseCase(
        repository: IPracticeRepository,
        authManager: AuthManager
    ): UpdateSkillLevelUseCase = UpdateSkillLevelUseCase(repository, authManager)

    @Provides
    @Singleton
    fun provideToggleLessonStatusUseCase(
        repository: IPracticeRepository,
        authManager: AuthManager
    ): ToggleLessonStatusUseCase = ToggleLessonStatusUseCase(repository, authManager)

    @Provides
    @Singleton
    fun provideSetRecoveryQuestionUseCase(
        repository: IPracticeRepository
    ): SetRecoveryQuestionUseCase = SetRecoveryQuestionUseCase(repository)

    @Provides
    @Singleton
    fun provideVerifyRecoveryAnswerUseCase(
        repository: IPracticeRepository
    ): VerifyRecoveryAnswerUseCase = VerifyRecoveryAnswerUseCase(repository)

    @Provides
    @Singleton
    fun provideResetPinUseCase(
        repository: IPracticeRepository,
        authManager: AuthManager
    ): ResetPinUseCase = ResetPinUseCase(repository, authManager)
}
