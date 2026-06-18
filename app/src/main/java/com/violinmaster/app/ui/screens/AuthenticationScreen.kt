package com.violinmaster.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.violinmaster.app.di.AuthManager
import com.violinmaster.app.ui.component.AuthShieldHeader
import com.violinmaster.app.ui.component.BirthYearSelector
import com.violinmaster.app.ui.component.LoginKeypadGrid
import com.violinmaster.app.ui.component.PinBulletIndicator
import com.violinmaster.app.ui.component.RoleSelector
import com.violinmaster.app.ui.theme.AppLanguage
import com.violinmaster.app.ui.theme.Localization
import com.violinmaster.app.ui.viewmodel.AuthViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthenticationScreen(
    authViewModel: AuthViewModel,
    authManager: AuthManager,
    appLanguage: AppLanguage,
    modifier: Modifier = Modifier
) {
    val lang = appLanguage
    val errKey by authViewModel.loginError.collectAsState()
    val successKey by authViewModel.signupSuccess.collectAsState()
    var isRegisterMode by remember { mutableStateOf(false) }
    var isRecoveryMode by remember { mutableStateOf(false) }
    var recoveryStep by remember { mutableStateOf(0) } // 0=username, 1=question/answer, 2=newPIN
    var recoveryUsername by remember { mutableStateOf("") }
    var recoveryAnswer by remember { mutableStateOf("") }
    var recoveryNewPin by remember { mutableStateOf("") }
    var recoveryError by remember { mutableStateOf<String?>(null) }
    var recoverySuccess by remember { mutableStateOf<String?>(null) }
    var username by remember { mutableStateOf("") }
    var pin by remember { mutableStateOf("") }
    var selectedRole by remember { mutableStateOf("STUDENT") }
    var teacherCodeToLink by remember { mutableStateOf("") }
    var birthYear by remember { mutableStateOf("") }

    // Resolve displayed message
    val resolvedError = errKey?.let { Localization.get(it, lang) }
    val resolvedSuccess = successKey?.let { Localization.get(it, lang) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .widthIn(max = 440.dp)
                .verticalScroll(rememberScrollState())
        ) {
            AuthShieldHeader(
                isRegisterMode = isRegisterMode,
                lang = lang
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Text Field for Username
            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text(Localization.get("username_label", lang), fontSize = 12.sp) },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.12f)
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("username_input")
            )

            // PIN Bullet display representation
            PinBulletIndicator(
                pin = pin,
                lang = lang
            )

            // Role selection panel
            RoleSelector(
                selectedRole = selectedRole,
                onRoleSelected = { selectedRole = it },
                lang = lang
            )

            // Birth year selector (required for registration)
            if (isRegisterMode) {
                BirthYearSelector(
                    birthYear = birthYear,
                    onYearSelected = { birthYear = it },
                    lang = lang
                )
            }

            // Student target teacher code injection if register
            if (isRegisterMode && selectedRole == "STUDENT") {
                OutlinedTextField(
                    value = teacherCodeToLink,
                    onValueChange = { teacherCodeToLink = it },
                    label = { Text(Localization.get("input_code_hint", lang), fontSize = 11.sp) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = MaterialTheme.colorScheme.secondary,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.12f)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("teacher_code_field")
                )
            }

            // Error and Success Feedback Loops
            if (resolvedError != null) {
                Text(
                    text = resolvedError,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }

            if (resolvedSuccess != null) {
                Text(
                    text = resolvedSuccess,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Green,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }

            // Compact tactile PIN keyboard
            LoginKeypadGrid(
                currentValue = pin,
                onValueChange = { newVal ->
                    if (newVal.length <= 4) {
                        pin = newVal
                        authViewModel.clearAuthMessages()
                    }
                },
                onValidate = {
                    if (pin.length == 4) {
                        if (isRegisterMode) {
                            authViewModel.register(username, pin, selectedRole, teacherCodeToLink, birthYear.toIntOrNull() ?: 0)
                        } else {
                            authViewModel.login(username, pin)
                        }
                        pin = ""
                    }
                },
                lang = lang,
                isCorrectLength = pin.length == 4
            )

            // ── Forgot PIN? link (only in login mode) ──────────────────
            if (!isRegisterMode && !isRecoveryMode) {
                Text(
                    text = Localization.get("forgot_pin", lang),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary,
                    fontWeight = FontWeight.Normal,
                    modifier = Modifier
                        .clickable {
                            isRecoveryMode = true
                            recoveryStep = 0
                            recoveryError = null
                            recoverySuccess = null
                            authViewModel.clearAuthMessages()
                        }
                        .padding(8.dp)
                        .testTag("forgot_pin_button")
                )

                // ── Google Sign-In button ──────────────────────────
                Spacer(modifier = Modifier.height(8.dp))
                val context = LocalContext.current
                val googleSignInClient = remember {
                    val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                        .requestIdToken(context.getString(android.R.string.ok))
                        .requestEmail()
                        .build()
                    GoogleSignIn.getClient(context, gso)
                }
                OutlinedButton(
                    onClick = {
                        val signInIntent = googleSignInClient.signInIntent
                        // Launch sign-in via Activity — the result is handled by the Activity's onActivityResult
                        context.startActivity(signInIntent)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("google_sign_in_button"),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.3f))
                ) {
                    Text(
                        text = Localization.get("sign_in_with_google", lang),
                        color = Color.White,
                        fontSize = 14.sp
                    )
                }
            }

            // ── Recovery Flow UI ───────────────────────────────────────
            if (isRecoveryMode) {
                when (recoveryStep) {
                    // Step 0: Enter username
                    0 -> {
                        Text(
                            text = Localization.get("recovery_enter_username", lang),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White
                        )
                        OutlinedTextField(
                            value = recoveryUsername,
                            onValueChange = { recoveryUsername = it },
                            label = { Text(Localization.get("username_label", lang), fontSize = 12.sp) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.12f)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("recovery_username_input")
                        )
                        Button(
                            onClick = {
                                if (recoveryUsername.isBlank()) return@Button
                                authViewModel.setForgotPinUsername(recoveryUsername)
                                val question = authViewModel.getRecoveryQuestionForUser(recoveryUsername)
                                if (question.isNullOrBlank()) {
                                    recoveryError = Localization.get("recovery_no_question", lang)
                                } else {
                                    recoveryError = null
                                    recoveryStep = 1
                                }
                            },
                            modifier = Modifier.testTag("recovery_continue_button")
                        ) {
                            Text("Continue")
                        }
                    }

                    // Step 1: Answer security question
                    1 -> {
                        val questionKey = authViewModel.getRecoveryQuestionForUser(recoveryUsername)
                        val questionText = questionKey?.let { Localization.get(it, lang) }
                            ?: Localization.get("recovery_question_label", lang)
                        Text(
                            text = questionText,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        OutlinedTextField(
                            value = recoveryAnswer,
                            onValueChange = { recoveryAnswer = it },
                            label = { Text(Localization.get("recovery_answer_hint", lang), fontSize = 12.sp) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.12f)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("recovery_answer_input")
                        )
                        Button(
                            onClick = {
                                if (authViewModel.recoveryLocked.value) return@Button
                                val correct = authViewModel.verifyRecoveryAnswer(recoveryUsername, recoveryAnswer)
                                if (correct) {
                                    recoveryError = null
                                    recoveryStep = 2
                                } else {
                                    recoveryAnswer = ""
                                    if (authViewModel.recoveryLocked.value) {
                                        recoveryError = Localization.get("recovery_locked", lang)
                                            .replace("{0}", "5")
                                    } else {
                                        recoveryError = Localization.get("recovery_wrong_answer", lang)
                                    }
                                }
                            },
                            modifier = Modifier.testTag("recovery_verify_button")
                        ) {
                            Text("Verify")
                        }
                    }

                    // Step 2: Set new PIN
                    2 -> {
                        Text(
                            text = Localization.get("recovery_set_new_pin", lang),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White
                        )
                        PinBulletIndicator(
                            pin = recoveryNewPin,
                            lang = lang
                        )
                        LoginKeypadGrid(
                            currentValue = recoveryNewPin,
                            onValueChange = { newVal ->
                                if (newVal.length <= 4) {
                                    recoveryNewPin = newVal
                                }
                            },
                            onValidate = {
                                if (recoveryNewPin.length == 4) {
                                    authViewModel.resetPin(recoveryUsername, recoveryNewPin)
                                    recoverySuccess = Localization.get("recovery_success", lang)
                                    recoveryError = null
                                    // Reset recovery mode after successful pin reset
                                    isRecoveryMode = false
                                    recoveryStep = 0
                                    recoveryUsername = ""
                                    recoveryNewPin = ""
                                    recoveryAnswer = ""
                                }
                            },
                            lang = lang,
                            isCorrectLength = recoveryNewPin.length == 4
                        )
                        Button(
                            onClick = {
                                isRecoveryMode = false
                                recoveryStep = 0
                                recoveryError = null
                                recoveryUsername = ""
                                recoveryNewPin = ""
                                recoveryAnswer = ""
                            },
                            modifier = Modifier.testTag("recovery_cancel_button")
                        ) {
                            Text("Cancel")
                        }
                    }
                }

                // Recovery error display
                if (recoveryError != null) {
                    Text(
                        text = recoveryError!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.testTag("recovery_error_text")
                    )
                }

                // Recovery success display
                if (recoverySuccess != null) {
                    Text(
                        text = recoverySuccess!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Green,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.testTag("recovery_success_text")
                    )
                }

                // Cancel recovery
                if (recoveryStep < 2) {
                    Text(
                        text = "Cancel",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .clickable {
                                isRecoveryMode = false
                                recoveryStep = 0
                                recoveryError = null
                                recoveryUsername = ""
                                recoveryAnswer = ""
                                authViewModel.resetRecoveryLock()
                            }
                            .padding(8.dp)
                    )
                }
            }

            // Switch Mode Toggle
            Text(
                text = Localization.get(if (isRegisterMode) "switch_to_log" else "switch_to_reg", lang),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .clickable {
                        isRegisterMode = !isRegisterMode
                        authViewModel.clearAuthMessages()
                        username = ""
                        pin = ""
                    }
                    .padding(8.dp)
            )

        }
    }
}
