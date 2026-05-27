# Proposal: Teacher-Student Chat & Video Sharing

## Intent

Add real-time chat and real video recording/upload to teacher-student assignments. Replace simulated video URLs with CameraX → MediaCodec → Firebase Storage pipeline. Enforce on-device face blur for minor students (isMinor=true).

## Scope

### In Scope
- Firestore real-time chat per assignment (teacher ↔ student, 1:1)
- CameraX video recording (3-min max, 720p)
- MediaCodec H.264 compression (~1–2 MB/min target)
- Firebase Storage upload with shareable URL
- ML Kit face detection + Gaussian blur for minor students (on-device, pre-upload)
- Integration with existing TeacherStudentWorkspace, AssignmentViewModel, SessionManager

### Out of Scope
- Live streaming / WebRTC
- Group chat, video playback in-app, cloud blur, video trimming

## Capabilities

### New Capabilities
- `chat-messaging`: Firestore real-time messaging within assignment context
- `video-recording-upload`: CameraX recording → MediaCodec compression → Firebase Storage upload
- `face-blur`: ML Kit on-device face detection + Gaussian blur, gated by `SessionManager.isCurrentUserMinor`

### Modified Capabilities
- None at spec level — assignment publish/view behavior preserved; only video URL source changes implementation

## Approach

**3 phases, each independently shippable:**

1. **Chat**: Firestore `messages/{assignmentId}/messages/{autoId}`. ChatRepository with Room cache. ChatViewModel + ChatScreen tabs in TeacherStudentWorkspace.
2. **Teacher Video**: CameraX → MediaCodec compression → Firebase Storage `videos/{teacher}/{assignmentId}/{timestamp}.mp4`. Integrates with existing Assignment video fields.
3. **Student Blur**: Reuses Phase 2 pipeline + FaceBlurProcessor (ML Kit every 5th frame, interpolated Gaussian). Gates on `isMinor`. Runs before compression.

**Dependencies**: Uncomment CameraX in `libs.versions.toml`. New: `firebase-firestore-ktx`, `firebase-storage-ktx`, `mlkit-face-detection` (all versioned by Firebase BOM 34.12.0).

## Affected Areas

| Area | Impact | Phase |
|------|--------|-------|
| `gradle/libs.versions.toml` | Modified | 1–3 |
| `app/build.gradle.kts` | Modified | 1–3 |
| `di/FirestoreModule.kt` | New | 1 |
| `di/StorageModule.kt` | New | 2 |
| `data/remote/ChatRepository.kt` | New | 1 |
| `data/remote/VideoUploadService.kt` | New | 2 |
| `service/VideoRecordingService.kt` | New | 2 |
| `service/VideoCompressionService.kt` | New | 2 |
| `service/FaceBlurProcessor.kt` | New | 3 |
| `ui/screens/ChatScreen.kt` | New | 1 |
| `ui/screens/VideoRecordScreen.kt` | New | 2 |
| `ui/viewmodel/ChatViewModel.kt` | New | 1 |
| `ui/viewmodel/VideoUploadViewModel.kt` | New | 2 |
| `ui/screens/TeacherStudentWorkspace.kt` | Modified | 1 |
| `ui/viewmodel/AssignmentViewModel.kt` | Modified | 2 |
| `proguard-rules.pro` | Modified | 2 |

## Risks

| Risk | Likelihood | Mitigation |
|------|------------|------------|
| Face blur: 5400 frames/3 min @ 30 fps | High | Sample every 5th frame (~1080), interpolate Gaussian between keyframes |
| Firestore read costs (real-time listeners per assignment) | Medium | Room cache; scope listener to single assignmentId |
| Storage costs (~15–30 MB per 3-min video) | Medium | 720p H.264 compression; delete videos on assignment deletion |
| ML Kit false negatives (no faces detected) | Low | Skip blur with warning log if 0 faces in first 30 s |
| CameraX OOM on low-end devices | Low | Surface encoder (MediaCodec) avoids buffer copies |
| ProGuard strips Firestore/ML Kit | Medium | Keep rules in `proguard-rules.pro` |

## Rollback Plan

Per-phase git revert. Fallback: hide chat/video tabs; restore `VideoSecurityService.obtainSecureSignedUrl()` in AssignmentViewModel for simulated URLs. Firebase rules left in place (no traffic).

## Dependencies

- Firebase Console: enable Firestore + Storage in `violin-app-795ee`
- SHA-1 already registered (Google Sign-In works)
- CameraX 1.5.0 already in version catalog (commented out)
- `SessionManager.isCurrentUserMinor` already exists
- Firebase BOM 34.12.0 already in dependencies

## Success Criteria

- [ ] Teacher and student exchange real-time messages within an assignment
- [ ] Teacher records 3-min video → compressed → uploaded → URL stored on Assignment
- [ ] Student recordings have faces blurred (if minor) before upload
- [ ] Video URL shared via chat message
- [ ] 119 existing tests pass (zero regression)
- [ ] ProGuard release build succeeds
