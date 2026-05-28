package com.violinmaster.app.ui.viewmodel

import android.util.Base64
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.violinmaster.app.data.PracticeRepository
import com.violinmaster.app.data.UserAccount
import com.violinmaster.app.di.SessionManager
import com.violinmaster.app.security.SecurityUtils
import com.violinmaster.app.security.VideoSecurityService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Arrays
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val repository: PracticeRepository,
    private val sessionManager: SessionManager,
    private val securityUtils: SecurityUtils
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
        val savedUserId = sessionManager.getSavedUserId()
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
        val currentYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
        if (birthYear < 1900 || birthYear > currentYear) {
            _loginError.value = "error_birth_year_invalid"
            return
        }
        _loginError.value = null
        _signupSuccess.value = null

        viewModelScope.launch {
            val existing = repository.getUserByUsername(username)
            if (existing != null) {
                _loginError.value = "error_user_exists"
                return@launch
            }

            // Generate salt and hash passcode securely using SecurityUtils
            val salt = SecurityUtils.generateSalt()
            val passChars = pin.toCharArray()
            val hashed = SecurityUtils.hashPasscode(passChars, salt)
            Arrays.fill(passChars, '0')

            // If teacher, invent a new Invite Code
            val inviteCode = if (role == "TEACHER") "TEACH-${(1000..9999).random()}" else teacherCodeInput

            val newAccount = UserAccount(
                username = username,
                role = role,
                hashedPassword = hashed,
                salt = Base64.encodeToString(salt, Base64.DEFAULT),
                teacherCode = inviteCode,
                birthYear = birthYear
            )

            repository.insertUser(newAccount)
            _signupSuccess.value = "success_register"
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
            val user = repository.getUserByUsername(username)
            if (user == null) {
                _loginError.value = "error_login_failed"
                return@launch
            }

            val saltBytes = Base64.decode(user.salt, Base64.DEFAULT)
            val passChars = pin.toCharArray()
            val computedHash = SecurityUtils.hashPasscode(passChars, saltBytes)
            Arrays.fill(passChars, '0')

            if (computedHash == user.hashedPassword) {
                _currentUser.value = user
                sessionManager.saveCurrentUser(user)
                _loginError.value = null
            } else {
                _loginError.value = "error_login_failed"
            }
        }
    }

    fun logout() {
        _currentUser.value = null
        sessionManager.clearSession()
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
