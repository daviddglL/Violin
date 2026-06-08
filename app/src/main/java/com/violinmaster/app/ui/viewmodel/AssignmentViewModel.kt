package com.violinmaster.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.violinmaster.app.data.Assignment
import com.violinmaster.app.data.IPracticeRepository
import com.violinmaster.app.data.UserAccount
import com.violinmaster.app.di.AuthManager
import com.violinmaster.app.domain.usecase.CompleteAssignmentUseCase
import com.violinmaster.app.domain.usecase.DeleteAssignmentUseCase
import com.violinmaster.app.domain.usecase.GetAssignmentsUseCase
import com.violinmaster.app.domain.usecase.PublishAssignmentUseCase
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
    private val repository: IPracticeRepository,
    private val authManager: AuthManager,
    private val getAssignmentsUseCase: GetAssignmentsUseCase,
    private val completeAssignmentUseCase: CompleteAssignmentUseCase,
    private val publishAssignmentUseCase: PublishAssignmentUseCase,
    private val deleteAssignmentUseCase: DeleteAssignmentUseCase
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
            authManager.currentUser.collect { user ->
                assignmentsJob?.cancel()
                if (user != null) {
                    assignmentsJob = viewModelScope.launch {
                        getAssignmentsUseCase(user.username, user.role, user.teacherCode).collect { list ->
                            if (user.role == "TEACHER") {
                                _teacherAssignments.value = list
                            } else if (user.role == "STUDENT") {
                                _studentAssignments.value = list
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

    fun publishAssignment(
        title: String,
        description: String,
        targetStudent: String,
        videoTitle: String,
        durationSeconds: Int,
        videoUrl: String = ""
    ) {
        val userVal = authManager.currentUser.value ?: return
        if (userVal.role != "TEACHER") return

        viewModelScope.launch {
            publishAssignmentUseCase(title, description, targetStudent, videoTitle, durationSeconds, videoUrl)
        }
    }

    fun markAssignmentComplete(assignmentId: Int, completed: Boolean) {
        viewModelScope.launch {
            completeAssignmentUseCase(assignmentId, completed)
        }
    }

    fun deleteAssignment(assignmentId: Int) {
        viewModelScope.launch {
            deleteAssignmentUseCase(assignmentId)
        }
    }
}
