package com.violinmaster.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.violinmaster.app.audio.ViolinAudioEngine
import com.violinmaster.app.data.LessonProgress
import com.violinmaster.app.data.PracticeRepository
import com.violinmaster.app.data.PracticeSession
import com.violinmaster.app.di.SessionManager
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
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.util.Calendar
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import kotlin.random.Random

@HiltViewModel
class PracticeViewModel @Inject constructor(
    private val repository: PracticeRepository,
    private val sessionManager: SessionManager,
    private val audioEngine: ViolinAudioEngine
) : ViewModel() {

    // --- Configuration State ---
    private val _dailyGoalMinutes = MutableStateFlow(60)
    val dailyGoalMinutes: StateFlow<Int> = _dailyGoalMinutes.asStateFlow()

    fun updateDailyGoal(minutes: Int) {
        _dailyGoalMinutes.value = minutes
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
                    seedDefaultLessons()
                }
            }
        }

        loadDailyTasksCompleted()
    }

    private suspend fun seedDefaultLessons() {
        val defaults = listOf(
            LessonProgress("beg_1", "Posture & Open Strings Bowing", "Beginner", false, 0),
            LessonProgress("beg_2", "First Finger Patterns (D Major)", "Beginner", false, 0),
            LessonProgress("beg_3", "First Position Rhythms & Songs", "Beginner", false, 0),
            LessonProgress("int_1", "Relaxing Left Hand & Vibrato", "Intermediate", false, 0),
            LessonProgress("int_2", "Shifting to Third Position (III)", "Intermediate", false, 0),
            LessonProgress("int_3", "Double Stop Balance & Harmony", "Intermediate", false, 0),
            LessonProgress("adv_1", "Bowing Styles: Martelé, Spiccato", "Advanced", false, 0),
            LessonProgress("adv_2", "Paganini Practice Theme (A minor)", "Advanced", false, 0),
            LessonProgress("adv_3", "High Position Shifts (5th & 7th)", "Advanced", false, 0)
        )
        for (lesson in defaults) {
            repository.insertLessonProgress(lesson)
        }
    }

    // --- Timer Methods ---

    fun startPracticeTimer(category: String) {
        _practiceCategoryName.value = category
        _practiceElapsedSeconds.value = 0
        _isPracticing.value = true

        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                _practiceElapsedSeconds.value += 1
            }
        }
    }

    fun pausePracticeTimer() {
        timerJob?.cancel()
        timerJob = null
        _isPracticing.value = false
    }

    fun resumePracticeTimer() {
        if (!_isPracticing.value) {
            _isPracticing.value = true
            timerJob?.cancel()
            timerJob = viewModelScope.launch {
                while (true) {
                    delay(1000)
                    _practiceElapsedSeconds.value += 1
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
                repository.insertSession(newSession)

                val lessonsList = allLevelProgress.value
                val matchingLesson = lessonsList.firstOrNull { it.lessonTitle == category }
                if (matchingLesson != null) {
                    val finalSecs = matchingLesson.totalPracticedSeconds + seconds
                    val markedDone = finalSecs >= 300
                    val wasCompletedAlready = matchingLesson.completed
                    repository.insertLessonProgress(
                        matchingLesson.copy(
                            totalPracticedSeconds = finalSecs,
                            completed = markedDone || matchingLesson.completed,
                            lastPracticedTimestamp = System.currentTimeMillis()
                        )
                    )
                    if (markedDone && !wasCompletedAlready) {
                        earnPoints(150)
                    }
                }
            }
        }

        _practiceElapsedSeconds.value = 0
        _isPracticing.value = false
    }

    fun cancelPracticeTimer() {
        timerJob?.cancel()
        timerJob = null
        _practiceElapsedSeconds.value = 0
        _isPracticing.value = false
    }

    // --- Practice Data Methods ---

    fun generateDemoHistory() {
        viewModelScope.launch {
            repository.clearSessions()

            val calendar = Calendar.getInstance()
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

            val categories = listOf("Smart Tuner Tuning", "Advanced Bowing: Détaché & Martelé", "Open Strings Tuning & Bowing", "Metronome Scale Practice")

            for (dayOffset in -6..0) {
                calendar.time = Date()
                calendar.add(Calendar.DAY_OF_YEAR, dayOffset)
                val dayString = sdf.format(calendar.time)

                val logsForDayCount = Random.nextInt(1, 3)
                for (i in 0 until logsForDayCount) {
                    val practiceMin = Random.nextInt(10, 40)
                    val randCategory = categories.shuffled().first()
                    repository.insertSession(
                        PracticeSession(
                            dateString = dayString,
                            durationSeconds = practiceMin * 60,
                            category = randCategory,
                            timestamp = calendar.timeInMillis + (i * 3600 * 1000)
                        )
                    )
                }
            }

            val beg1 = repository.getLessonProgressById("beg_1")
            val adv1 = repository.getLessonProgressById("adv_1")

            if (beg1 != null) {
                repository.insertLessonProgress(
                    beg1.copy(totalPracticedSeconds = 900, completed = true, lastPracticedTimestamp = System.currentTimeMillis())
                )
            }
            if (adv1 != null) {
                repository.insertLessonProgress(
                    adv1.copy(totalPracticedSeconds = 480, completed = true, lastPracticedTimestamp = System.currentTimeMillis() - 86400000)
                )
            }
        }
    }

    fun toggleLessonStatus(lessonId: String, completed: Boolean) {
        viewModelScope.launch {
            val current = repository.getLessonProgressById(lessonId)
            repository.updateLessonCompletion(lessonId, completed)
            if (completed && current?.completed == false) {
                earnPoints(150)
            }
        }
    }

    fun deleteSession(sessionId: Int) {
        viewModelScope.launch {
            repository.deleteSession(sessionId)
        }
    }

    // --- Scoring Methods ---

    fun loadDailyTasksCompleted() {
        val today = LocalDate.now().toString()
        val completedSet = sessionManager.getDailyTasksCompleted(today)
        _dailyTasksCompleted.value = completedSet
    }

    fun completeDailyTask(taskId: String, attempts: Int) {
        val today = LocalDate.now().toString()
        val currentCompleted = _dailyTasksCompleted.value.toMutableSet()
        if (currentCompleted.add(taskId)) {
            _dailyTasksCompleted.value = currentCompleted
            sessionManager.saveDailyTaskCompleted(today, currentCompleted)

            val pointsEarned = when (attempts) {
                1 -> 100
                2 -> 75
                3 -> 50
                else -> 25
            }
            earnPoints(pointsEarned)
        }
    }

    fun earnPoints(additionalPoints: Int) {
        val userVal = sessionManager.currentUser.value ?: return
        viewModelScope.launch {
            val updatedUser = userVal.copy(points = userVal.points + additionalPoints)
            repository.insertUser(updatedUser)
            sessionManager.restoreCurrentUser(updatedUser)
        }
    }

    fun updateSkillLevel(level: String) {
        val userVal = sessionManager.currentUser.value ?: return
        viewModelScope.launch {
            val updatedUser = userVal.copy(skillLevel = level)
            repository.insertUser(updatedUser)
            sessionManager.restoreCurrentUser(updatedUser)
        }
    }
}
