package com.violinmaster.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.violinmaster.app.data.IPracticeRepository
import com.violinmaster.app.data.UserAccount
import com.violinmaster.app.data.AnalyticsHelper
import com.violinmaster.app.di.AuthManager
import com.violinmaster.app.domain.usecase.LoginUseCase
import com.violinmaster.app.domain.usecase.RegisterUseCase
import com.violinmaster.app.domain.usecase.SetRecoveryQuestionUseCase
import com.violinmaster.app.domain.usecase.VerifyRecoveryAnswerUseCase
import com.violinmaster.app.domain.usecase.ResetPinUseCase
import com.violinmaster.app.domain.model.RecoveryQuestion
import com.violinmaster.app.security.SecurityUtils
import com.violinmaster.app.security.VideoSecurityService
import com.violinmaster.app.ui.state.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

/**
 * Content data class for AuthViewModel UiState.
 */
data class AuthContent(
    val currentUser: UserAccount? = null
)

/**
 * Result of a PIN recovery answer verification.
 * Reset to [Idle] when a new recovery flow starts.
 */
sealed class RecoveryVerification {
    /** No verification in progress / initial state. */
    data object Idle : RecoveryVerification()

    /** Verification is running (DB query in progress). */
    data object InProgress : RecoveryVerification()

    /** Answer was correct — proceed to set new PIN. */
    data object Success : RecoveryVerification()

    /** Answer was wrong. Check [AuthViewModel.recoveryAttempts] for count. */
    data object Failed : RecoveryVerification()
}

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val repository: IPracticeRepository,
    private val authManager: AuthManager,
    private val securityUtils: SecurityUtils,
    private val analyticsHelper: AnalyticsHelper,
    private val loginUseCase: LoginUseCase,
    private val registerUseCase: RegisterUseCase,
    private val setRecoveryQuestionUseCase: SetRecoveryQuestionUseCase,
    private val verifyRecoveryAnswerUseCase: VerifyRecoveryAnswerUseCase,
    private val resetPinUseCase: ResetPinUseCase
) : ViewModel() {

    // --- User Roles Authentication State ---
    private val _currentUser = MutableStateFlow<UserAccount?>(null)
    val currentUser: StateFlow<UserAccount?> = _currentUser.asStateFlow()

    private val _loginError = MutableStateFlow<String?>(null)
    val loginError: StateFlow<String?> = _loginError.asStateFlow()

    private val _signupSuccess = MutableStateFlow<String?>(null)
    val signupSuccess: StateFlow<String?> = _signupSuccess.asStateFlow()

    // ── Unified UiState (REQ-UISTATE-001) ──────────────────────────
    private val _uiState = MutableStateFlow<UiState<AuthContent>>(
        UiState.Content(AuthContent())
    )
    val uiState: StateFlow<UiState<AuthContent>> = _uiState.asStateFlow()

    // --- Secure Passcode & Privacy states ---
    private val _isSecurityLocked = MutableStateFlow(securityUtils.isPasscodeSet())
    val isSecurityLocked: StateFlow<Boolean> = _isSecurityLocked.asStateFlow()

    private val _isUserAuthenticated = MutableStateFlow(!securityUtils.isPasscodeSet())
    val isUserAuthenticated: StateFlow<Boolean> = _isUserAuthenticated.asStateFlow()

    private val _securityErrorString = MutableStateFlow<String?>(null)
    val securityErrorString: StateFlow<String?> = _securityErrorString.asStateFlow()

    // ── PIN Recovery State (REQ-PINREC-001, REQ-PINREC-002, REQ-PINREC-005) ──
    private val _forgotPinUsername = MutableStateFlow("")
    val forgotPinUsername: StateFlow<String> = _forgotPinUsername.asStateFlow()

    private val _recoveryAttempts = MutableStateFlow(0)
    val recoveryAttempts: StateFlow<Int> = _recoveryAttempts.asStateFlow()

    private val _recoveryLocked = MutableStateFlow(false)
    val recoveryLocked: StateFlow<Boolean> = _recoveryLocked.asStateFlow()

    private val _recoveryQuestion = MutableStateFlow<String?>(null)
    val recoveryQuestion: StateFlow<String?> = _recoveryQuestion.asStateFlow()

    /** Result of the last recovery answer verification. Reset on new flow start. */
    private val _recoveryVerificationResult = MutableStateFlow<RecoveryVerification>(RecoveryVerification.Idle)
    val recoveryVerificationResult: StateFlow<RecoveryVerification> = _recoveryVerificationResult.asStateFlow()

    /** Whether the recovery question has been loaded from the database. */
    private val _recoveryQuestionLoaded = MutableStateFlow(false)
    val recoveryQuestionLoaded: StateFlow<Boolean> = _recoveryQuestionLoaded.asStateFlow()

    init {
        // Restore user session from SharedPreferences
        val savedUserId = authManager.getSavedUserId()
        if (savedUserId != null) {
            viewModelScope.launch {
                val dbUser = repository.getUserByUsername(savedUserId)
                if (dbUser != null) {
                    _currentUser.value = dbUser
                    _uiState.value = UiState.Content(AuthContent(currentUser = dbUser))
                }
            }
        }
    }

    fun register(username: String, pin: String, role: String, teacherCodeInput: String = "", birthYear: Int = 0) {
        if (username.isBlank()) {
            _loginError.value = "error_username_empty"
            return
        }
        if (pin.length != 4) {
            _loginError.value = "error_pin_length"
            return
        }
        val currentYear = LocalDate.now().year
        if (birthYear < 1900 || birthYear > currentYear) {
            _loginError.value = "error_birth_year_invalid"
            return
        }
        _loginError.value = null
        _signupSuccess.value = null

        viewModelScope.launch {
            val result = registerUseCase(username, pin, role, birthYear)
            if (result != null) {
                // Wire teacherCode for non-teacher roles (use case defaults to "")
                if (teacherCodeInput.isNotBlank() && role != "TEACHER") {
                    val updated = result.copy(teacherCode = teacherCodeInput)
                    repository.insertUser(updated)
                }
                _signupSuccess.value = "success_register"
            } else {
                _loginError.value = "error_user_exists"
            }
        }
    }

    fun login(username: String, pin: String) {
        if (username.isBlank()) {
            _loginError.value = "error_username_empty"
            return
        }
        if (pin.length != 4) {
            _loginError.value = "error_pin_length"
            return
        }
        _loginError.value = null

        viewModelScope.launch {
            val user = loginUseCase(username, pin)
            if (user != null) {
                _currentUser.value = user
                _loginError.value = null
                _uiState.value = UiState.Content(AuthContent(currentUser = user))
            } else {
                _loginError.value = "error_login_failed"
            }
        }
    }

    fun logout() {
        _currentUser.value = null
        _uiState.value = UiState.Content(AuthContent(currentUser = null))
        authManager.clearSession()
        _loginError.value = null
        _signupSuccess.value = null
    }

    fun linkTeacherCode(teacherCode: String) {
        val userVal = _currentUser.value ?: return
        if (userVal.role != "STUDENT") return

        viewModelScope.launch {
            val updatedUser = userVal.copy(teacherCode = teacherCode)
            repository.insertUser(updatedUser)
            _currentUser.value = updatedUser
            _uiState.value = UiState.Content(AuthContent(currentUser = updatedUser))
        }
    }

    fun setPasscodeLock(clearcode: String) {
        if (clearcode.length < 4) {
            _securityErrorString.value = "Passcode must be at least 4 digits."
            return
        }
        val success = securityUtils.savePasscode(clearcode)
        if (success) {
            _isSecurityLocked.value = true
            _isUserAuthenticated.value = true // automatically unlock area when set
            _securityErrorString.value = null
        } else {
            _securityErrorString.value = "Failed to secure passcode."
        }
    }

    fun removePasscodeLock(clearcode: String) {
        if (securityUtils.verifyPasscode(clearcode)) {
            securityUtils.clearPasscode()
            _isSecurityLocked.value = false
            _isUserAuthenticated.value = true
            _securityErrorString.value = null
        } else {
            _securityErrorString.value = "Incorrect passcode. Security preservation active."
        }
    }

    fun authenticatePasscode(inputPin: String): Boolean {
        val isCorrect = securityUtils.verifyPasscode(inputPin)
        if (isCorrect) {
            _isUserAuthenticated.value = true
            _securityErrorString.value = null
            return true
        } else {
            _securityErrorString.value = "Access Denied: Incorrect Passcode Match"
            return false
        }
    }

    fun clearSecurityErrors() {
        _securityErrorString.value = null
    }

    fun lockSessionPromptly() {
        if (securityUtils.isPasscodeSet()) {
            _isUserAuthenticated.value = false
        }
    }

    fun logoutUnlockedArea() {
        _isUserAuthenticated.value = false
        _isSecurityLocked.value = securityUtils.isPasscodeSet()
    }

    // --- Secure Media State (Premium Videos) ---
    private val _selectedPremiumVideoId = MutableStateFlow<String?>(null)
    val selectedPremiumVideoId: StateFlow<String?> = _selectedPremiumVideoId.asStateFlow()

    private val _securePlaybackUrl = MutableStateFlow<String?>(null)
    val securePlaybackUrl: StateFlow<String?> = _securePlaybackUrl.asStateFlow()

    fun selectAndDecryptVideo(videoId: String) {
        if (!_isUserAuthenticated.value) {
            _securityErrorString.value = "Please authenticate passcode to play masterclass video stream."
            return
        }
        _selectedPremiumVideoId.value = videoId
        val mockSession = "session_token_master"
        val signedUrl = VideoSecurityService.obtainSecureSignedUrl(videoId, mockSession)
        _securePlaybackUrl.value = signedUrl
        _securityErrorString.value = null
    }

    fun closeVideoPlayer() {
        _selectedPremiumVideoId.value = null
        _securePlaybackUrl.value = null
    }

    fun clearAuthMessages() {
        _loginError.value = null
        _signupSuccess.value = null
    }

    // ═══════════════════════════════════════════════════════════════════════
    // PIN Recovery Methods (REQ-PINREC-001, REQ-PINREC-002, REQ-PINREC-004, REQ-PINREC-005)
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Sets the recovery question and hashed answer for the currently logged-in user.
     * During registration, call this after a successful registration.
     */
    fun setRecoveryQuestion(question: String, answer: String) {
        val user = _currentUser.value ?: return
        val recoveryEnum = RecoveryQuestion.entries.find { it.questionKey == question } ?: return
        viewModelScope.launch {
            setRecoveryQuestionUseCase(user.username, recoveryEnum, answer)
        }
    }

    /**
     * Verifies a recovery answer for a given username.
     * Rate-limited: after 3 failed attempts, recovery is locked for 5 minutes.
     *
     * Result is delivered asynchronously via [recoveryVerificationResult] StateFlow.
     * Callers MUST observe this flow instead of relying on a return value.
     */
    fun verifyRecoveryAnswer(username: String, answer: String) {
        if (_recoveryLocked.value) {
            _recoveryVerificationResult.value = RecoveryVerification.Failed
            return
        }

        _recoveryVerificationResult.value = RecoveryVerification.InProgress

        viewModelScope.launch {
            val success = verifyRecoveryAnswerUseCase(username, answer)

            if (success) {
                _recoveryAttempts.value = 0
                _recoveryLocked.value = false
                _recoveryVerificationResult.value = RecoveryVerification.Success
            } else {
                _recoveryAttempts.value += 1
                if (_recoveryAttempts.value >= 3) {
                    _recoveryLocked.value = true
                }
                _recoveryVerificationResult.value = RecoveryVerification.Failed
            }
        }
    }

    /**
     * Resets the PIN for a user after successful recovery verification.
     * Auto-logs the user in after reset.
     */
    fun resetPin(username: String, newPin: String) {
        viewModelScope.launch {
            val success = resetPinUseCase(username, newPin)
            if (success) {
                // Reload the user after PIN reset
                val updatedUser = repository.getUserByUsername(username)
                if (updatedUser != null) {
                    _currentUser.value = updatedUser
                    _uiState.value = UiState.Content(AuthContent(currentUser = updatedUser))
                    _loginError.value = null
                }
                // Reset recovery state
                _recoveryAttempts.value = 0
                _recoveryLocked.value = false
            }
        }
    }

    /**
     * Loads the stored security question key for a user from the database.
     * Result is delivered asynchronously via [recoveryQuestion] StateFlow.
     * Completion is signaled via [recoveryQuestionLoaded].
     *
     * Callers MUST observe [recoveryQuestion] instead of relying on a return value.
     * When [recoveryQuestion] is null AND [recoveryQuestionLoaded] is true,
     * the user has no recovery question configured.
     */
    fun loadRecoveryQuestion(username: String) {
        _recoveryQuestionLoaded.value = false
        viewModelScope.launch {
            val user = repository.getUserByUsername(username)
            _recoveryQuestion.value = user?.securityQuestion?.takeIf { it.isNotBlank() }
            _recoveryQuestionLoaded.value = true
        }
    }

    /**
     * Sets the username for the forgot-PIN flow and resets all recovery state.
     */
    fun setForgotPinUsername(username: String) {
        _forgotPinUsername.value = username
        // Reset recovery state when starting a new recovery flow
        _recoveryAttempts.value = 0
        _recoveryLocked.value = false
        _recoveryQuestion.value = null
        _recoveryQuestionLoaded.value = false
        _recoveryVerificationResult.value = RecoveryVerification.Idle
    }

    /**
     * Resets the recovery lockout state (manually or after timeout).
     */
    fun resetRecoveryLock() {
        _recoveryAttempts.value = 0
        _recoveryLocked.value = false
    }

    // ── Private helpers ─────────────────────────────────────────────────
}
