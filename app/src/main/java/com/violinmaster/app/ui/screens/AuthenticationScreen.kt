@file:Suppress("DEPRECATION") // GoogleSignIn deprecated in play-services-auth 21+

package com.violinmaster.app.ui.screens

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import com.google.android.gms.common.api.ApiException
import com.violinmaster.app.data.auth.IGoogleAuthRepository
import com.violinmaster.app.di.AuthManager
import com.violinmaster.app.ui.component.AuthShieldHeader
import com.violinmaster.app.ui.component.BirthYearSelector
import com.violinmaster.app.ui.component.GoogleSignInSection
import com.violinmaster.app.ui.component.LoginKeypadGrid
import com.violinmaster.app.ui.component.PinBulletIndicator
import com.violinmaster.app.ui.component.RoleSelector
import com.violinmaster.app.ui.theme.AppLanguage
import com.violinmaster.app.ui.theme.Localization
import com.violinmaster.app.ui.viewmodel.AuthViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthenticationScreen(
    authViewModel: AuthViewModel,
    googleAuthRepository: IGoogleAuthRepository,
    authManager: AuthManager,
    appLanguage: AppLanguage,
    modifier: Modifier = Modifier
) {
    val lang = appLanguage
    val errKey by authViewModel.loginError.collectAsState()
    val successKey by authViewModel.signupSuccess.collectAsState()
    val isGoogleSignedIn by authManager.isGoogleSignedIn.collectAsState()

    var isRegisterMode by remember { mutableStateOf(false) }
    var username by remember { mutableStateOf("") }
    var pin by remember { mutableStateOf("") }
    var selectedRole by remember { mutableStateOf("STUDENT") } // "TEACHER", "STUDENT", "FREELANCER"
    var teacherCodeToLink by remember { mutableStateOf("") }
    var birthYear by remember { mutableStateOf("") } // Required for registration

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    // Google Sign-In launcher
    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            coroutineScope.launch {
                try {
                    val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                    val account = task.getResult(ApiException::class.java)
                    val idToken = account.idToken
                    if (idToken != null) {
                        val signInResult = googleAuthRepository.signIn(idToken)
                        signInResult.onSuccess {
                            authManager.setGoogleSignedIn(true)
                        }.onFailure { error ->
                            snackbarHostState.showSnackbar(
                                "Google Sign-In failed: ${error.message ?: "Unknown error"}"
                            )
                        }
                    } else {
                        snackbarHostState.showSnackbar("Google Sign-In failed: no ID token received")
                    }
                } catch (e: ApiException) {
                    snackbarHostState.showSnackbar("Google Sign-In failed: ${e.statusCode}")
                } catch (e: Exception) {
                    snackbarHostState.showSnackbar("Google Sign-In failed: ${e.message}")
                }
            }
        }
    }

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

            Spacer(modifier = Modifier.height(8.dp))

            // Google Sign-In section
            GoogleSignInSection(
                isGoogleSignedIn = isGoogleSignedIn,
                onSignInClick = {
                    val signInIntent = googleAuthRepository.getSignInIntent()
                    googleSignInLauncher.launch(signInIntent)
                }
            )

            // SnackbarHost for Google Sign-In errors
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
    }
}

