package com.violinmaster.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.violinmaster.app.data.AnalyticsHelper
import com.violinmaster.app.data.IChatRepository
import com.violinmaster.app.data.firebase.Message
import com.violinmaster.app.di.AuthManager
import com.violinmaster.app.domain.usecase.GetMessagesUseCase
import com.violinmaster.app.domain.usecase.SendMessageUseCase
import com.violinmaster.app.ui.state.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Content data class for ChatViewModel UiState.
 *
 * Groups all primary screen data into a single value object
 * so the UiState<T> pattern has exactly one Content payload.
 */
data class ChatContent(
    val messages: List<Message> = emptyList(),
    val currentAssignmentId: String? = null
)

/**
 * ViewModel for teacher-student chat.
 *
 * REQ-CHAT-005: Exposes messages StateFlow, sendMessage(), and loading/error states.
 * REQ-UISTATE-001: Unified UiState<ChatContent> pattern replaces individual is/error/loading flows.
 * REQ-UISTATE-002: Screen consumes uiState.collectAsState() with when(is Loading/Error/Content).
 *
 * Reads currentUser from AuthManager to populate sender identity on message send.
 *
 * @param chatRepository Repository for Firestore + Room message persistence.
 * @param authManager Provides current user identity.
 */
@HiltViewModel
class ChatViewModel @Inject constructor(
    private val chatRepository: IChatRepository,
    internal val authManager: AuthManager,
    private val analyticsHelper: AnalyticsHelper,
    private val sendMessageUseCase: SendMessageUseCase,
    private val getMessagesUseCase: GetMessagesUseCase
) : ViewModel() {

    // ── Unified UiState (REQ-UISTATE-001) ──────────────────────────

    private val _uiState = MutableStateFlow<UiState<ChatContent>>(
        UiState.Content(ChatContent())
    )
    val uiState: StateFlow<UiState<ChatContent>> = _uiState.asStateFlow()

    // ── Individual StateFlows (kept for backward compat, updated synchronously with _uiState) ──

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

        // Update both UiState and legacy flows synchronously
        _uiState.value = UiState.Loading
        _isLoading.value = true
        _currentAssignmentId.value = assignmentId
        _messages.value = emptyList()

        messagesJob = viewModelScope.launch {
            try {
                getMessagesUseCase(assignmentId).collect { messageList ->
                    _messages.value = messageList
                    _isLoading.value = false
                    _uiState.value = UiState.Content(
                        ChatContent(
                            messages = messageList,
                            currentAssignmentId = assignmentId
                        )
                    )
                }
            } catch (e: Exception) {
                _isLoading.value = false
                _uiState.value = UiState.Error(
                    message = "Failed to load messages: ${e.message}",
                    throwable = e
                )
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

        val currentUser = authManager.currentUser.value
        if (currentUser == null) {
            _error.value = "Failed to send: no user logged in"
            return
        }

        viewModelScope.launch {
            try {
                sendMessageUseCase(assignmentId, text)
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

        val currentUser = authManager.currentUser.value
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
