package com.violinmaster.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.violinmaster.app.data.IPracticeRepository
import com.violinmaster.app.data.UserAccount
import com.violinmaster.app.di.AuthManager
import com.violinmaster.app.domain.usecase.LoginUseCase
import com.violinmaster.app.domain.usecase.RegisterUseCase
import com.violinmaster.app.security.SecurityUtils
import com.violinmaster.app.security.VideoSecurityService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val repository: IPracticeRepository,
    private val authManager: AuthManager,
    private val securityUtils: SecurityUtils,
    private val loginUseCase: LoginUseCase,
    private val registerUseCase: RegisterUseCase
) : ViewModel() {

    // --- User Roles Authentication State ---
    private val _currentUser = MutableStateFlow<UserAccount?>(null)
    val currentUser: StateFlow<UserAccount?> = _currentUser.asStateFlow()

    private val _loginError = MutableStateFlow<String?>(null)
    val loginError: StateFlow<String?> = _loginError.asStateFlow()

    private val _signupSuccess = MutableStateFlow<String?>(null)
    val signupSuccess: StateFlow<String?> = _signupSuccess.asStateFlow()

    // --- Secure Passcode & Privacy states ---
    private val _isSecurityLocked = MutableStateFlow(securityUtils.isPasscodeSet())
    val isSecurityLocked: StateFlow<Boolean> = _isSecurityLocked.asStateFlow()

    private val _isUserAuthenticated = MutableStateFlow(!securityUtils.isPasscodeSet())
    val isUserAuthenticated: StateFlow<Boolean> = _isUserAuthenticated.asStateFlow()

    private val _securityErrorString = MutableStateFlow<String?>(null)
    val securityErrorString: StateFlow<String?> = _securityErrorString.asStateFlow()

    init {
        // Restore user session from SharedPreferences
        val savedUserId = authManager.getSavedUserId()
        if (savedUserId != null) {
            viewModelScope.launch {
                val dbUser = repository.getUserByUsername(savedUserId)
                if (dbUser != null) {
                    _currentUser.value = dbUser
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
            } else {
                _loginError.value = "error_login_failed"
            }
        }
    }

    fun logout() {
        _currentUser.value = null
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
}
