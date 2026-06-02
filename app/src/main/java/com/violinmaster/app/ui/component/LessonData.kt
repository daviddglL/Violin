package com.violinmaster.app.ui.component

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
