# Quiz Advancement Specification

## Purpose

Quiz-gated skill level advancement replacing the free-clickable skill chip cycler. Quiz completion with score ≥80 becomes the mandatory gate for advancing skill level. The skill chip on the home screen becomes read-only and single-line.

## Requirements

### Requirement: Read-Only Single-Line Skill Chip

The skill level chip on ProfilePointsCard MUST be non-clickable (read-only). The skill level text (e.g., "Principiante") SHALL fit a single line (maxLines=1) without wrapping. The onCycleSkillLevel callback and its level-cycling logic SHALL be removed.

#### Scenario: Click does nothing

- GIVEN ProfilePointsCard renders with skill level "Principiante"
- WHEN user taps the skill chip
- THEN the skill level does not change

#### Scenario: Single-line text

- GIVEN the app language is Spanish and skill level is "Principiante"
- WHEN ProfilePointsCard renders on a standard-width device
- THEN the text fits on one line without wrapping to a second

### Requirement: Quiz-Gated Level Advancement

UpdateSkillLevelUseCase MUST only persist a new skill level when the TheoryQuizTab reports a final score of ≥80 points (out of 100). The current unconditional level save SHALL be replaced with this gated behavior. Scores below 80 SHALL NOT advance the level. Existing users retain their current level; the gate applies only on advancement attempts.

#### Scenario: Score ≥80 — level advances

- GIVEN user completes TheoryQuizTab with score 80
- WHEN the quiz summary triggers level advancement
- THEN the skill level is updated and persisted

#### Scenario: Score <80 — no advancement

- GIVEN user completes TheoryQuizTab with score 60
- WHEN the quiz summary triggers level advancement
- THEN the skill level remains unchanged

#### Scenario: Existing users retain level

- GIVEN user has current skill level "Intermediate"
- WHEN the app is updated to quiz-gated advancement
- THEN the existing level is preserved without requiring a re-quiz

### Requirement: Home Screen Quiz Navigation

HomeScreen MUST display a "Take Quiz" button that navigates the user to the TheoryQuizTab on LessonsScreen. The button SHALL be visible regardless of the user's current skill level.

#### Scenario: Tap "Take Quiz" navigates to quiz

- GIVEN user is on HomeScreen
- WHEN user taps the "Take Quiz" button
- THEN the app navigates to LessonsScreen with TheoryQuizTab selected
