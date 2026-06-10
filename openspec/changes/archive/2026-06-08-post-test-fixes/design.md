# Design: Post-Test Fixes

## Technical Approach

Five independent issues fixed via four parallelizable work units. Video infrastructure (Media3) unblocks assignment playback and upload. Multi-instrument content expands fingering + quiz banks. Quiz-gated advancement locks skill level behind score≥80. DI wiring connects already-`@HiltViewModel`-annotated `VideoUploadViewModel` to the composition tree via existing top-level `hiltViewModel()` pattern in `MainLayout`.

## Architecture Decisions

| Decision | Choice | Rejected | Rationale |
|----------|--------|----------|-----------|
| Video player | Media3 `exoplayer-core` + `exoplayer-ui` | Keep simulated, raw VideoView | Current AndroidX standard; `PlayerView` composable supports MP4; modular artifacts minimize APK (~8MB) |
| Passcode wall | Remove mandatory gate; `MasterclassTab` shows videos directly | Keep mandatory wall | Free-tier users need masterclass access per proposal scope |
| VideoUploadViewModel DI | Wire at `MainLayout` via `hiltViewModel()`, pass down as parameter | Create in `TeacherDashboardTab` locally, or in `LessonsScreen` | Follows existing pattern: all VMs created at top level; student tab also needs it |
| Fingering data | Expand inline `fingeringMap` in `VirtualFingerboard.kt` | Separate data classes, JSON resources | Follows existing pattern; static data, no IO cost; existing filter by `instrument.strings` already works |
| Quiz question banks | Change `quizQuestions: List` → `quizQuestionsByInstrument: Map<Instrument, List<QuizQuestion>>` | Per-instrument JSON files | Follows existing inline pattern; `TheoryQuizTab` already runs client-side; simple lookup by instrument key |
| Quiz gating | `onQuizCompleted(score, instrument)` callback → `UpdateSkillLevelUseCase` only if score≥80 | Server-side Firestore gating | Room is source of truth; offline-friendly; immediate feedback |
| Skill chip | `maxLines=1`, remove `clickable` + `onCycleSkillLevel` callback; show read-only badge | Keep clickable with quiz redirect | Clean separation: chip shows level; "Take Quiz" button handles advancement |
| Recording trigger | Replace checkbox `attachVideo` with "Record" button → `VideoRecordScreen` fullscreen | Audio-only, simplified camera | `VideoRecordScreen` already exists; full pipeline (record→blur→compress→upload) already implemented in `VideoUploadViewModel` |

## Data Flow

### Video Upload (Teacher)

```
AssignmentCreationForm
  "Record Video" button → onStartRecording()
       │
       ▼
TeacherDashboardTab (isRecordingVideo=true)
       │
       ▼
VideoRecordScreen ← VideoUploadViewModel (hiltViewModel in MainLayout)
       │  record → blur(minor) → compress → upload → Done(url)
       ▼
onVideoSent(url)
       │
       ▼
AssignmentCreationForm.publishAssignment(...) with real url ──→ PublishAssignmentUseCase(url)
       │                                                              │
       ▼                                                              ▼
AssignmentViewModel.publishAssignment(...)                    Firestore/Repository
```

### Quiz-Gated Advancement

```
HomeScreen ── "Take Quiz" button → navigationManager.selectTab(1) + setActiveSubIndex(2)
       │
       ▼
LessonsScreen tab=2 → TheoryQuizTab(instrument, practiceVM, onQuizCompleted)
       │  quiz score tracked (currentScoreVal), per-question correctAnswerIndex comparison
       ▼
Finish → if score ≥ 80: onQuizCompleted(score, instrument)
       │         │
       │         ▼
       │  practiceVM.updateSkillLevel(nextLevel) → UpdateSkillLevelUseCase(level="Intermediate")
       │
       └─ score < 80: no advancement, "Try Again" button
```

### Video Playback

```
LessonsScreen / MasterclassTab → LessonVideoPlayer(videoTitle, signedUrl) ──→ ExoPlayer composable
       │                                                                        │
       │  Replaces SecureMediaPlaybackConsole                                   │ plays local MP4 or
       │  (simulated soundwave bars, decrypt logs)                              │ Firebase signed URL
```

## File Changes

| File | Action | Description |
|------|--------|-------------|
| `ui/component/VideoPlayer.kt` | **Replace** | Simulated `SecureMediaPlaybackConsole` → ExoPlayer `@Composable` using `AndroidView` with `PlayerView` |
| `ui/component/LessonVideoPlayer.kt` | Modify | Replace `SecureMediaPlaybackConsole` call with real ExoPlayer composable |
| `ui/screens/MasterclassTab.kt` | Modify | Remove `SecureAuthenticationLockScreen` flow; show `UnlockedMasterclassHub` directly; ExoPlayer for playback |
| `ui/screens/LessonsScreen.kt` | Modify | Accept `videoViewModel: VideoUploadViewModel` param; pass to `TeacherDashboardTab` and `StudentAssignmentsTab`; accept `activeSubIndex` override for quiz nav |
| `ui/component/TeacherPanel.kt` | Modify | `videoViewModel` default `null` → required (non-null); wire "Record" button in `AssignmentCreationForm` |
| `ui/component/AssignmentCreationForm.kt` | Modify | Replace `attachVideo` checkbox with "Record Video" button; accept `onStartRecording: () -> Unit` callback; accept `videoUrl: String?` for real URL |
| `ui/component/StudentPanel.kt` | Modify | Remove `videoViewModel? = null` default; wire recording trigger in `StudentAssignmentItemCard` |
| `ui/component/VirtualFingerboard.kt` | Modify | Expand `fingeringMap` with viola C3/G3/D4/A4 and cello C2/G2/D3/A3 entries; remove placeholder text |
| `ui/component/LessonData.kt` | Modify | Change `quizQuestions: List` → `quizQuestionsByInstrument: Map<Instrument, List<QuizQuestion>>`; add viola + cello question banks (5 each) |
| `ui/component/TheoryQuizTab.kt` | Modify | Accept `instrument: Instrument` param; lookup `quizQuestionsByInstrument[instrument]`; fire `onQuizCompleted(score)` callback on finish |
| `ui/component/ProfilePointsCard.kt` | Modify | `maxLines=1` on skill text; remove `clickable` + `onCycleSkillLevel` param; show read-only badge |
| `ui/screens/HomeScreen.kt` | Modify | Add "Take Quiz" button in quick tools grid; remove `onCycleSkillLevel` callback; pass `navigationManager` for quiz nav |
| `domain/usecase/PublishAssignmentUseCase.kt` | Modify | Replace `VideoSecurityService.obtainSecureSignedUrl()` with real `videoUrl` parameter; remove fake URL generation |
| `domain/usecase/UpdateSkillLevelUseCase.kt` | Modify | Accept optional `quizScore: Int? = null`; require score≥80 for level advancement beyond current |
| `ui/viewmodel/AssignmentViewModel.kt` | Modify | `publishAssignment()` accepts `videoUrl: String?` parameter; passes to `PublishAssignmentUseCase` |
| `ui/viewmodel/PracticeViewModel.kt` | Modify | `updateSkillLevel()` accepts `quizScore: Int?` parameter; passes to `UpdateSkillLevelUseCase` |
| `ui/viewmodel/AuthViewModel.kt` | Modify | Apply passcode wall removal changes |
| `di/UseCaseModule.kt` | Modify | Update `PublishAssignmentUseCase` and `UpdateSkillLevelUseCase` provider signatures |
| `MainActivity.kt` | Modify | Add `hiltViewModel<VideoUploadViewModel>()`; pass to `LessonsScreen` |
| `build.gradle.kts` | Modify | Add `androidx.media3:media3-exoplayer` + `media3-ui` dependencies |
| `ui/component/SecureVideoGate.kt` | **Delete** | No longer needed after passcode wall removal |
| `security/VideoSecurityService.kt` | **Delete** | Fake URL signing replaced by real storage URLs |

## Interfaces / Contracts

```kotlin
// New: Quiz completion callback
typealias OnQuizCompleted = (score: Int, instrument: Instrument) -> Unit

// Modified: TheoryQuizTab signature
@Composable
fun TheoryQuizTab(
    practiceVM: PracticeViewModel,
    userPreferencesManager: UserPreferencesManager,
    instrument: Instrument = Instrument.VIOLIN,
    onQuizCompleted: OnQuizCompleted? = null
)

// Modified: PublishAssignmentUseCase — real URL parameter
suspend operator fun invoke(
    title: String, description: String, targetStudent: String,
    videoTitle: String, durationSeconds: Int,
    videoUrl: String? = null  // real Firebase URL, null = no video
): Assignment

// Modified: UpdateSkillLevelUseCase — quiz gating
suspend operator fun invoke(
    level: String,
    quizScore: Int? = null
)
// Guard: if currentLevel != "Advanced" && quizScore != null && quizScore < 80 → no-op
```

## DI Module Design

No new Hilt modules needed. All services (`VideoRecordingService`, `VideoCompressionService`, `VideoUploadService`, `FaceBlurProcessor`) are already `@Singleton @Inject`. `VideoUploadViewModel` is already `@HiltViewModel @Inject`. Only wiring missing is the `hiltViewModel()` call in `MainLayout`.

## Testing Strategy

| Layer | What to Test | Approach |
|-------|-------------|----------|
| Unit | ExoPlayer lifecycle (init, play, release) | JUnit + fake `ExoPlayer` |
| Unit | `UpdateSkillLevelUseCase` gating logic (score<80, score≥80, null score, already Advanced) | Parameterized JUnit |
| Unit | `PublishAssignmentUseCase` with real URL vs null | JUnit mock repo |
| Unit | `quizQuestionsByInstrument` completeness (all 3 instruments have ≥5 questions, correctAnswerIndex in bounds) | JUnit |
| Unit | `fingeringMap` correctness (viola/cello entries, frequencies match standard reference) | JUnit |
| Compose | `ProfilePointsCard` non-clickable, single-line text | Robolectric + Compose UI test |
| Compose | `TheoryQuizTab` instrument switching, quiz completion callback | Compose UI test |
| Compose | `VideoRecordScreen` state transitions (Idle→Recording→Done) | Compose UI test |

## Migration / Rollout

- No DB migration needed (Assignment entity already has `videoResourceUrl` field; Room schema unchanged)
- Existing users retain their skill level; quiz gate only applies on advancement attempts
- Passcode wall removal: existing passcodes become inert; no data loss
- Rollback: revert to `SecureMediaPlaybackConsole`, restore `VideoSecurityService`, remove media3 deps

## Open Questions

- [ ] Should bass fingering content be added in same unit or deferred? (proposal says deferred)
- [ ] Sample MP4 files for masterclass videos — bundle in APK or host on Firebase?
