package com.violinmaster.app.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.violinmaster.app.di.UserPreferencesManager
import com.violinmaster.app.domain.model.Instrument
import com.violinmaster.app.ui.screens.BorderStrokeHelper
import com.violinmaster.app.ui.theme.AppLanguage
import com.violinmaster.app.ui.viewmodel.PracticeViewModel

// ----------------------------------------------------
// THE THEORY QUIZ TAB GAMEPLAY
// ----------------------------------------------------
@Composable
fun TheoryQuizTab(
    practiceVM: PracticeViewModel,
    userPreferencesManager: UserPreferencesManager,
    instrument: Instrument = Instrument.VIOLIN
) {
    val lang by userPreferencesManager.appLanguage.collectAsState()
    val isEs = lang == AppLanguage.SPANISH

    // Load the instrument-specific quiz bank
    val currentQuizBank = quizBanks[instrument] ?: quizBanks[Instrument.VIOLIN]!!

    var questionPointerIndex by rememberSaveable { mutableStateOf(0) }
    var userChosenOptionSelected by rememberSaveable { mutableStateOf<Int?>(null) }
    var quizTurnAnswered by rememberSaveable { mutableStateOf(false) }
    var currentScoreVal by rememberSaveable { mutableStateOf(0) }
    var liveStreakCount by rememberSaveable { mutableStateOf(0) }
    var quizIsUnderwayFinished by rememberSaveable { mutableStateOf(false) }

    // Reset quiz state when the instrument changes
    LaunchedEffect(instrument) {
        questionPointerIndex = 0
        userChosenOptionSelected = null
        quizTurnAnswered = false
        currentScoreVal = 0
        liveStreakCount = 0
        quizIsUnderwayFinished = false
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        if (!quizIsUnderwayFinished) {
            val q = currentQuizBank[questionPointerIndex]
            val dispQuestion = if (isEs) q.questionEs else q.question
            val dispOptions = if (isEs) q.optionsEs else q.options
            val dispExplanation = if (isEs) q.explanationEs else q.explanation

            // Top Status stats
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isEs) "PREGUNTA ${questionPointerIndex + 1} DE ${currentQuizBank.size}" else "QUESTION ${questionPointerIndex + 1} OF ${currentQuizBank.size}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (isEs) "Puntos: $currentScoreVal pts" else "Score: $currentScoreVal pts",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Progress Indicators dots
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                currentQuizBank.forEachIndexed { idx, _ ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(6.dp)
                            .clip(CircleShape)
                            .background(
                                if (idx <= questionPointerIndex) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                            )
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Question Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                border = BorderStrokeHelper()
            ) {
                Text(
                    text = dispQuestion,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    modifier = Modifier.padding(18.dp),
                    lineHeight = 22.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Options selection list
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                dispOptions.forEachIndexed { oIdx, opt ->
                    val isChosen = userChosenOptionSelected == oIdx
                    val correctIdx = q.correctAnswerIndex
                    val optionBgColor = when {
                        !quizTurnAnswered && isChosen -> MaterialTheme.colorScheme.primary
                        quizTurnAnswered && oIdx == correctIdx -> Color(0xFF2E7D32) // Correct Option Green highlight
                        quizTurnAnswered && isChosen && isChosen -> Color(0xFFC53030) // Wrong selected Option Red
                        else -> MaterialTheme.colorScheme.surface
                    }
                    val optionTextColor = when {
                        !quizTurnAnswered && isChosen -> MaterialTheme.colorScheme.onPrimary
                        quizTurnAnswered && oIdx == correctIdx -> Color.White
                        quizTurnAnswered && isChosen -> Color.White
                        else -> Color.White
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(optionBgColor)
                            .border(
                                1.dp,
                                if (isChosen) Color.Transparent else Color(0xFF49454F),
                                RoundedCornerShape(14.dp)
                            )
                            .clickable(enabled = !quizTurnAnswered) {
                                userChosenOptionSelected = oIdx
                            }
                            .padding(16.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = when (oIdx) {
                                    0 -> "A"
                                    1 -> "B"
                                    2 -> "C"
                                    else -> "D"
                                },
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.15f)),
                                textAlign = TextAlign.Center,
                                color = optionTextColor,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodySmall
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = opt,
                                style = MaterialTheme.typography.bodyMedium,
                                color = optionTextColor,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Action Verification Buttons
            if (!quizTurnAnswered) {
                Button(
                    onClick = {
                        if (userChosenOptionSelected != null) {
                            quizTurnAnswered = true
                            if (userChosenOptionSelected == q.correctAnswerIndex) {
                                currentScoreVal += 20
                                liveStreakCount += 1
                            } else {
                                liveStreakCount = 0
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    enabled = userChosenOptionSelected != null
                ) {
                    Text(
                        text = if (isEs) "VERIFICAR RESPUESTA" else "VERIFY ANSWER",
                        fontWeight = FontWeight.Bold
                    )
                }
            } else {
                // Display explanation and Next button
                val explanationLabel = if (userChosenOptionSelected == q.correctAnswerIndex) {
                    if (isEs) "✓ ¡Excelente respuesta!" else "✓ Excellent Guess!"
                } else {
                    if (isEs) "✗ ¡Corrección!" else "✗ Correction!"
                }
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.03f)),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStrokeHelper()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = explanationLabel,
                            color = if (userChosenOptionSelected == q.correctAnswerIndex) Color(0xFF81C784) else Color(0xFFF2B8B5),
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelSmall
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = dispExplanation,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Button(
                    onClick = {
                        if (questionPointerIndex + 1 < currentQuizBank.size) {
                            questionPointerIndex += 1
                            userChosenOptionSelected = null
                            quizTurnAnswered = false
                        } else {
                            practiceVM.earnPoints(currentScoreVal)
                            quizIsUnderwayFinished = true
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    val nextLabel = if (questionPointerIndex + 1 < currentQuizBank.size) {
                        if (isEs) "SIGUIENTE PREGUNTA" else "NEXT QUESTION"
                    } else {
                        if (isEs) "MOSTRAR RESUMEN" else "SHOW FINISHED SUMMARY"
                    }
                    Text(
                        text = nextLabel,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        } else {
            // Summary finished card view
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStrokeHelper()
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("🎓", fontSize = 60.sp)
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = if (isEs) "¡Cuestionario Completado!" else "Theory Quiz Completed!",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (isEs) "Puntaje obtenido: $currentScoreVal puntos de 100" else "You scored $currentScoreVal points out of 100",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Badge Reward item (unlocked if user gets high scores)
                    if (currentScoreVal >= 80) {
                        Surface(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            contentColor = MaterialTheme.colorScheme.primary,
                            shape = RoundedCornerShape(16.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("👑", fontSize = 24.sp)
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = if (isEs) "Insignia de Maestro Académico" else "Theory Academic Master Badge",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.White
                                    )
                                    Text(
                                        text = if (isEs) "¡Conocimiento técnico perfecto desbloqueado!" else "Perfect technical literacy unlocked!",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }

                Button(
                    onClick = {
                        questionPointerIndex = 0
                        userChosenOptionSelected = null
                        quizTurnAnswered = false
                        currentScoreVal = 0
                        liveStreakCount = 0
                        quizIsUnderwayFinished = false
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = if (isEs) "INTENTAR DE NUEVO" else "TRY AGAIN",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}
