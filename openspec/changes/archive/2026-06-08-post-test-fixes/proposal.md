# Proposal: Post-Test Fixes

## Intent

Fix 5 issues found during APK testing: simulated video player blocking all playback, missing fingering/quiz content for non-violin instruments, broken teacher/student media upload pipeline, disconnected quiz-to-advancement flow, and home screen UI defects. Core features unreachable in current build.

## Scope

### In Scope
- Real video player (ExoPlayer/Media3) replacing simulated soundwave bars and fake decrypt logs
- Fingering maps + quiz banks for viola, cello, bass
- Teacher recording/upload pipeline: DI wiring, real recording UI, Firebase upload
- Student recording/upload with face blur verification
- Quiz-gated level advancement (score≥80), read-only skill chip, "Take Quiz" button
- Passcode wall optionalization (free-tier access to masterclass)

### Out of Scope
- Streaming URL support (local/embedded MP4s only)
- Audio-only recording (video first; deferred)
- Double bass fingerboard content (enum exists; content deferred)

## Capabilities

### New Capabilities
- `video-playback`: ExoPlayer/Media3 replacing simulated SecureMediaPlaybackConsole. Real streaming for masterclass and assignment playback.
- `video-upload`: Teacher/student recording pipeline: record → face-blur → H.264 compress → Firebase upload → real URL. DI wiring via Hilt.
- `quiz-advancement`: Quiz-gated level advancement. Score≥80 required. Skill chip becomes read-only. "Take Quiz" navigation button on home screen.
- `multi-instrument-content`: Fingering maps + quiz banks for viola (C3/G3/D4/A4), cello (C2/G2/D3/A3), bass (E1/A1/D2/G2).

### Modified Capabilities
- `app`: Passcode wall becomes optional. AssignmentViewModel and PracticeViewModel receive new dependencies for video upload and quiz-gated advancement.

## Approach

Four delivery slices respecting dependencies:

1. **Video infrastructure** (Issues 1→3→4): Add ExoPlayer dep → replace simulated player → wire VideoUploadViewModel via Hilt → replace checkbox with recording launcher → verify student blur pipeline.
2. **Multi-instrument content** (Issue 2, independent): Add fingering maps + quiz banks per instrument. Wire `instrument` param into TheoryQuizTab.
3. **Quiz-advancement gating** (Issue 5, independent): Remove clickable level cycling → add quiz gate in UpdateSkillLevelUseCase → add "Take Quiz" button on home screen.
4. **Integration**: Cross-test all flows end-to-end.

## Affected Areas

| Area | Impact | Description |
|------|--------|-------------|
| `ui/component/VideoPlayer.kt` | Replaced | Simulated → ExoPlayer composable |
| `ui/screens/MasterclassTab.kt` | Modified | Optional passcode wall |
| `ui/component/VirtualFingerboard.kt` | Modified | Multi-instrument fingering maps |
| `ui/component/LessonData.kt` | Modified | Instrument-specific quiz banks |
| `ui/screens/LessonsScreen.kt` | Modified | videoViewModel wiring + instrument param |
| `ui/component/AssignmentCreationForm.kt` | Modified | Checkbox → recording launcher |
| `domain/usecase/PublishAssignmentUseCase.kt` | Modified | Accept real URLs |
| `domain/usecase/UpdateSkillLevelUseCase.kt` | Modified | Quiz completion gate |
| `ui/component/ProfilePointsCard.kt` | Modified | maxLines=1, remove clickable cycle |
| `ui/viewmodel/VideoUploadViewModel.kt` | Wired | DI binding + call chain pass |
| `di/` | Modified | Hilt module entry |

## Risks

| Risk | Likelihood | Mitigation |
|------|------------|------------|
| ExoPlayer inflates APK size (~15MB) | Med | Use Media3 modular deps (exoplayer-core + ui only) |
| Face blur fails on devices lacking Play Services | Low | Existing fallback returns original video + warning |
| Incorrect fingering data for viola/cello/bass | Med | Cross-reference standard charts; unit test against known frequencies |
| Quiz advancement regression for existing users | Low | Existing users retain level; gate only on advancement attempt |

## Rollback Plan

- Video: revert to `SecureMediaPlaybackConsole`, remove media3 deps
- DI: revert `LessonsScreen` param pass → VideoUploadViewModel null → buttons hidden (current state)
- Content: revert to violin-only maps, remove instrument param from TheoryQuizTab
- Quiz gate: revert `UpdateSkillLevelUseCase` to unconditional save, restore clickable chip

## Dependencies

- `androidx.media3:media3-exoplayer` + `media3-ui` Gradle dependencies
- Issues 3 & 4 blocked on Issue 1 (real player needed for assignment playback)
- Issues 2 & 5 independent — shippable in any order

## Success Criteria

- [ ] Masterclass/Lesson videos play real MP4 content; passcode wall skippable for free tier
- [ ] Viola/cello fingering renders correct note positions for all strings
- [ ] Teacher records → uploads → student sees real playback (not simulated decrypt UI)
- [ ] Student records with face blur → Firebase receives blurred upload
- [ ] "Principiante" fits single line; clicking skill chip does nothing
- [ ] Quiz score≥80 advances level; score<80 does not
- [ ] `./gradlew :app:assembleDebug` succeeds with zero compilation errors
