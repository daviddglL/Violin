package com.violinmaster.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.violinmaster.app.data.IChatRepository
import com.violinmaster.app.data.firebase.Message
import com.violinmaster.app.di.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for teacher-student chat.
 *
 * REQ-CHAT-005: Exposes messages StateFlow, sendMessage(), and loading/error states.
 * Reads currentUser from SessionManager to populate sender identity on message send.
 *
 * @param chatRepository Repository for Firestore + Room message persistence.
 * @param sessionManager Provides current user identity and app state.
 */
@HiltViewModel
class ChatViewModel @Inject constructor(
    private val chatRepository: IChatRepository,
    internal val sessionManager: SessionManager
) : ViewModel() {

    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _currentAssignmentId = MutableStateFlow<String?>(null)
    val currentAssignmentId: StateFlow<String?> = _currentAssignmentId.asStateFlow()

    private var messagesJob: Job? = null

    /**
     * Loads messages for an assignment and starts observing the real-time flow.
     *
     * REQ-CHAT-002: Messages are scoped to a single assignment.
     * Previous assignment subscription is cancelled on switch.
     *
     * @param assignmentId The Firestore document ID for the assignment.
     */
    fun loadAssignment(assignmentId: String) {
        // Cancel previous subscription
        messagesJob?.cancel()
        _isLoading.value = true
        _currentAssignmentId.value = assignmentId
        _messages.value = emptyList()

        messagesJob = viewModelScope.launch {
            try {
                chatRepository.loadMessages(assignmentId).collect { messageList ->
                    _messages.value = messageList
                    _isLoading.value = false
                }
            } catch (e: Exception) {
                _error.value = "Failed to load messages: ${e.message}"
                _isLoading.value = false
            }
        }
    }

    /**
     * Sends a text message to the current assignment.
     *
     * REQ-CHAT-005: Reads currentUser from SessionManager for senderUsername
     * and senderRole. Validates that an assignment is loaded and a user is
     * logged in before sending.
     *
     * @param text The message text. Blank/empty text is silently ignored.
     */
    fun sendMessage(text: String) {
        if (text.isBlank()) return

        val assignmentId = _currentAssignmentId.value
        if (assignmentId == null) {
            _error.value = "Failed to send: no assignment loaded"
            return
        }

        val currentUser = sessionManager.currentUser.value
        if (currentUser == null) {
            _error.value = "Failed to send: no user logged in"
            return
        }

        val message = Message(
            senderUsername = currentUser.username,
            senderRole = currentUser.role,
            text = text.trim(),
            timestamp = System.currentTimeMillis()
        )

        viewModelScope.launch {
            try {
                chatRepository.sendMessage(assignmentId, message)
                // The loadMessages Flow will automatically emit the updated list
                // including the newly sent message after Room cache update.
            } catch (e: Exception) {
                _error.value = "Failed to send message: ${e.message}"
            }
        }
    }

    /**
     * Sends a video attachment as a chat message.
     *
     * REQ-VID-007: After successful upload, the download URL is shared
     * as a chat message with attachmentType="video".
     *
     * @param videoUrl Firebase Storage download URL of the uploaded video.
     */
    fun sendVideoAttachment(videoUrl: String) {
        val assignmentId = _currentAssignmentId.value
        if (assignmentId == null) {
            _error.value = "Failed to send video: no assignment loaded"
            return
        }

        val currentUser = sessionManager.currentUser.value
        if (currentUser == null) {
            _error.value = "Failed to send video: no user logged in"
            return
        }

        val message = Message(
            senderUsername = currentUser.username,
            senderRole = currentUser.role,
            text = "",
            attachmentUrl = videoUrl,
            attachmentType = "video",
            timestamp = System.currentTimeMillis()
        )

        viewModelScope.launch {
            try {
                chatRepository.sendMessage(assignmentId, message)
            } catch (e: Exception) {
                _error.value = "Failed to send video: ${e.message}"
            }
        }
    }

    /**
     * Clears the current error state.
     */
    fun clearError() {
        _error.value = null
    }
}
