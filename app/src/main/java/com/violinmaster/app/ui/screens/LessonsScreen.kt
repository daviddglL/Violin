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
import com.violinmaster.app.ui.viewmodel.ChatViewModel
import com.violinmaster.app.di.AuthManager
import com.violinmaster.app.di.UserPreferencesManager
import com.violinmaster.app.di.NavigationManager
import com.violinmaster.app.ui.component.LessonVideoPlayer
import com.violinmaster.app.ui.component.StudentAssignmentsTab
import com.violinmaster.app.ui.component.TeacherDashboardTab
import com.violinmaster.app.ui.component.VirtualFingerboard

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
    userPreferencesManager: UserPreferencesManager,
    authManager: AuthManager,
    navigationManager: NavigationManager,
    chatViewModel: ChatViewModel,
    modifier: Modifier = Modifier
) {
    val lang by userPreferencesManager.appLanguage.collectAsState()
    val user by authManager.currentUser.collectAsState()
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
            LessonVideoPlayer(
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
                            "TEACHER" -> TeacherDashboardTab(assignmentVM = assignmentVM, userPreferencesManager = userPreferencesManager, authManager = authManager, chatViewModel = chatViewModel)
                            "STUDENT" -> StudentAssignmentsTab(
                                assignmentVM = assignmentVM,
                                authVM = authVM,
                                userPreferencesManager = userPreferencesManager,
                                authManager = authManager,
                                chatViewModel = chatViewModel,
                                onPlayTutorialVideo = { url, title ->
                                    activeTutorVideoUrl = url
                                    activeTutorVideoTitle = title
                                }
                            )
                            else -> CurriculumTab(
                                levelProgressList = levelProgressList,
                                isPracticing = isPracticing,
                                practiceCategory = practiceCategory,
                                appLanguage = lang,
                                practiceVM = practiceVM,
                                navigationManager = navigationManager
                            )
                        }
                    }
                    1 -> VirtualFingerboard(tunerVM = tunerVM, appLanguage = lang)
                    2 -> TheoryQuizTab(practiceVM = practiceVM, userPreferencesManager = userPreferencesManager)
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
    appLanguage: AppLanguage = AppLanguage.ENGLISH,
    practiceVM: PracticeViewModel,
    navigationManager: NavigationManager
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
                                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
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
                                                navigationManager.selectTab(0) // redirect home to see timer sweeping
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
// THE THEORY QUIZ TAB GAMEPLAY
// ----------------------------------------------------
@Composable
fun TheoryQuizTab(
    practiceVM: PracticeViewModel,
    userPreferencesManager: UserPreferencesManager
) {
    val lang by userPreferencesManager.appLanguage.collectAsState()
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


