# Tasks: Teacher-Student Chat & Video Sharing

## Review Workload Forecast

| Field | Value |
|-------|-------|
| Estimated changed lines | ~1800–2200 |
| 400-line budget risk | High |
| Chained PRs recommended | Yes |
| Suggested split | 6 work units → 6 stacked PRs to main |
| Delivery strategy | auto-chain |
| Chain strategy | stacked-to-main |

Decision needed before apply: No
Chained PRs recommended: Yes
Chain strategy: stacked-to-main
400-line budget risk: High

### Suggested Work Units

| Unit | Goal | Likely PR | Est. Lines | Notes |
|------|------|-----------|------------|-------|
| 1 | Firestore + Room foundation | PR 1 (base: main) | ~350 | DB, migration, repo, tests |
| 2 | Chat UI + ViewModel | PR 2 (base: PR 1) | ~320 | ViewModel, screen, workspace, tests |
| 3 | Video recording + compression | PR 3 (base: PR 2) | ~380 | CameraX, MediaCodec, temp files |
| 4 | Video upload + UI | PR 4 (base: PR 3) | ~330 | Storage, VM, screen, wire to chat |
| 5 | Face blur processor | PR 5 (base: PR 4) | ~320 | ML Kit, Gaussian blur, pipeline gate |
| 6 | Integration + localization | PR 6 (base: PR 5) | ~250 | Integration tests, 18+ string keys, ProGuard verify |

## Phase 1: Chat Foundation

- [x] T-001: Create `di/FirebaseModule.kt` — `@Module @InstallIn(SingletonComponent)` with `@Provides @Singleton` for `FirebaseFirestore` (persistence enabled) and `FirebaseStorage`. Files: `di/FirebaseModule.kt` (new) + `libs.versions.toml` (uncomment CameraX). Deps: none. AC: REQ-CC-001. Test: Hilt `@SingletonComponent` unit. ~35 lines.
- [x] T-002: Create `data/Message.kt` — data class with fields: `id`, `assignmentId`, `senderUsername`, `senderRole`, `text`, `attachmentUrl`, `attachmentType`, `timestamp`, `read`. Firestore-compatible via `toMap()` extension. Files: `data/Message.kt` (new). Deps: none. AC: REQ-CHAT-001. Test: JSON serialization round-trip. ~25 lines.
- [x] T-003: Create `data/local/CachedMessage.kt` (Room `@Entity(tableName="cached_messages")`) + `CachedMessageDao` interface with `@Query` for `assignmentId` ordered by `timestamp ASC`. Files: `data/local/CachedMessage.kt` (new), `data/local/PracticeDao.kt` (modify). Deps: T-002. AC: REQ-CHAT-004, REQ-CC-002. Test: DAO query with in-memory Room. ~50 lines.
- [x] T-004: Add Room migration `MIGRATION_4_5` adding `cached_messages` table (id TEXT PK, assignmentId, senderUsername, senderRole, text, attachmentUrl, attachmentType, timestamp INTEGER, read INTEGER DEFAULT 0) + index. Update `PracticeDatabase.kt` to v5. Files: `data/PracticeDatabase.kt` (modify). Deps: T-003. AC: REQ-CC-002. Test: `MigrationTestHelper` Robolectric test. ~45 lines.
- [x] T-005: Create `data/remote/ChatRepository.kt` — `observeMessages(assignmentId)` as `Flow<List<Message>>` using Firestore snapshot listener + Room cache sync. `sendMessage()` writes to Firestore. `markAsRead()` batch-updates visible messages. Files: `data/remote/ChatRepository.kt` (new). Deps: T-001, T-003, T-004. AC: REQ-CHAT-001–004, REQ-CHAT-008. Test: mock Firestore + in-memory DAO. ~110 lines.
- [x] T-006: Create `ui/viewmodel/ChatViewModel.kt` — `@HiltViewModel` with `messages: StateFlow`, `sendMessage(text)`, `sendError`. Injects `ChatRepository` + `SessionManager`. Reads `currentUser` for sender identity. Files: `ui/viewmodel/ChatViewModel.kt` (new). Deps: T-005. AC: REQ-CHAT-005. Test: TDD — fake repo, verify state transitions. ~80 lines.
- [x] T-007: Create `ui/components/MessageBubble.kt` — composable rendering teacher (right-aligned, primary color) vs student (left-aligned, surface) with sender label, text/attachment indicator, timestamp. Files: `ui/components/MessageBubble.kt` (new). Deps: T-002. AC: REQ-CHAT-006. Test: Compose preview + alignment assertions. ~55 lines.
- [x] T-008: Create `ui/screens/ChatScreen.kt` — `LazyColumn` of `MessageBubble`, `OutlinedTextField` + send `IconButton`, empty state text. Observes `ChatViewModel.messages`. Read receipt trigger on viewport entry. Files: `ui/screens/ChatScreen.kt` (new). Deps: T-006, T-007. AC: REQ-CHAT-006, REQ-CHAT-008. Test: Roborazzi screenshot (alignment, empty state). ~140 lines.
- [x] T-009: Modify `ui/screens/TeacherStudentWorkspace.kt` — add "Chat" `TextButton` per assignment card (teacher + student tabs). Navigate to `ChatScreen(assignmentId = assignment.id.toString())`. Files: `ui/screens/TeacherStudentWorkspace.kt` (modify). Deps: T-008. AC: REQ-CHAT-007. Test: Navigation test verifies ChatScreen opens. ~35 lines.
- [x] T-010: Create `ChatViewModelTest.kt` + `ChatRepositoryTest.kt` — verify: send appends to Firestore, flow emits on snapshot change, offline error state, read receipts skip own messages. Files: `ui/viewmodel/ChatViewModelTest.kt` (new), `data/remote/ChatRepositoryTest.kt` (new). Deps: T-006, T-005. AC: REQ-CHAT-003, REQ-CHAT-005. Test: self. ~140 lines.

## Phase 2: Teacher Video

- [x] T-011: Create `service/VideoRecordingService.kt` — wraps CameraX `ProcessCameraProvider`, `Preview`, `VideoCapture` with `setDurationLimitMillis(180000)`. Exposes `startRecording(File)`, `stopRecording(): File`. Files: `service/VideoRecordingService.kt` (new), `app/build.gradle.kts` (CameraX deps). Deps: T-001 (camera deps uncommented). AC: REQ-VID-001–003. Test: Instrumentation test with fake Surface. ~130 lines.
- [x] T-012: Create `service/VideoCompressionService.kt` — `MediaCodec` async H.264 encoder: 720p max, 1 Mbps, 30 fps. Validates output via `MediaMetadataRetriever`. Preserves original on error. Files: `service/VideoCompressionService.kt` (new). Deps: T-011. AC: REQ-VID-004. Test: synthetic 1s video → verify bitrate/resolution. ~150 lines.
- [x] T-013: Create `data/remote/VideoUploadService.kt` — Firebase Storage `putFile(uri)` at `videos/{teacher}/{assignmentId}/{ts}.mp4`. Emits `StateFlow<Float>` progress. Obtains download URL. Deletes temp file on success. Files: `data/remote/VideoUploadService.kt` (new). Deps: T-001, T-005. AC: REQ-VID-005–008. Test: mock `StorageReference`, verify progress emission. ~100 lines.
- [x] T-014: Create `ui/viewmodel/VideoUploadViewModel.kt` — state machine: IDLE→RECORDING→COMPRESSING→UPLOADING→DONE. Exposes `recordingState`, `compressionProgress`, `uploadProgress`, `error`. Injects all services + `SessionManager`. Files: `ui/viewmodel/VideoUploadViewModel.kt` (new). Deps: T-011, T-012, T-013. AC: REQ-VID-007. Test: TDD state transitions with fake services. ~120 lines.
- [x] T-015: Create `ui/components/VideoPreview.kt` + `ui/screens/VideoRecordScreen.kt` — CameraX `PreviewView`, record/stop `FloatingActionButton`, elapsed timer, cancel. Post-recording: thumbnail via `VideoPreview`, Upload/Discard buttons. Progress indicator during upload. CAMERA permission request. Files: `ui/components/VideoPreview.kt` (new), `ui/screens/VideoRecordScreen.kt` (new). Deps: T-014. AC: REQ-VID-009, REQ-CC-004. Test: Compose test for FAB state, timer format. ~175 lines.
- [x] T-016: Modify `ui/screens/TeacherStudentWorkspace.kt` — add "Record Video" button (teacher-only, `isCurrentUserTeacher`). Navigate to `VideoRecordScreen`. Wire upload success → `ChatRepository.sendMessage(attachmentUrl, "video")`. Files: `ui/screens/TeacherStudentWorkspace.kt` (modify). Deps: T-014, T-009. AC: REQ-VID-007. Test: verify button visibility gated by role. ~40 lines.
- [x] T-017: Create `VideoUploadViewModelTest.kt` — verify state machine transitions, compression/upload progress emission, error states, file cleanup on success/failure. Files: `ui/viewmodel/VideoUploadViewModelTest.kt` (new). Deps: T-014. AC: REQ-VID-006, REQ-VID-008. Test: self. ~110 lines.

## Phase 3: Student Face Blur

- [x] T-018: Create `service/FaceBlurProcessor.kt` — ML Kit `FaceDetector` (FAST mode, no contours). `MediaExtractor` extracts every 5th frame; `MediaCodec` re-encodes with Gaussian blur on face boxes (adaptive kernel). Progress via `StateFlow<ProcessingProgress>`. Fallback: skip if 0 faces in first 30s + warning. Files: `service/FaceBlurProcessor.kt` (new), `app/build.gradle.kts` (ML Kit deps). Deps: T-012. AC: REQ-BLR-001–005, REQ-BLR-007–008. Test: Robolectric + ML Kit sample frames. ~190 lines.
- [x] T-019: Modify `VideoUploadViewModel.kt` — add blur pipeline gate: if `isCurrentUserMinor`, run `FaceBlurProcessor` before compression. Wire `blurProgress` state. Modify `VideoRecordScreen.kt` — show blur progress overlay for minors. Files: `ui/viewmodel/VideoUploadViewModel.kt` (modify), `ui/screens/VideoRecordScreen.kt` (modify). Deps: T-018. AC: REQ-BLR-002, REQ-BLR-006. Test: verify pipeline order via state transitions. ~60 lines.
- [x] T-020: Create `FaceBlurProcessorTest.kt` — verify: face detection on sample frames, Gaussian blur applied to boxes, 5th-frame sampling, interpolation, fallback on zero faces, progress emission. Files: `service/FaceBlurProcessorTest.kt` (new). Deps: T-018. AC: REQ-BLR-003–005, REQ-BLR-007. Test: self (Robolectric + sample images). ~130 lines.
- [x] T-021: Create integration test: record → blur (minor) → compress → upload → chat message. Verify: pipeline order, 2× duration budget, temp file cleanup. Add 18+ Localization keys (`chat_button`, `record_video`, `processing_frame`, `blur_warning`, etc.) to `Localization.kt` EN + ES maps. Verify `proguard-rules.pro` (lines 107-115). Files: `VideoPipelineIntegrationTest.kt` (new), `ui/theme/Localization.kt` (modify), `app/proguard-rules.pro` (verify). Deps: all. AC: REQ-BLR-008, REQ-CC-003, REQ-CC-005. Test: self. ~250 lines.
