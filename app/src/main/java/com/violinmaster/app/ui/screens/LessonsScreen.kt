package com.violinmaster.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.violinmaster.app.data.LessonProgress
import com.violinmaster.app.ui.theme.Localization
import com.violinmaster.app.ui.theme.AppLanguage
import com.violinmaster.app.ui.viewmodel.PracticeViewModel
import com.violinmaster.app.ui.viewmodel.TunerViewModel
import com.violinmaster.app.ui.viewmodel.AssignmentViewModel
import com.violinmaster.app.ui.viewmodel.AuthViewModel
import com.violinmaster.app.di.SessionManager

// ----------------------------------------------------
// CURRICULUM RICH CONTENT MODEL
// ----------------------------------------------------
data class LessonDetailsContent(
    val description: String,
    val objectives: List<String>,
    val subtopics: List<String>
)

val lessonDetailsMap = mapOf(
    "beg_1" to LessonDetailsContent(
        description = "Lay a flawless foundation. Learn to hold the instrument with structural ease and coordinate straight, relaxed bowing movements.",
        objectives = listOf(
            "Proper violin chinrest placement without neck tension",
            "Balanced bow-hold (flexible bent thumb and pinky cushion)",
            "Straight 90° bow lines parallel to the bridge"
        ),
        subtopics = listOf(
            "Violin and Bow Anatomy & Stance",
            "The Bow-Hold Silent Exercises",
            "Open String Whole-bow Drills"
        )
    ),
    "beg_2" to LessonDetailsContent(
        description = "Discover absolute finger positions in the key of D Major. Master basic muscle memory for half-steps and whole-steps of first position.",
        objectives = listOf(
            "Align 1st, 2nd, and 3rd finger tapes precisely on the fingerboard",
            "Distinguish between whole steps (spatial gap) and half steps (touching)",
            "Validate intonation using adjacent open string resonates"
        ),
        subtopics = listOf(
            "First Finger (E/B) placement",
            "High Second Finger (F# / C#) pattern",
            "Third Finger (G / D) octaves resonance"
        )
    ),
    "beg_3" to LessonDetailsContent(
        description = "Consolidate your bowing and finger placement! Read and coordinate simple notation with primary string crossings and slurs.",
        objectives = listOf(
            "Implement basic note reading for G, D, A, and E string pitches",
            "Coordinate continuous bowing during active finger shifts",
            "Apply two-note slurs (legato) in a single bow stroke"
        ),
        subtopics = listOf(
            "Read Quarter and Eighth note rhythms",
            "Two-note Slurred bowing transitions",
            "Traditional Folk Melodies (e.g., Ode to Joy)"
        )
    ),
    "int_1" to LessonDetailsContent(
        description = "Transition from tight finger pressure to fluid warmth. Master wrist and arm oscillations to sing with rich expressive vibrato.",
        objectives = listOf(
            "Isolate thumb tension and maintain loose finger joints",
            "Establish steady, symmetrical back-and-forth joint oscillations",
            "Vary pitch oscillations smoothly matching standard tempo rates"
        ),
        subtopics = listOf(
            "Left Thumb Release Technique",
            "Sliding Polishing-Neck isolated exercises",
            "Slow-pulsed Wrist Vibrato drills"
        )
    ),
    "int_2" to LessonDetailsContent(
        description = "Navigate beyond the first position. Learn safe shifting pathways to the third position with intermediate pitch checks.",
        objectives = listOf(
            "Slide smoothly without gripping or squeezing the violin neck",
            "Anchor your wrist or palm against the instrument shoulder",
            "Verify shifted pitch precision using unison and overtone harmonics"
        ),
        subtopics = listOf(
            "The GLIDE Finger intermediate slide",
            "Third Position hand anchor points",
            "Harmonic Ring spot calibration checks"
        )
    ),
    "int_3" to LessonDetailsContent(
        description = "Begin harmony playing. Coordinate two fingers and control bow weight to play dual-string chords with clean resonance.",
        objectives = listOf(
            "Angle the bow hair specifically touching two adjacent strings",
            "Vary weight distribution to balance the volume of both voices",
            "Maintain pure vertical finger placement preventing adjacent muting"
        ),
        subtopics = listOf(
            "Tuning perfect fifth double stops",
            "Single-finger double-stop templates",
            "Moving scales in parallel thirds"
        )
    ),
    "adv_1" to LessonDetailsContent(
        description = "Build a formidable bowing toolset. Master accented off-the-string bouncing (Spiccato) and sharp, biting attack bows (Martelé).",
        objectives = listOf(
            "Locate the natural bounce equilibrium spot of your bow stick",
            "Control Martelé start-bite using index finger bow pinch-and-release",
            "Maintain fluid wrist string-crossings during rapid bow hops"
        ),
        subtopics = listOf(
            "Martelé forearm release bites",
            "Spiccato natural gravity bounce balance",
            "Rapid string crossings with hopping accents"
        )
    ),
    "adv_2" to LessonDetailsContent(
        description = "Put your fingers and bow to the ultimate test. Learn parts of Paganini's legendary theme, exploring velocity and acrobatics.",
        objectives = listOf(
            "Perform rapid arpeggiated string hops across 3 strings",
            "Utilize pivot finger transitions to shift high-speed intervals",
            "Coordinate extreme left-right hand synchronization at 120+ BPM"
        ),
        subtopics = listOf(
            "Paganini A Minor Theme arpeggios",
            "Pivot Finger shifting coordinates",
            "Acoustic velocity and synchronization scales"
        )
    ),
    "adv_3" to LessonDetailsContent(
        description = "Climb to the stratosphere! Master fingerboard geometry for 5th, 6th, and 7th positions with extreme intonation control.",
        objectives = listOf(
            "Reposition the left thumb under the neck curvature for high reach",
            "Compact finger groupings together to adjust for shrinking step scales",
            "Control bow speed and placement near the bridge for high-pitch focus"
        ),
        subtopics = listOf(
            "Thumb repositioning for high-register clearance",
            "Compact finger closeness calibration",
            "In-tune high register scales & arpeggios"
        )
    )
)

// ----------------------------------------------------
// FINGERBOARD INTERACTIVE DATA MODEL
// ----------------------------------------------------
data class FingeringNote(
    val finger: String, 
    val noteName: String,
    val frequency: Double,
    val description: String
)

val fingeringMap = mapOf(
    "G" to listOf(
        FingeringNote("Open", "G3", 196.00, "Base note G of the violin wood, deep and resonant."),
        FingeringNote("1st Pos", "A3", 220.00, "Whole step from G open string. Active unison check."),
        FingeringNote("Low 2nd", "B♭3", 233.08, "Half step from 1st finger. Used in minor scales."),
        FingeringNote("High 2nd", "B3", 246.94, "Whole step from 1st finger. Major scale interval."),
        FingeringNote("3rd Pos", "C4", 261.63, "Half step from High 2nd finger. Forms octave check."),
        FingeringNote("4th Pos", "D4", 293.66, "Perfect unison double stop resonance with open D.")
    ),
    "D" to listOf(
        FingeringNote("Open", "D4", 293.66, "Warm central string, crucial for fundamental melodies."),
        FingeringNote("1st Pos", "E4", 329.63, "Whole step from D open. Anchors the D Major hand frame."),
        FingeringNote("Low 2nd", "F4", 349.23, "Half step from 1st finger, minor third resonance."),
        FingeringNote("High 2nd", "F♯4", 369.99, "Whole step from 1st finger. Standard D Major third."),
        FingeringNote("3rd Pos", "G4", 392.00, "Half step from High 2nd finger. Unison with G open."),
        FingeringNote("4th Pos", "A4", 440.00, "Unison resonance of absolute pitch with open A string.")
    ),
    "A" to listOf(
        FingeringNote("Open", "A4", 440.00, "The universal reference tuning pitch for orchestras."),
        FingeringNote("1st Pos", "B4", 493.88, "Whole step from A. Used extensively in first melodies."),
        FingeringNote("Low 2nd", "C5", 523.25, "Half step from 1st finger. Central C note in first-pos."),
        FingeringNote("High 2nd", "C♯5", 554.37, "Whole step from 1st finger. Major third in A Major."),
        FingeringNote("3rd Pos", "D5", 587.33, "Half step from High 2nd. Clean octave resonance check."),
        FingeringNote("4th Pos", "E5", 659.25, "Matches the open E pitch precisely. Tests pinky strength.")
    ),
    "E" to listOf(
        FingeringNote("Open", "E5", 659.25, "Bright, brilliant, projecting steel string pitch."),
        FingeringNote("1st Pos", "F♯5", 739.99, "Whole step from E. Requires soft high finger curve."),
        FingeringNote("Low 2nd", "G5", 783.99, "Half step from 1st. Brilliant, crisp minor third pitch."),
        FingeringNote("High 2nd", "G♯5", 830.61, "Whole step from 1st. High major third resonance."),
        FingeringNote("3rd Pos", "A5", 880.00, "One octave higher than open A. Check projection rings."),
        FingeringNote("4th Pos", "B5", 987.77, "Very high first-pos pitch. Requires soft, accurate touch.")
    )
)

// ----------------------------------------------------
// THEORY QUIZ MODEL AND QUESTIONS
// ----------------------------------------------------
data class QuizQuestion(
    val id: Int,
    val question: String,
    val questionEs: String,
    val options: List<String>,
    val optionsEs: List<String>,
    val correctAnswerIndex: Int,
    val explanation: String,
    val explanationEs: String
)

val quizQuestions = listOf(
    QuizQuestion(
        id = 1,
        question = "What are the standard tuning pitches for the four strings of a violin (low to high)?",
        questionEs = "¿Cuáles son los tonos de afinación estándar para las cuatro cuerdas de un violín (de grave a agudo)?",
        options = listOf(
            "G3 (196Hz), D4 (294Hz), A4 (440Hz), E5 (659Hz)",
            "C3 (130Hz), G3 (196Hz), D4 (294Hz), A4 (440Hz)",
            "E3 (164Hz), A3 (220Hz), D4 (294Hz), G4 (392Hz)",
            "G3 (196Hz), C4 (262Hz), F4 (349Hz), B4 (494)"
        ),
        optionsEs = listOf(
            "Sol3 (196Hz), Re4 (294Hz), La4 (440Hz), Mi5 (659Hz)",
            "Do3 (130Hz), Sol3 (196Hz), Re4 (294Hz), La4 (440Hz)",
            "Mi3 (164Hz), La3 (220Hz), Re4 (294Hz), Sol4 (392Hz)",
            "Sol3 (196Hz), Do4 (262Hz), Fa4 (349Hz), Si4 (494Hz)"
        ),
        correctAnswerIndex = 0,
        explanation = "The standard violin strings are tuned in intervals of perfect fifths: G3 (196Hz), D4 (293.7Hz), A4 (440Hz), and E5 (659.3Hz).",
        explanationEs = "Las cuerdas estándar del violín se afinan en intervalos de quintas perfectas: Sol3 (196Hz), Re4 (293.7Hz), La4 (440Hz) y Mi5 (659.3Hz)."
    ),
    QuizQuestion(
        id = 2,
        question = "In 'First Position' on the D string, what pitch is played with a High 2nd Finger?",
        questionEs = "En la 'Primera Posición' de la cuerda Re, ¿qué tono se toca con el segundo dedo alto?",
        options = listOf(
            "F natural (F4)",
            "F sharp (F♯4)",
            "G natural (G4)",
            "E natural (E4)"
        ),
        optionsEs = listOf(
            "Fa natural (F4)",
            "Fa sostenido (F♯4)",
            "Sol natural (G4)",
            "Mi natural (E4)"
        ),
        correctAnswerIndex = 1,
        explanation = "A High 2nd finger on the D string raises the pitch by a whole step from E (1st finger) to F sharp (F♯4), which represents the major third in D major.",
        explanationEs = "Un segundo dedo alto en la cuerda Re eleva el tono un tono entero desde Mi (primer dedo) a Fa sostenido (F♯4), que representa la tercera mayor en Re mayor."
    ),
    QuizQuestion(
        id = 3,
        question = "What is the term for the expressive, pulsating pitch variation produced by oscillating the left hand fingers?",
        questionEs = "¿Cuál es el término para la variación expresiva y pulsante de tono producida al oscilar los dedos de la mano izquierda?",
        options = listOf(
            "Tremolo",
            "Pizzicato",
            "Vibrato",
            "Glissando"
        ),
        optionsEs = listOf(
            "Trémolo",
            "Pizzicato",
            "Vibrato",
            "Glissando"
        ),
        correctAnswerIndex = 2,
        explanation = "Vibrato is the subtle pitch oscillation used to warm and beautify the violin tone. It requires isolated joint flexibility.",
        explanationEs = "El vibrato es la sutil oscilación del tono utilizada para dar calidez y embellecer el sonido del violín. Requiere flexibilidad en las articulaciones de la mano izquierda."
    ),
    QuizQuestion(
        id = 4,
        question = "How do violinists check an octave on the fingerboard for pitch accuracy (intonation)?",
        questionEs = "¿Cómo comprueban los violinistas una octava en el diapasón para verificar la entonación?",
        options = listOf(
            "By comparing a 3rd finger note with the lower adjacent open string",
            "By bowing two open strings at the same time to hear beats",
            "By sliding the pinky randomly until it squeaks",
            "By adjusting tailpiece fine tuners rapidly"
        ),
        optionsEs = listOf(
            "Comparando una nota del tercer dedo con la cuerda al aire adyacente inferior",
            "Tocando dos cuerdas al aire al mismo tiempo para escuchar batimentos",
            "Deslizando el meñique al azar hasta que chirríe",
            "Ajustando los microafinadores de manera rápida"
        ),
        correctAnswerIndex = 0,
        explanation = "Checking a third-finger note (e.g., G4 on D string) against the lower adjacent open string (open G string) creates a pure octave acoustic resonance.",
        explanationEs = "Al comparar una nota del tercer dedo (por ejemplo, Sol4 en la cuerda Re) con la cuerda al aire adyacente inferior (cuerda Sol al aire) se genera una octava de resonancia acústica pura."
    ),
    QuizQuestion(
        id = 5,
        question = "Which bowing style involves crisp, forearm releases where the bow stick pinches then launches to cause a hammering sound?",
        questionEs = "¿Qué estilo de arco implica golpes nítidos de antebrazo donde la vara del arco presiona y luego se libera para causar un sonido de martilleo?",
        options = listOf(
            "Spiccato",
            "Détaché",
            "Martelé",
            "Col Legno"
        ),
        optionsEs = listOf(
            "Spiccato",
            "Détaché",
            "Martelé",
            "Col Legno"
        ),
        correctAnswerIndex = 2,
        explanation = "Martelé (meaning 'hammered') is an accented bow stroke where the player pinches then instantly releases pressure to form a sharp launch.",
        explanationEs = "Martelé (que significa 'martillado') es un golpe de arco acentuado de antebrazo donde el intérprete presiona y luego libera instantáneamente para formar un ataque agudo."
    )
)

// ----------------------------------------------------
// MAIN SCREEN IMPLEMENTATION
// ----------------------------------------------------
@Composable
fun LessonsScreen(
    practiceVM: PracticeViewModel,
    tunerVM: TunerViewModel,
    authVM: AuthViewModel,
    assignmentVM: AssignmentViewModel,
    sessionManager: SessionManager,
    modifier: Modifier = Modifier
) {
    val lang by sessionManager.appLanguage.collectAsState()
    val user by sessionManager.currentUser.collectAsState()
    val levelProgressList by practiceVM.allLevelProgress.collectAsState()
    val isPracticing by practiceVM.isPracticing.collectAsState()
    val practiceCategory by practiceVM.practiceCategoryName.collectAsState()

    var activeTabSubIndex by rememberSaveable { mutableStateOf(0) } // 0: Curriculum, 1: Fingerboard, 2: Theory Quiz, 3: Masterclass

    // Student tutor video target states
    var activeTutorVideoUrl by remember { mutableStateOf<String?>(null) }
    var activeTutorVideoTitle by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        if (activeTutorVideoUrl != null && activeTutorVideoTitle != null) {
            SecureMediaPlaybackConsole(
                videoTitle = activeTutorVideoTitle ?: "",
                signedUrl = activeTutorVideoUrl ?: "",
                onClose = {
                    activeTutorVideoUrl = null
                    activeTutorVideoTitle = null
                },
                appLanguage = lang
            )
        } else {
            Column(modifier = Modifier.fillMaxSize()) {
                // Shared Screen Header
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Text(
                        text = Localization.get("tab_lessons", lang).uppercase(java.util.Locale.ROOT),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    )
                    Text(
                        text = when (activeTabSubIndex) {
                            0 -> {
                                when (user?.role) {
                                    "TEACHER" -> Localization.get("teacher_dashboard_title", lang)
                                    "STUDENT" -> Localization.get("student_dashboard_title", lang)
                                    else -> Localization.get("lessons_header_freelancer", lang)
                                }
                            }
                            1 -> Localization.get("smart_tuner", lang)
                            2 -> Localization.get("theory_quest_quiz", lang)
                            else -> Localization.get("premium_masterclass", lang)
                        },
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = when (activeTabSubIndex) {
                            0 -> {
                                when (user?.role) {
                                    "TEACHER" -> Localization.get("teacher_tab_desc", lang)
                                    "STUDENT" -> Localization.get("student_tab_desc", lang)
                                    else -> Localization.get("freelancer_tab_desc", lang)
                                }
                            }
                            1 -> Localization.get("tuner_desc", lang)
                            2 -> Localization.get("theory_quiz_desc", lang)
                            else -> Localization.get("premium_masterclass", lang)
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Premium Navigation Tabs
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val tabLabels = listOf(
                        if (user?.role == "TEACHER") "🎓 INSTRUCT" else "📖 LESSONS",
                        "🎯 NECK",
                        "💡 QUIZ",
                        "🔒 VIDEOS"
                    )
                    tabLabels.forEachIndexed { index, title ->
                        val isSelected = activeTabSubIndex == index
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                                .clickable { activeTabSubIndex = index }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 10.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Tab Content Switch
                when (activeTabSubIndex) {
                    0 -> {
                        when (user?.role) {
                            "TEACHER" -> TeacherDashboardTab(assignmentVM = assignmentVM, sessionManager = sessionManager)
                            "STUDENT" -> StudentAssignmentsTab(
                                assignmentVM = assignmentVM,
                                authVM = authVM,
                                sessionManager = sessionManager,
                                onPlayTutorialVideo = { url, title ->
                                    activeTutorVideoUrl = url
                                    activeTutorVideoTitle = title
                                }
                            )
                            else -> CurriculumTab(
                                levelProgressList = levelProgressList,
                                isPracticing = isPracticing,
                                practiceCategory = practiceCategory,
                                appLanguage = lang
                            )
                        }
                    }
                    1 -> FingerboardTab(tunerVM = tunerVM, appLanguage = lang)
                    2 -> TheoryQuizTab(practiceVM = practiceVM, sessionManager = sessionManager)
                    3 -> MasterclassTab(authViewModel = authVM)
                }
            }
        }
    }
}

// ----------------------------------------------------
// EXTRA SUBVIEWS & COMPONENTS
// ----------------------------------------------------

@Composable
fun CurriculumTab(
    levelProgressList: List<LessonProgress>,
    isPracticing: Boolean,
    practiceCategory: String,
    appLanguage: AppLanguage = AppLanguage.ENGLISH
) {
    val groupedLessons = levelProgressList.groupBy { it.difficulty }
    val difficultyOrder = listOf("Beginner", "Intermediate", "Advanced")

    // Dynamic expanded item tracking
    var expandedLessonId by remember { mutableStateOf<String?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ----------------------------------------------------
        // JOURNEY OVERVIEW DASHBOARD PANEL
        // ----------------------------------------------------
        item {
            val totalSeconds = levelProgressList.sumOf { it.totalPracticedSeconds }
            val completedCount = levelProgressList.count { it.completed }
            val totalCount = levelProgressList.size.coerceAtLeast(1)
            val completionPercentageValue = (completedCount.toFloat() / totalCount.toFloat())
            val progressAnim by animateFloatAsState(targetValue = completionPercentageValue, label = "Progress Circle")

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("journey_tracker_dashboard"),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                border = BorderStrokeHelper()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left Circular Progress Ring
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(80.dp)
                    ) {
                        CircularProgressIndicator(
                            progress = { progressAnim },
                            modifier = Modifier.fillMaxSize(),
                            color = MaterialTheme.colorScheme.primary,
                            strokeWidth = 8.dp,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant,
                            strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                        )
                        Text(
                            text = "${(completedCount * 100) / totalCount}%",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    // Right Summary Metadata details
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = Localization.get("learning_journey_progress", appLanguage),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = String.format(Localization.get("modules_progress_format", appLanguage), completedCount, totalCount),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = String.format(Localization.get("total_bow_time_format", appLanguage), totalSeconds / 60),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        // Dynamic encouraging message
                        Text(
                            text = when {
                                completionPercentageValue <= 0.25f -> Localization.get("encouragement_seed", appLanguage)
                                completionPercentageValue <= 0.60f -> Localization.get("encouragement_warm", appLanguage)
                                completionPercentageValue <= 0.85f -> Localization.get("encouragement_habits", appLanguage)
                                else -> Localization.get("encouragement_virtuoso", appLanguage)
                            },
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }
        }

        // Seeding groups
        for (difficulty in difficultyOrder) {
            val lessons = groupedLessons[difficulty] ?: emptyList()
            if (lessons.isNotEmpty()) {
                item {
                    Text(
                        text = String.format(Localization.get("syllabus_suffix", appLanguage), difficulty.uppercase()),
                        style = MaterialTheme.typography.labelLarge,
                        color = when (difficulty) {
                            "Beginner" -> Color(0xFF81C784)
                            "Intermediate" -> MaterialTheme.colorScheme.primary
                            else -> Color(0xFFF2B8B5)
                        },
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                    )
                }

                items(lessons) { lesson ->
                    val extraDetails = lessonDetailsMap[lesson.lessonId]
                    val isExpanded = expandedLessonId == lesson.lessonId

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("lesson_item_${lesson.lessonId}")
                            .clickable {
                                expandedLessonId = if (isExpanded) null else lesson.lessonId
                            },
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        border = if (isExpanded) {
                            androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                        } else {
                            BorderStrokeHelper()
                        }
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            // Row Header (Title, check indicator)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (lesson.completed) "✅" else "🎻",
                                    fontSize = 20.sp,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = lesson.lessonTitle,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = Color.White,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = String.format(Localization.get("level_format", appLanguage), difficulty) + " • ",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = String.format(Localization.get("minutes_practiced_format", appLanguage), lesson.totalPracticedSeconds / 60),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                                Text(
                                    text = if (isExpanded) "▲" else "▼",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }

                            // Expandable Details Section
                            AnimatedVisibility(visible = isExpanded) {
                                Column(modifier = Modifier.padding(top = 16.dp)) {
                                    Divider(color = MaterialTheme.colorScheme.surfaceVariant)
                                    Spacer(modifier = Modifier.height(12.dp))

                                    if (extraDetails != null) {
                                        // Description
                                        Text(
                                            text = extraDetails.description,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            lineHeight = 18.sp
                                        )
                                        Spacer(modifier = Modifier.height(12.dp))

                                        // Explicit Learning Objectives
                                    Text(
                                        text = "🎯 " + Localization.get("stage_learning_objectives", appLanguage),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary,
                                            letterSpacing = 0.5.sp
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        extraDetails.objectives.forEach { obj ->
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 2.dp),
                                                verticalAlignment = Alignment.Top
                                            ) {
                                                Text(
                                                    text = "✓",
                                                    color = Color(0xFF81C784),
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(end = 6.dp)
                                                )
                                                Text(
                                                    text = obj,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(12.dp))

                                        // Sub-topics checklist
                                    Text(
                                        text = "📝 " + Localization.get("included_core_drills", appLanguage),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.secondary,
                                            letterSpacing = 0.5.sp
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        extraDetails.subtopics.forEach { sub ->
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "◽",
                                                    modifier = Modifier.padding(end = 6.dp),
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                                Text(
                                                    text = sub,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(16.dp))

                                    // Activity Action Buttons
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Manual Status Toggle Complete
                                        Button(
                                            onClick = { practiceVM.toggleLessonStatus(lesson.lessonId, !lesson.completed) },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = if (lesson.completed) Color(0xFF2E7D32).copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant,
                                                contentColor = if (lesson.completed) Color(0xFF81C784) else MaterialTheme.colorScheme.onSurfaceVariant
                                            ),
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier.testTag("check_lesson_${lesson.lessonId}")
                                        ) {
                                            Text(if (lesson.completed) "✓ " + Localization.get("lesson_completed_button", appLanguage) else Localization.get("mark_done_button", appLanguage))
                                        }

                                        // Start Live Practice Session Timer
                                        val isCurrent = isPracticing && practiceCategory == lesson.lessonTitle
                                        Button(
                                            onClick = {
                                                practiceVM.startPracticeTimer(lesson.lessonTitle)
                                                sessionManager.selectTab(0) // redirect home to see timer sweeping
                                            },
                                            shape = RoundedCornerShape(10.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = if (isCurrent) Color(0xFF2E7D32) else MaterialTheme.colorScheme.primary,
                                                contentColor = Color.White
                                            ),
                                            modifier = Modifier.testTag("practice_button_${lesson.lessonId}")
                                        ) {
                                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(if (isCurrent) "▶ " + Localization.get("practicing_label", appLanguage) else Localization.get("drill_time_button", appLanguage))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

// ----------------------------------------------------
// INTERACTIVE FINGERBOARD CHART TAB
// ----------------------------------------------------
@Composable
fun FingerboardTab(
    tunerVM: TunerViewModel,
    appLanguage: AppLanguage = AppLanguage.ENGLISH
) {
    var selectedFretString by remember { mutableStateOf("A") } // G, D, A, E
    val notesAndPositions = fingeringMap[selectedFretString] ?: emptyList()
    var activeFingeringNote by remember { mutableStateOf<FingeringNote?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // String selection bar
        Text(
            text = Localization.get("select_current_string", appLanguage),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.secondary,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            listOf("G", "D", "A", "E").forEach { s ->
                val isSelected = selectedFretString == s
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface)
                        .border(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else Color(0xFF49454F), RoundedCornerShape(12.dp))
                        .clickable {
                            selectedFretString = s
                            activeFingeringNote = null // reset selection
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = String.format(Localization.get("string_label_format", appLanguage), s),
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else Color.White,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Visual Fingerboard Graphic representation
        Text(
            text = Localization.get("virtual_fingerboard", appLanguage),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.secondary,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(8.dp))

        // Simulated neck box
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(BorderStrokeHelper(), RoundedCornerShape(16.dp)),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1005)) // Beautiful nutwood fingerboard color vibe!
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp, horizontal = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Layout vertical representation of finger slots
                notesAndPositions.forEach { fNote ->
                    val isFingerActive = activeFingeringNote?.finger == fNote.finger
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isFingerActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.3f) else Color.Transparent)
                            .clickable {
                                activeFingeringNote = fNote
                                // Play study note pitch sound automatically
                                tunerVM.playCustomFrequency(fNote.frequency)
                            }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Circular Finger Label Tap Spot
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .background(
                                        if (isFingerActive) MaterialTheme.colorScheme.primary else Color(0xFF493628),
                                        CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = fNote.finger.take(1),
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontSize = 13.sp
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = fNote.finger,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = Localization.get("position_relative_tape", appLanguage),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Pitch note display bubble
                        Box(
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = String.format(Localization.get("note_frequency_format", appLanguage), fNote.noteName, fNote.frequency.toInt()),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Dynamic Interactive Note Analysis Details Panel
        AnimatedVisibility(visible = activeFingeringNote != null) {
            val fn = activeFingeringNote
            if (fn != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    border = BorderStrokeHelper()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = String.format(Localization.get("string_target_format", appLanguage), selectedFretString, fn.noteName),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            Text(
                                text = String.format(Localization.get("frequency_match_format", appLanguage), fn.frequency),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            // Mute sound action
                            Button(
                                onClick = { tunerVM.stopAudioEngineTone() },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.error,
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Mute Tone", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = fn.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        // Study Tip Indicator
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
                                .padding(8.dp)
                        ) {
                            Text("💡", fontSize = 16.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = Localization.get("listen_and_match", appLanguage),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

// ----------------------------------------------------
// THE THEORY QUIZ TAB GAMEPLAY
// ----------------------------------------------------
@Composable
fun TheoryQuizTab(
    practiceVM: PracticeViewModel,
    sessionManager: SessionManager
) {
    val lang by sessionManager.appLanguage.collectAsState()
    val isEs = lang == com.violinmaster.app.ui.theme.AppLanguage.SPANISH

    var questionPointerIndex by rememberSaveable { mutableStateOf(0) }
    var userChosenOptionSelected by rememberSaveable { mutableStateOf<Int?>(null) }
    var quizTurnAnswered by rememberSaveable { mutableStateOf(false) }
    var currentScoreVal by rememberSaveable { mutableStateOf(0) }
    var liveStreakCount by rememberSaveable { mutableStateOf(0) }
    var quizIsUnderwayFinished by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        if (!quizIsUnderwayFinished) {
            val q = quizQuestions[questionPointerIndex]
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
                    text = if (isEs) "PREGUNTA ${questionPointerIndex + 1} DE ${quizQuestions.size}" else "QUESTION ${questionPointerIndex + 1} OF ${quizQuestions.size}",
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
                quizQuestions.forEachIndexed { idx, _ ->
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
                        if (questionPointerIndex + 1 < quizQuestions.size) {
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
                    val nextLabel = if (questionPointerIndex + 1 < quizQuestions.size) {
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


