# Delta for App

## ADDED Requirements

### Requirement: Optional Passcode Wall

The mandatory passcode authentication on MasterclassTab SHALL become optional. Users without a configured passcode SHALL access masterclass content directly. Users with a configured passcode SHALL continue to require PIN authentication. The `!isSecurityLocked` state in AuthViewModel MUST no longer force the passcode setup screen; it SHALL allow direct access to the content hub.

#### Scenario: No passcode — direct access

- GIVEN no passcode is set in the system
- WHEN user navigates to MasterclassTab
- THEN the masterclass content hub renders immediately without lock screen

#### Scenario: Passcode set — authentication required

- GIVEN a passcode is configured
- WHEN user navigates to MasterclassTab
- THEN the passcode lock screen appears and requires correct PIN

### Requirement: VideoUploadViewModel DI Wiring

The Hilt dependency injection graph MUST provide VideoUploadViewModel and its service dependencies (VideoRecordingService, FaceBlurProcessor, VideoCompressionService, VideoUploadService). TeacherDashboardTab and StudentAssignmentsTab SHALL receive VideoUploadViewModel as a non-null constructor/parameter, replacing the current nullable default (`null`).

#### Scenario: Hilt provides VideoUploadViewModel

- GIVEN the Hilt component graph is initialized
- WHEN TeacherDashboardTab is composed
- THEN VideoUploadViewModel is injected and non-null

#### Scenario: Recording trigger replaces checkbox

- GIVEN teacher is viewing AssignmentCreationForm
- WHEN the video attachment section renders
- THEN a recording trigger button is present instead of the checkbox-only attachVideo toggle

## MODIFIED Requirements

### Requirement: PracticeViewModel

A `PracticeViewModel` MUST handle practice timer (start/pause/resume/stop-save/cancel), daily tasks (load/complete/score), session persistence, points earning, daily goal tracking, and quiz-gated skill level advancement. The `updateSkillLevel` method SHALL accept a quiz score parameter and only persist advancement when score ≥ 80.
(Previously: `updateSkillLevel` was unconditional — any call immediately persisted the new level with no quiz gate.)

#### Scenario: User starts a practice session

- GIVEN the user starts a practice session with category "Smart Tuner"
- WHEN `PracticeViewModel.startPracticeTimer("Smart Tuner")` is called
- THEN `isPracticing` becomes true AND `practiceElapsedSeconds` increments every second

#### Scenario: Session saved after 30 seconds

- GIVEN a practice session has elapsed 30 seconds
- WHEN `PracticeViewModel.stopAndSavePracticeSession()` is called
- THEN a PracticeSession is persisted to Room with durationSeconds=30 AND `isPracticing` becomes false

#### Scenario: Daily task completed on first attempt

- GIVEN a daily task is completed on first attempt
- WHEN `PracticeViewModel.completeDailyTask("task_1", 1)` is called
- THEN the user earns 100 points AND the task is marked completed in SharedPreferences for today

#### Scenario: Quiz score ≥80 advances level

- GIVEN user completed TheoryQuizTab with score 85
- WHEN quiz summary triggers skill advancement
- THEN `updateSkillLevel("Intermediate", quizScore=85)` persists the new level

#### Scenario: Quiz score <80 does not advance

- GIVEN user completed TheoryQuizTab with score 60
- WHEN quiz summary triggers skill advancement
- THEN the skill level remains unchanged

**Acceptance**: `HomeScreen.kt` practice timer controls, daily task UI, and quiz advancement bind exclusively to `PracticeViewModel`.

### Requirement: AssignmentViewModel

An `AssignmentViewModel` MUST handle assignment CRUD (teacher publish, student view, mark complete, delete) and teacher-student linking via invite code. The `publishAssignment` method SHALL accept an optional real video URL from the upload pipeline, replacing the fake URL generation via VideoSecurityService.
(Previously: `PublishAssignmentUseCase` generated fake signed URLs via `VideoSecurityService.obtainSecureSignedUrl()` — no real upload capability.)

#### Scenario: Teacher publishes assignment

- GIVEN a teacher is logged in
- WHEN `AssignmentViewModel.publishAssignment(title, desc, student, video, duration)` is called
- THEN an Assignment entity is inserted into Room with the teacher's code

#### Scenario: Student assignments react to teacher code

- GIVEN a student is logged in with a linked teacher code
- WHEN assignments flow emits items
- THEN `studentAssignments` state updates reactively with filtered assignments for that student

#### Scenario: Mark assignment complete

- GIVEN an assignment exists
- WHEN `AssignmentViewModel.markAssignmentComplete(id, true)` is called
- THEN the assignment's `completed` field is updated AND 200 points are earned

#### Scenario: Publish with real video URL

- GIVEN teacher has recorded and uploaded a video (real Firebase URL)
- WHEN `publishAssignment` is called with the URL
- THEN the Assignment stores the real URL, not a generated fake signed URL

**Acceptance**: `TeacherStudentWorkspace.kt` binds exclusively to `AssignmentViewModel` for assignment state.
