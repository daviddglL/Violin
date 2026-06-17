package com.violinmaster.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.violinmaster.app.audio.ViolinAudioEngine
import com.violinmaster.app.data.AnalyticsHelper
import com.violinmaster.app.data.LessonProgress
import com.violinmaster.app.data.IPracticeRepository
import com.violinmaster.app.data.PracticeSession
import com.violinmaster.app.di.AuthManager
import com.violinmaster.app.di.UserPreferencesManager
import com.violinmaster.app.domain.usecase.DeletePracticeSessionUseCase
import com.violinmaster.app.domain.usecase.EarnPointsUseCase
import com.violinmaster.app.domain.usecase.GenerateDemoHistoryUseCase
import com.violinmaster.app.domain.usecase.GetPracticeSessionsUseCase
import com.violinmaster.app.domain.usecase.SavePracticeSessionUseCase
import com.violinmaster.app.domain.usecase.SeedDefaultLessonsUseCase
import com.violinmaster.app.domain.usecase.ToggleLessonStatusUseCase
import com.violinmaster.app.domain.usecase.UpdateLessonProgressUseCase
import com.violinmaster.app.domain.usecase.UpdateSkillLevelUseCase
import com.violinmaster.app.domain.util.ScoringPolicy
import com.violinmaster.app.ui.state.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

/**
 * Content data class for PracticeViewModel UiState.
 *
 * Groups core practice screen data into a single value object.
 */
data class PracticeContent(
    val isPracticing: Boolean = false,
    val practiceCategoryName: String = "General Practice",
    val practiceElapsedSeconds: Int = 0,
    val dailyGoalMinutes: Int = 60,
    val dailyTasksCompleted: Set<String> = emptySet()
)

@HiltViewModel
class PracticeViewModel @Inject constructor(
    private val repository: IPracticeRepository,
    private val authManager: AuthManager,
    private val userPreferencesManager: UserPreferencesManager,
    private val analyticsHelper: AnalyticsHelper,
    private val audioEngine: ViolinAudioEngine,
    private val savePracticeSessionUseCase: SavePracticeSessionUseCase,
    private val getPracticeSessionsUseCase: GetPracticeSessionsUseCase,
    private val updateLessonProgressUseCase: UpdateLessonProgressUseCase,
    private val generateDemoHistoryUseCase: GenerateDemoHistoryUseCase,
    private val toggleLessonStatusUseCase: ToggleLessonStatusUseCase,
    private val deletePracticeSessionUseCase: DeletePracticeSessionUseCase,
    private val seedDefaultLessonsUseCase: SeedDefaultLessonsUseCase,
    private val earnPointsUseCase: EarnPointsUseCase,
    private val updateSkillLevelUseCase: UpdateSkillLevelUseCase
) : ViewModel() {

    // --- Configuration State ---
    private val _dailyGoalMinutes = MutableStateFlow(60)
    val dailyGoalMinutes: StateFlow<Int> = _dailyGoalMinutes.asStateFlow()

    // ── Unified UiState (REQ-UISTATE-001) ──────────────────────────
    private val _uiState = MutableStateFlow<UiState<PracticeContent>>(
        UiState.Content(PracticeContent())
    )
    val uiState: StateFlow<UiState<PracticeContent>> = _uiState.asStateFlow()

    private fun syncUiState() {
        _uiState.value = UiState.Content(
            PracticeContent(
                isPracticing = _isPracticing.value,
                practiceCategoryName = _practiceCategoryName.value,
                practiceElapsedSeconds = _practiceElapsedSeconds.value,
                dailyGoalMinutes = _dailyGoalMinutes.value,
                dailyTasksCompleted = _dailyTasksCompleted.value
            )
        )
    }

    fun updateDailyGoal(minutes: Int) {
        _dailyGoalMinutes.value = minutes
        syncUiState()
    }

    // --- Database Flows ---
    val allSessions: StateFlow<List<PracticeSession>>
    val allLevelProgress: StateFlow<List<LessonProgress>>

    private val _todayDateString = MutableStateFlow(LocalDate.now().toString())
    val todayDateString: StateFlow<String> = _todayDateString.asStateFlow()

    val todayFinishedSeconds: StateFlow<Int>

    // --- Scoring & Daily Tasks ---
    private val _dailyTasksCompleted = MutableStateFlow<Set<String>>(emptySet())
    val dailyTasksCompleted: StateFlow<Set<String>> = _dailyTasksCompleted.asStateFlow()

    // --- Active Practice Timer State ---
    private val _isPracticing = MutableStateFlow(false)
    val isPracticing: StateFlow<Boolean> = _isPracticing.asStateFlow()

    private val _practiceCategoryName = MutableStateFlow("General Practice")
    val practiceCategoryName: StateFlow<String> = _practiceCategoryName.asStateFlow()

    private val _practiceElapsedSeconds = MutableStateFlow(0)
    val practiceElapsedSeconds: StateFlow<Int> = _practiceElapsedSeconds.asStateFlow()

    private var timerJob: Job? = null

    init {
        allSessions = repository.allSessions.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        allLevelProgress = repository.allLevelProgress.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        todayFinishedSeconds = combine(allSessions, _todayDateString) { sessionsList, today ->
            sessionsList.filter { it.dateString == today }.sumOf { it.durationSeconds }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

        viewModelScope.launch {
            repository.allLevelProgress.collect { list ->
                if (list.isEmpty()) {
                    seedDefaultLessonsUseCase()
                }
            }
        }

        loadDailyTasksCompleted()
    }

    // --- Timer Methods ---

    fun startPracticeTimer(category: String) {
        _practiceCategoryName.value = category
        _practiceElapsedSeconds.value = 0
        _isPracticing.value = true
        syncUiState()

        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                _practiceElapsedSeconds.value += 1
                syncUiState()
            }
        }
    }

    fun pausePracticeTimer() {
        timerJob?.cancel()
        timerJob = null
        _isPracticing.value = false
        syncUiState()
    }

    fun resumePracticeTimer() {
        if (!_isPracticing.value) {
            _isPracticing.value = true
            syncUiState()
            timerJob?.cancel()
            timerJob = viewModelScope.launch {
                while (true) {
                    delay(1000)
                    _practiceElapsedSeconds.value += 1
                    syncUiState()
                }
            }
        }
    }

    fun stopAndSavePracticeSession() {
        timerJob?.cancel()
        timerJob = null

        val seconds = _practiceElapsedSeconds.value
        val category = _practiceCategoryName.value

        if (seconds >= 3) {
            viewModelScope.launch {
                val newSession = PracticeSession(
                    dateString = LocalDate.now().toString(),
                    durationSeconds = seconds,
                    category = category
                )
                savePracticeSessionUseCase.invoke(newSession)

                val lessonsList = allLevelProgress.value
                val matchingLesson = lessonsList.firstOrNull { it.lessonTitle == category }
                if (matchingLesson != null) {
                    val newlyCompleted = updateLessonProgressUseCase(
                        matchingLesson.lessonId,
                        seconds
                    )
                    if (newlyCompleted) {
                        earnPointsUseCase(ScoringPolicy.LESSON_COMPLETION_POINTS)
                    }
                }
            }
        }

        _practiceElapsedSeconds.value = 0
        _isPracticing.value = false
        syncUiState()
    }

    fun cancelPracticeTimer() {
        timerJob?.cancel()
        timerJob = null
        _practiceElapsedSeconds.value = 0
        _isPracticing.value = false
        syncUiState()
    }

    // --- Practice Data Methods ---

    fun generateDemoHistory() {
        viewModelScope.launch {
            generateDemoHistoryUseCase()
        }
    }

    fun toggleLessonStatus(lessonId: String, completed: Boolean) {
        viewModelScope.launch {
            toggleLessonStatusUseCase(lessonId, completed)
        }
    }

    fun deleteSession(sessionId: Int) {
        viewModelScope.launch {
            deletePracticeSessionUseCase(sessionId)
        }
    }

    // --- Scoring Methods ---

    fun loadDailyTasksCompleted() {
        val today = LocalDate.now().toString()
        val completedSet = userPreferencesManager.getDailyTasksCompleted(today)
        _dailyTasksCompleted.value = completedSet
    }

    fun completeDailyTask(taskId: String, attempts: Int) {
        val today = LocalDate.now().toString()
        val currentCompleted = _dailyTasksCompleted.value.toMutableSet()
        if (currentCompleted.add(taskId)) {
            _dailyTasksCompleted.value = currentCompleted
            syncUiState()
            userPreferencesManager.saveDailyTaskCompleted(today, currentCompleted)

            val pointsEarned = ScoringPolicy.pointsForTaskCompletion(attempts)
            viewModelScope.launch {
                earnPointsUseCase(pointsEarned)
            }
        }
    }

    fun earnPoints(additionalPoints: Int) {
        viewModelScope.launch {
            earnPointsUseCase(additionalPoints)
        }
    }

    fun updateSkillLevel(level: String, quizScore: Int = -1) {
        viewModelScope.launch {
            updateSkillLevelUseCase(level, quizScore)
        }
    }
}
