package com.violinmaster.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.violinmaster.app.data.Assignment
import com.violinmaster.app.data.PracticeRepository
import com.violinmaster.app.data.UserAccount
import com.violinmaster.app.di.SessionManager
import com.violinmaster.app.security.VideoSecurityService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AssignmentViewModel @Inject constructor(
    private val repository: PracticeRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    // --- Student / Teacher Assignments flows ---
    private val _studentAssignments = MutableStateFlow<List<Assignment>>(emptyList())
    val studentAssignments: StateFlow<List<Assignment>> = _studentAssignments.asStateFlow()

    private val _teacherAssignments = MutableStateFlow<List<Assignment>>(emptyList())
    val teacherAssignments: StateFlow<List<Assignment>> = _teacherAssignments.asStateFlow()

    val allUsers: StateFlow<List<UserAccount>>

    init {
        allUsers = repository.allUsers.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        var assignmentsJob: Job? = null
        viewModelScope.launch {
            sessionManager.currentUser.collect { user ->
                assignmentsJob?.cancel()
                if (user != null) {
                    assignmentsJob = viewModelScope.launch {
                        if (user.role == "TEACHER") {
                            repository.getAssignmentsByTeacher(user.teacherCode).collect { list ->
                                _teacherAssignments.value = list
                            }
                        } else if (user.role == "STUDENT") {
                            val code = user.teacherCode
                            repository.allAssignments.collect { list ->
                                _studentAssignments.value = list.filter {
                                    it.studentUsername.equals(user.username, ignoreCase = true) ||
                                            (it.studentUsername == "ALL" && it.teacherUsername == code)
                                }
                            }
                        }
                    }
                } else {
                    _studentAssignments.value = emptyList()
                    _teacherAssignments.value = emptyList()
                }
            }
        }
    }

    fun publishAssignment(title: String, description: String, targetStudent: String, videoTitle: String, durationSeconds: Int) {
        val userVal = sessionManager.currentUser.value ?: return
        if (userVal.role != "TEACHER") return

        viewModelScope.launch {
            val secureVideoUrl = if (videoTitle.isNotEmpty()) {
                VideoSecurityService.obtainSecureSignedUrl("vid_dynamic_tutor_" + System.currentTimeMillis() % 1000, "session_token_master")
            } else ""

            val assignment = Assignment(
                title = title,
                description = description,
                teacherUsername = userVal.teacherCode,
                studentUsername = targetStudent,
                videoTitle = videoTitle,
                videoDurationSeconds = durationSeconds,
                videoResourceUrl = secureVideoUrl
            )
            repository.insertAssignment(assignment)
        }
    }

    fun markAssignmentComplete(assignmentId: Int, completed: Boolean) {
        viewModelScope.launch {
            repository.updateAssignmentCompletion(assignmentId, completed)
            if (completed) {
                // Award 200 points via repository
                val userVal = sessionManager.currentUser.value
                if (userVal != null) {
                    val updatedUser = userVal.copy(points = userVal.points + 200)
                    repository.insertUser(updatedUser)
                    sessionManager.restoreCurrentUser(updatedUser)
                }
            }
        }
    }

    fun deleteAssignment(assignmentId: Int) {
        viewModelScope.launch {
            repository.deleteAssignmentById(assignmentId)
        }
    }
}
