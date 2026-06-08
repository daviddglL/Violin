package com.violinmaster.app.ui.component

import com.violinmaster.app.domain.model.Instrument

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

// Backwards-compatible alias for the violin quiz bank
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

val quizBanks: Map<Instrument, List<QuizQuestion>> = mapOf(
    Instrument.VIOLIN to listOf(
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
            options = listOf("Tremolo", "Pizzicato", "Vibrato", "Glissando"),
            optionsEs = listOf("Trémolo", "Pizzicato", "Vibrato", "Glissando"),
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
            options = listOf("Spiccato", "Détaché", "Martelé", "Col Legno"),
            optionsEs = listOf("Spiccato", "Détaché", "Martelé", "Col Legno"),
            correctAnswerIndex = 2,
            explanation = "Martelé (meaning 'hammered') is an accented bow stroke where the player pinches then instantly releases pressure to form a sharp launch.",
            explanationEs = "Martelé (que significa 'martillado') es un golpe de arco acentuado de antebrazo donde el intérprete presiona y luego libera instantáneamente para formar un ataque agudo."
        )
    ),
    Instrument.VIOLA to listOf(
        QuizQuestion(
            id = 1,
            question = "What are the standard tuning pitches for the four strings of a viola (low to high)?",
            questionEs = "¿Cuáles son los tonos de afinación estándar para las cuatro cuerdas de una viola (de grave a agudo)?",
            options = listOf(
                "C3 (130.81Hz), G3 (196Hz), D4 (293.66Hz), A4 (440Hz)",
                "G3 (196Hz), D4 (293.66Hz), A4 (440Hz), E5 (659.25Hz)",
                "C2 (65.41Hz), G2 (98Hz), D3 (146.83Hz), A3 (220Hz)",
                "E1 (41.20Hz), A1 (55Hz), D2 (73.42Hz), G2 (98Hz)"
            ),
            optionsEs = listOf(
                "Do3 (130.81Hz), Sol3 (196Hz), Re4 (293.66Hz), La4 (440Hz)",
                "Sol3 (196Hz), Re4 (293.66Hz), La4 (440Hz), Mi5 (659.25Hz)",
                "Do2 (65.41Hz), Sol2 (98Hz), Re3 (146.83Hz), La3 (220Hz)",
                "Mi1 (41.20Hz), La1 (55Hz), Re2 (73.42Hz), Sol2 (98Hz)"
            ),
            correctAnswerIndex = 0,
            explanation = "The viola is tuned a perfect fifth below the violin: C3 (130.81Hz), G3 (196Hz), D4 (293.66Hz), and A4 (440Hz). It shares the A, D, and G strings with the violin.",
            explanationEs = "La viola se afina una quinta perfecta por debajo del violín: Do3 (130.81Hz), Sol3 (196Hz), Re4 (293.66Hz) y La4 (440Hz). Comparte las cuerdas La, Re y Sol con el violín."
        ),
        QuizQuestion(
            id = 2,
            question = "In 'First Position' on the C string of a viola, what note is played with the 4th Finger?",
            questionEs = "En la 'Primera Posición' de la cuerda Do de una viola, ¿qué nota se toca con el 4° Dedo?",
            options = listOf(
                "G3 (196Hz)",
                "F3 (174.61Hz)",
                "A3 (220Hz)",
                "D3 (146.83Hz)"
            ),
            optionsEs = listOf(
                "Sol3 (196Hz)",
                "Fa3 (174.61Hz)",
                "La3 (220Hz)",
                "Re3 (146.83Hz)"
            ),
            correctAnswerIndex = 0,
            explanation = "The 4th finger on the C string plays G3 (196Hz)—a perfect fifth above the open C, matching the open G string in unison.",
            explanationEs = "El 4° dedo en la cuerda Do toca Sol3 (196Hz)—una quinta perfecta sobre el Do al aire, coincidiendo al unísono con la cuerda Sol."
        ),
        QuizQuestion(
            id = 3,
            question = "What is the term for the expressive, pulsating pitch variation produced by oscillating the left hand fingers?",
            questionEs = "¿Cuál es el término para la variación expresiva y pulsante de tono producida al oscilar los dedos de la mano izquierda?",
            options = listOf("Tremolo", "Pizzicato", "Vibrato", "Glissando"),
            optionsEs = listOf("Trémolo", "Pizzicato", "Vibrato", "Glissando"),
            correctAnswerIndex = 2,
            explanation = "Vibrato is the subtle pitch oscillation used to warm and beautify string instrument tone. It requires isolated joint flexibility.",
            explanationEs = "El vibrato es la sutil oscilación del tono utilizada para dar calidez y embellecer el sonido de los instrumentos de cuerda. Requiere flexibilidad en las articulaciones."
        ),
        QuizQuestion(
            id = 4,
            question = "How do string players check an octave on the fingerboard for pitch accuracy (intonation)?",
            questionEs = "¿Cómo comprueban los intérpretes de cuerda una octava en el diapasón para verificar la entonación?",
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
            explanation = "Checking a third-finger note against the lower adjacent open string creates a pure octave acoustic resonance for accurate intonation.",
            explanationEs = "Al comparar una nota del tercer dedo con la cuerda al aire adyacente inferior se genera una octava de resonancia acústica pura para verificar la entonación."
        ),
        QuizQuestion(
            id = 5,
            question = "Which bowing style involves crisp, forearm releases where the bow stick pinches then launches to cause a hammering sound?",
            questionEs = "¿Qué estilo de arco implica golpes nítidos de antebrazo donde la vara del arco presiona y luego se libera para causar un sonido de martilleo?",
            options = listOf("Spiccato", "Détaché", "Martelé", "Col Legno"),
            optionsEs = listOf("Spiccato", "Détaché", "Martelé", "Col Legno"),
            correctAnswerIndex = 2,
            explanation = "Martelé (meaning 'hammered') is an accented bow stroke where the player pinches then instantly releases pressure to form a sharp launch.",
            explanationEs = "Martelé (que significa 'martillado') es un golpe de arco acentuado de antebrazo donde el intérprete presiona y luego libera instantáneamente para formar un ataque agudo."
        )
    ),
    Instrument.CELLO to listOf(
        QuizQuestion(
            id = 1,
            question = "What are the standard tuning pitches for the four strings of a cello (low to high)?",
            questionEs = "¿Cuáles son los tonos de afinación estándar para las cuatro cuerdas de un violonchelo (de grave a agudo)?",
            options = listOf(
                "C2 (65.41Hz), G2 (98Hz), D3 (146.83Hz), A3 (220Hz)",
                "G2 (98Hz), D3 (146.83Hz), A3 (220Hz), E4 (329.63Hz)",
                "C3 (130.81Hz), G3 (196Hz), D4 (293.66Hz), A4 (440Hz)",
                "E1 (41.20Hz), A1 (55Hz), D2 (73.42Hz), G2 (98Hz)"
            ),
            optionsEs = listOf(
                "Do2 (65.41Hz), Sol2 (98Hz), Re3 (146.83Hz), La3 (220Hz)",
                "Sol2 (98Hz), Re3 (146.83Hz), La3 (220Hz), Mi4 (329.63Hz)",
                "Do3 (130.81Hz), Sol3 (196Hz), Re4 (293.66Hz), La4 (440Hz)",
                "Mi1 (41.20Hz), La1 (55Hz), Re2 (73.42Hz), Sol2 (98Hz)"
            ),
            correctAnswerIndex = 0,
            explanation = "The cello is tuned an octave below the viola: C2 (65.41Hz), G2 (98Hz), D3 (146.83Hz), and A3 (220Hz)—all in perfect fifths.",
            explanationEs = "El violonchelo se afina una octava por debajo de la viola: Do2 (65.41Hz), Sol2 (98Hz), Re3 (146.83Hz) y La3 (220Hz)—todas en quintas perfectas."
        ),
        QuizQuestion(
            id = 2,
            question = "In 'First Position' on the C string of a cello, what note is played with a High 2nd Finger?",
            questionEs = "En la 'Primera Posición' de la cuerda Do de un violonchelo, ¿qué nota se toca con el 2° Dedo Alto?",
            options = listOf(
                "E♭2 (77.78Hz)",
                "E2 (82.41Hz)",
                "D2 (73.42Hz)",
                "F2 (87.31Hz)"
            ),
            optionsEs = listOf(
                "Mi♭2 (77.78Hz)",
                "Mi2 (82.41Hz)",
                "Re2 (73.42Hz)",
                "Fa2 (87.31Hz)"
            ),
            correctAnswerIndex = 1,
            explanation = "A High 2nd finger on the C string raises the pitch by a whole step from D (1st finger) to E2 (82.41Hz)—the major third in C major.",
            explanationEs = "Un 2° dedo alto en la cuerda Do eleva el tono un tono entero desde Re (1° dedo) a Mi2 (82.41Hz)—la tercera mayor en Do mayor."
        ),
        QuizQuestion(
            id = 3,
            question = "What is the term for the expressive, pulsating pitch variation produced by oscillating the left hand fingers?",
            questionEs = "¿Cuál es el término para la variación expresiva y pulsante de tono producida al oscilar los dedos de la mano izquierda?",
            options = listOf("Tremolo", "Pizzicato", "Vibrato", "Glissando"),
            optionsEs = listOf("Trémolo", "Pizzicato", "Vibrato", "Glissando"),
            correctAnswerIndex = 2,
            explanation = "Vibrato is the subtle pitch oscillation used to warm and beautify string instrument tone. It requires isolated joint flexibility.",
            explanationEs = "El vibrato es la sutil oscilación del tono utilizada para dar calidez y embellecer el sonido de los instrumentos de cuerda. Requiere flexibilidad en las articulaciones."
        ),
        QuizQuestion(
            id = 4,
            question = "How do string players check an octave on the fingerboard for pitch accuracy (intonation)?",
            questionEs = "¿Cómo comprueban los intérpretes de cuerda una octava en el diapasón para verificar la entonación?",
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
            explanation = "Checking a third-finger note against the lower adjacent open string creates a pure octave acoustic resonance for accurate intonation.",
            explanationEs = "Al comparar una nota del tercer dedo con la cuerda al aire adyacente inferior se genera una octava de resonancia acústica pura para verificar la entonación."
        ),
        QuizQuestion(
            id = 5,
            question = "Which bowing style involves crisp, forearm releases where the bow stick pinches then launches to cause a hammering sound?",
            questionEs = "¿Qué estilo de arco implica golpes nítidos de antebrazo donde la vara del arco presiona y luego se libera para causar un sonido de martilleo?",
            options = listOf("Spiccato", "Détaché", "Martelé", "Col Legno"),
            optionsEs = listOf("Spiccato", "Détaché", "Martelé", "Col Legno"),
            correctAnswerIndex = 2,
            explanation = "Martelé (meaning 'hammered') is an accented bow stroke where the player pinches then instantly releases pressure to form a sharp launch.",
            explanationEs = "Martelé (que significa 'martillado') es un golpe de arco acentuado de antebrazo donde el intérprete presiona y luego libera instantáneamente para formar un ataque agudo."
        )
    ),
    Instrument.DOUBLE_BASS to listOf(
        QuizQuestion(
            id = 1,
            question = "What are the standard tuning pitches for the four strings of a double bass (low to high)?",
            questionEs = "¿Cuáles son los tonos de afinación estándar para las cuatro cuerdas de un contrabajo (de grave a agudo)?",
            options = listOf(
                "E1 (41.20Hz), A1 (55Hz), D2 (73.42Hz), G2 (98Hz)",
                "C2 (65.41Hz), G2 (98Hz), D3 (146.83Hz), A3 (220Hz)",
                "B0 (30.87Hz), E1 (41.20Hz), A1 (55Hz), D2 (73.42Hz)",
                "E1 (41.20Hz), A1 (55Hz), D2 (73.42Hz), C2 (65.41Hz)"
            ),
            optionsEs = listOf(
                "Mi1 (41.20Hz), La1 (55Hz), Re2 (73.42Hz), Sol2 (98Hz)",
                "Do2 (65.41Hz), Sol2 (98Hz), Re3 (146.83Hz), La3 (220Hz)",
                "Si0 (30.87Hz), Mi1 (41.20Hz), La1 (55Hz), Re2 (73.42Hz)",
                "Mi1 (41.20Hz), La1 (55Hz), Re2 (73.42Hz), Do2 (65.41Hz)"
            ),
            correctAnswerIndex = 0,
            explanation = "The double bass is tuned in fourths (unlike other string instruments): E1 (41.20Hz), A1 (55Hz), D2 (73.42Hz), and G2 (98Hz).",
            explanationEs = "El contrabajo se afina en cuartas (a diferencia de otros instrumentos de cuerda): Mi1 (41.20Hz), La1 (55Hz), Re2 (73.42Hz) y Sol2 (98Hz)."
        ),
        QuizQuestion(
            id = 2,
            question = "In 'First Position' on the E string of a double bass, what note is played with the 3rd Finger?",
            questionEs = "En la 'Primera Posición' de la cuerda Mi de un contrabajo, ¿qué nota se toca con el 3° Dedo?",
            options = listOf(
                "G♯1 (51.91Hz)",
                "F♯1 (46.25Hz)",
                "A1 (55Hz)",
                "B1 (61.74Hz)"
            ),
            optionsEs = listOf(
                "Sol♯1 (51.91Hz)",
                "Fa♯1 (46.25Hz)",
                "La1 (55Hz)",
                "Si1 (61.74Hz)"
            ),
            correctAnswerIndex = 2,
            explanation = "The 3rd finger on the E string plays A1 (55Hz)—a perfect fourth above the open E, matching the open A string in unison.",
            explanationEs = "El 3° dedo en la cuerda Mi toca La1 (55Hz)—una cuarta perfecta sobre el Mi al aire, coincidiendo al unísono con la cuerda La."
        ),
        QuizQuestion(
            id = 3,
            question = "What is the term for the expressive, pulsating pitch variation produced by oscillating the left hand fingers?",
            questionEs = "¿Cuál es el término para la variación expresiva y pulsante de tono producida al oscilar los dedos de la mano izquierda?",
            options = listOf("Tremolo", "Pizzicato", "Vibrato", "Glissando"),
            optionsEs = listOf("Trémolo", "Pizzicato", "Vibrato", "Glissando"),
            correctAnswerIndex = 2,
            explanation = "Vibrato is the subtle pitch oscillation used to warm and beautify string instrument tone. It requires isolated joint flexibility.",
            explanationEs = "El vibrato es la sutil oscilación del tono utilizada para dar calidez y embellecer el sonido de los instrumentos de cuerda. Requiere flexibilidad en las articulaciones."
        ),
        QuizQuestion(
            id = 4,
            question = "How do string players check an octave on the fingerboard for pitch accuracy (intonation)?",
            questionEs = "¿Cómo comprueban los intérpretes de cuerda una octava en el diapasón para verificar la entonación?",
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
            explanation = "Checking a third-finger note against the lower adjacent open string creates a pure octave acoustic resonance for accurate intonation.",
            explanationEs = "Al comparar una nota del tercer dedo con la cuerda al aire adyacente inferior se genera una octava de resonancia acústica pura para verificar la entonación."
        ),
        QuizQuestion(
            id = 5,
            question = "Which bowing style involves crisp, forearm releases where the bow stick pinches then launches to cause a hammering sound?",
            questionEs = "¿Qué estilo de arco implica golpes nítidos de antebrazo donde la vara del arco presiona y luego se libera para causar un sonido de martilleo?",
            options = listOf("Spiccato", "Détaché", "Martelé", "Col Legno"),
            optionsEs = listOf("Spiccato", "Détaché", "Martelé", "Col Legno"),
            correctAnswerIndex = 2,
            explanation = "Martelé (meaning 'hammered') is an accented bow stroke where the player pinches then instantly releases pressure to form a sharp launch.",
            explanationEs = "Martelé (que significa 'martillado') es un golpe de arco acentuado de antebrazo donde el intérprete presiona y luego libera instantáneamente para formar un ataque agudo."
        )
    )
)
