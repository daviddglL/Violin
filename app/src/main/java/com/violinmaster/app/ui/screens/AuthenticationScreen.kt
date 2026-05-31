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
import com.violinmaster.app.data.auth.GoogleAuthRepository
import com.violinmaster.app.di.SessionManager
import com.violinmaster.app.ui.theme.AppLanguage
import com.violinmaster.app.ui.theme.Localization
import com.violinmaster.app.ui.viewmodel.AuthViewModel
import kotlinx.coroutines.launch

@Composable
fun AuthenticationScreen(
    authViewModel: AuthViewModel,
    googleAuthRepository: GoogleAuthRepository,
    sessionManager: SessionManager,
    appLanguage: AppLanguage,
    modifier: Modifier = Modifier
) {
    val lang = appLanguage
    val errKey by authViewModel.loginError.collectAsState()
    val successKey by authViewModel.signupSuccess.collectAsState()
    val isGoogleSignedIn by sessionManager.isGoogleSignedIn.collectAsState()

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
                            sessionManager.setGoogleSignedIn(true)
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
            // Header Shield Icon
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Shield Security Logo",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(40.dp)
                )
            }

            Text(
                text = Localization.get("login_required", lang).uppercase(),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center
            )

            Text(
                text = Localization.get(if (isRegisterMode) "register_desc" else "login_info_desc", lang),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                lineHeight = 16.sp
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
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = Localization.get("password_label", lang) + " (" + Localization.get("pin_hint", lang) + ")",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    for (i in 0 until 4) {
                        val filled = i < pin.length
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .clip(CircleShape)
                                .background(
                                    if (filled) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.surfaceVariant
                                )
                                .border(1.dp, Color.White.copy(alpha = 0.1f), CircleShape)
                        )
                    }
                }
            }

            // Role selection panel
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = Localization.get("role_label", lang),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary,
                    fontWeight = FontWeight.Bold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val roles = listOf(
                        "STUDENT" to "role_student",
                        "TEACHER" to "role_teacher",
                        "FREELANCER" to "role_freelancer"
                    )

                    roles.forEach { (roleKey, transKey) ->
                        val isSelected = selectedRole == roleKey
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                )
                                .border(
                                    1.dp,
                                    if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                    RoundedCornerShape(10.dp)
                                )
                                .clickable { selectedRole = roleKey }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = Localization.get(transKey, lang),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }

            // Birth year selector (required for registration)
            if (isRegisterMode) {
                val currentYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
                val years = (currentYear downTo 1930).toList()
                var expanded by remember { mutableStateOf(false) }

                Text(
                    text = Localization.get("birth_year_label", lang) + " *",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 4.dp)
                )
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = birthYear,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(Localization.get("birth_year_hint", lang), fontSize = 11.sp) },
                        trailingIcon = {
                            Text(if (expanded) "▲" else "▼", fontSize = 12.sp)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { expanded = true },
                        singleLine = true
                    )
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                        modifier = Modifier.fillMaxWidth(0.9f)
                    ) {
                        years.forEach { year ->
                            DropdownMenuItem(
                                text = { Text(year.toString()) },
                                onClick = {
                                    birthYear = year.toString()
                                    expanded = false
                                }
                            )
                        }
                    }
                }
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

            // Divider before Google section
            HorizontalDivider(
                color = Color.White.copy(alpha = 0.12f),
                modifier = Modifier.fillMaxWidth(0.6f)
            )

            // Google Sign-In section
            if (isGoogleSignedIn) {
                // Task 10: Feature gating — show AI features enabled badge
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .background(
                            Color(0xFF1B5E20).copy(alpha = 0.3f),
                            RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                        .testTag("ai_features_enabled")
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "AI enabled",
                        tint = Color(0xFF4CAF50),
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "AI features enabled",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFA5D6A7),
                        fontWeight = FontWeight.Medium
                    )
                }
            } else {
                // "Sign in with Google" button
                Button(
                    onClick = {
                        val signInIntent = googleAuthRepository.getSignInIntent()
                        googleSignInLauncher.launch(signInIntent)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = Color(0xFF1A1A2E)
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .height(48.dp)
                        .testTag("google_sign_in_button")
                ) {
                    Text(
                        text = "Sign in with Google for AI features",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF1A1A2E)
                    )
                }
            }

            // SnackbarHost for Google Sign-In errors
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
    }
}

@Composable
fun LoginKeypadGrid(
    currentValue: String,
    onValueChange: (String) -> Unit,
    onValidate: () -> Unit,
    lang: AppLanguage,
    isCorrectLength: Boolean
) {
    val keys = listOf(
        "1", "2", "3",
        "4", "5", "6",
        "7", "8", "9",
        "CLR", "0", "OK"
    )

    Column(
        modifier = Modifier.width(320.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        val rows = keys.chunked(3)
        rows.forEach { rowKeys ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                rowKeys.forEach { key ->
                    val isAction = key == "CLR" || key == "OK"
                    val isOkActive = key == "OK" && isCorrectLength

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                when {
                                    isOkActive -> MaterialTheme.colorScheme.primary
                                    isAction -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                                    else -> MaterialTheme.colorScheme.surface
                                }
                            )
                            .clickable {
                                when (key) {
                                    "CLR" -> onValueChange("")
                                    "OK" -> onValidate()
                                    else -> {
                                        if (currentValue.length < 4) {
                                            onValueChange(currentValue + key)
                                        }
                                    }
                                }
                            }
                            .testTag("login_keypad_$key"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = key,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = when {
                                isOkActive -> MaterialTheme.colorScheme.onPrimary
                                key == "CLR" -> MaterialTheme.colorScheme.onSurfaceVariant
                                else -> Color.White
                            }
                        )
                    }
                }
            }
        }
    }
}
