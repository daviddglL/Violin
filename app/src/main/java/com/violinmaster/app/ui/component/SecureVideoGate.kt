package com.violinmaster.app.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.violinmaster.app.ui.theme.AppLanguage
import com.violinmaster.app.ui.theme.Localization

// ----------------------------------------------------
// SECURE PASSCODE SETUP VIEW
// ----------------------------------------------------
@Composable
fun SecurePasscodeSetupScreen(
    errorMsg: String?,
    onSavePasscode: (String) -> Unit,
    onResetError: () -> Unit,
    appLanguage: AppLanguage = AppLanguage.ENGLISH
) {
    var rawInputPin by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = "Shield Guard",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(36.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = Localization.get("secure_student_pin", appLanguage),
            style = MaterialTheme.typography.titleMedium,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = Localization.get("secure_pin_desc", appLanguage),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp),
            lineHeight = 16.sp
        )

        Spacer(modifier = Modifier.height(28.dp))

        // Large 4-bullet visual representation
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            for (i in 0 until 4) {
                val filled = i < rawInputPin.length
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(
                            if (filled) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surfaceVariant
                        )
                        .border(1.dp, Color.White.copy(alpha = 0.1f), CircleShape)
                )
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        // Clean tactile security keyboard panel
        SecureKeyboardGrid(
            currentValue = rawInputPin,
            onValueChange = { newVal ->
                if (newVal.length <= 4) {
                    rawInputPin = newVal
                    onResetError()
                }
            },
            onValidate = {
                if (rawInputPin.length == 4) {
                    onSavePasscode(rawInputPin)
                    rawInputPin = ""
                }
            }
        )

        if (errorMsg != null) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = errorMsg,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }
    }
}

// ----------------------------------------------------
// SECURE AUTH LOGINS SCREEN
// ----------------------------------------------------
@Composable
fun SecureAuthenticationLockScreen(
    errorMsg: String?,
    onSubmitPin: (String) -> Boolean,
    onResetError: () -> Unit,
    onDeconstructLock: () -> Unit,
    appLanguage: AppLanguage = AppLanguage.ENGLISH
) {
    var rawInputPin by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.15f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = "Shield Locked",
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(36.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = Localization.get("enter_vault_pin", appLanguage),
            style = MaterialTheme.typography.titleMedium,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = Localization.get("vault_pin_desc", appLanguage),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp)
        )

        Spacer(modifier = Modifier.height(28.dp))

        // Pin bullet displays
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            for (i in 0 until 4) {
                val filled = i < rawInputPin.length
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(
                            if (filled) MaterialTheme.colorScheme.error
                            else MaterialTheme.colorScheme.surfaceVariant
                        )
                        .border(1.dp, Color.White.copy(alpha = 0.1f), CircleShape)
                )
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        SecureKeyboardGrid(
            currentValue = rawInputPin,
            onValueChange = { newVal ->
                if (newVal.length <= 4) {
                    rawInputPin = newVal
                    onResetError()
                }
            },
            onValidate = {
                if (rawInputPin.length == 4) {
                    val correct = onSubmitPin(rawInputPin)
                    rawInputPin = ""
                }
            }
        )

        if (errorMsg != null) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = errorMsg,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }
    }
}

// ----------------------------------------------------
// TACTILE CUSTOM KEYBOARD DESIGN
// ----------------------------------------------------
@Composable
fun SecureKeyboardGrid(
    currentValue: String,
    onValueChange: (String) -> Unit,
    onValidate: () -> Unit
) {
    val keys = listOf(
        "1", "2", "3",
        "4", "5", "6",
        "7", "8", "9",
        "CLR", "0", "OK"
    )

    Column(
        modifier = Modifier.width(260.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        val rows = keys.chunked(3)
        rows.forEach { rowKeys ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                rowKeys.forEach { key ->
                    val isAction = key == "CLR" || key == "OK"
                    val isOkActive = key == "OK" && currentValue.length == 4

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(54.dp)
                            .clip(RoundedCornerShape(12.dp))
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
                            .testTag("keypad_$key"),
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
