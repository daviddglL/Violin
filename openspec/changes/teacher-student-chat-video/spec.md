# Delta Spec: Teacher-Student Chat & Video Sharing

## Purpose

Add real-time chat, video recording/upload, and face blur for minor students to Violin Master Android. Three new capabilities: `chat-messaging`, `video-recording-upload`, `face-blur`. No existing specs modified.

---

## SPEC-1: Chat Messaging (chat-messaging)

### REQ-CHAT-001 — Firestore Messages Collection

The system MUST store chat messages in a Firestore collection at path `assignments/{assignmentId}/messages/{autoId}`. Each document SHALL contain: `senderUsername` (String), `senderRole` (String, "TEACHER" | "STUDENT"), `text` (String), `attachmentUrl` (String, empty if none), `attachmentType` (String, empty | "video"), `timestamp` (server Timestamp), `read` (Boolean, default false).

| Scenario | Given | When | Then |
|----------|-------|------|------|
| Happy path | Teacher sends "Practice vibrato daily" in assignment `A1` | ChatRepository sends the message | Firestore document created at `assignments/A1/messages/{autoId}` with `senderRole="TEACHER"`, `serverTimestamp`, `read=false` |
| Video attachment | Teacher records a video and it uploads | Message sent with `attachmentUrl` and `attachmentType="video"` | Document contains `text=""`, `attachmentUrl` pointing to `firebasestorage.googleapis.com`, `attachmentType="video"` |

**Acceptance**: Firestore document structure validated in Firebase Console; fields match schema.

### REQ-CHAT-002 — Message Scoping per Assignment

Messages MUST be scoped to a single assignment. The collection path `assignments/{assignmentId}/messages` SHALL isolate messages per assignment; no cross-assignment queries.

| Scenario | Given | When | Then |
|----------|-------|------|------|
| Scoped listener | Assignment `A1` has 3 messages, `A2` has 5 messages | ChatScreen opens for `A1` | Only 3 messages from `assignments/A1/messages` are observed |
| New message in different assignment | ChatScreen is open for `A1` | A message is added to `A2` | ChatScreen for `A1` does NOT update |

**Acceptance**: ChatRepository query includes `whereEqualTo("assignmentId", ...)` or equivalent path scoping.

### REQ-CHAT-003 — ChatRepository Real-Time Listener

`ChatRepository` MUST expose `fun observeMessages(assignmentId: String): Flow<List<Message>>` using Firestore real-time snapshot listener. The Flow SHALL emit updated lists on any Firestore change (add/modify/remove). The listener MUST be removed on Flow cancellation.

| Scenario | Given | When | Then |
|----------|-------|------|------|
| Realtime emission | Room is empty; 2 messages exist in Firestore | Flow is collected | List with 2 messages emitted |
| New remote message | Flow is being collected | A new message arrives in Firestore | Updated list with +1 message emitted within 2 seconds |

**Acceptance**: Flow emits initial list + updates reactively. `removeOnCancelListener` called on scope cancellation.

### REQ-CHAT-004 — Room Cache for Offline Access

`CachedMessage` Room entity MUST mirror Firestore message fields (`assignmentId`, `senderUsername`, `senderRole`, `text`, `attachmentUrl`, `attachmentType`, `timestamp`, `read`). `ChatRepository` SHALL write Firestore snapshots to Room and serve from Room when offline. A `CachedMessageDao` SHALL provide `@Query("SELECT * FROM cached_messages WHERE assignmentId = :id ORDER BY timestamp ASC")`.

| Scenario | Given | When | Then |
|----------|-------|------|------|
| Online sync | 5 messages in Firestore for `A1` | Flow collected online | 5 messages persisted to `cached_messages` Room table |
| Offline fallback | Device offline; Room has cached messages | ChatScreen opens for `A1` | Cached messages displayed; Firestore listener fails silently |

**Acceptance**: Room query returns cached messages. Migration 4→5 adds `cached_messages` table.

### REQ-CHAT-005 — ChatViewModel Message Management

`ChatViewModel` MUST expose: `messages: StateFlow<List<Message>>`, `fun sendMessage(text: String)`, and observe messages via `ChatRepository.observeMessages(assignmentId)`. It SHALL read `currentUser` from `SessionManager` to populate `senderUsername` and `senderRole`.

| Scenario | Given | When | Then |
|----------|-------|------|------|
| Send text message | User "teacher_jane" sends "Great work!" | `sendMessage("Great work!")` called | Firestore document created with `senderUsername="teacher_jane"`, `senderRole="TEACHER"`, `text="Great work!"` |
| Send fails (offline) | Device is offline | `sendMessage(...)` called | Try/catch logs error; message NOT in Firestore; `sendError` StateFlow emits error state |

**Acceptance**: `ChatViewModel` is `@HiltViewModel` with `ChatRepository` and `SessionManager` injected.

### REQ-CHAT-006 — ChatScreen Composable

`ChatScreen` MUST render a `LazyColumn` of messages with sender distinction: teacher messages aligned right with primary color background, student messages aligned left with surface variant background. Each message bubble SHALL show: sender username (small), message text or attachment indicator, and timestamp. An `OutlinedTextField` + send button at the bottom.

| Scenario | Given | When | Then |
|----------|-------|------|------|
| Message list | ChatScreen receives 3 messages (2 teacher, 1 student) | Screen renders | Teacher bubbles right-aligned, student bubble left-aligned; 3 bubbles visible |
| Empty state | `messages` is empty | Screen renders | Centered text "No messages yet. Start the conversation!" |

**Acceptance**: Compose test verifies alignment, sender label, and empty state rendering.

### REQ-CHAT-007 — Chat Entry Point from Workspace

`TeacherStudentWorkspace` SHALL add a "Chat" `TextButton` or `IconButton` per assignment card. On click, it MUST navigate to `ChatScreen(assignmentId = assignment.id)`. The assignment ID from Room (`Int`) SHALL be converted to Firestore document ID format (String).

| Scenario | Given | When | Then |
|----------|-------|------|------|
| Teacher opens chat | TeacherDashboardTab shows assignment `A1` (id=5) | Teacher taps "Chat" button on `A1` card | ChatScreen opens with `assignmentId = "5"` |
| Student opens chat | StudentAssignmentsTab shows assignment from teacher | Student taps "Chat" button | ChatScreen opens scoped to that assignment |

**Acceptance**: Both teacher and student tabs have a Chat button. Navigation uses assignment `id` as Firestore document ID.

### REQ-CHAT-008 — Read Receipts

When the recipient views a message (message visible on screen), the `read` field in Firestore SHALL be set to `true`. The field MUST NOT be set by the sender for their own messages.

| Scenario | Given | When | Then |
|----------|-------|------|------|
| Recipient reads | Student opens ChatScreen; teacher's message `M1` has `read=false` | `M1` enters viewport | Firestore updates `M1.read = true` |
| Sender does not mark own | Teacher opens ChatScreen; teacher's own message `M2` has `read=true` | Screen renders | No write triggered for `M2` (sender skip) |

**Acceptance**: `read` only toggles for messages where `senderUsername != currentUser.username`. Batch write of visible message IDs.

---

## SPEC-2: Video Recording & Upload — Teacher (video-recording-upload)

### REQ-VID-001 — CameraX Recording with Duration Limit

The system MUST use CameraX `VideoCapture` use case with `setDurationLimitMillis(180000)` (3 minutes). Recording SHALL automatically stop at the limit. The output format MUST be H.264/MP4.

| Scenario | Given | When | Then |
|----------|-------|------|------|
| Normal recording | Teacher taps record, records for 90 seconds | Teacher taps stop | MP4 file saved to `cacheDir/recording_{timestamp}.mp4`; duration ≈90s |
| Duration limit reached | Teacher starts recording | Recording reaches 180 seconds | CameraX auto-stops; saved file duration ≤180s; "Time limit reached" toast shown |

**Acceptance**: File exists in `context.cacheDir`. Duration metadata matches recording length.

### REQ-VID-002 — VideoRecordingService

`VideoRecordingService` MUST wrap CameraX `ProcessCameraProvider`, `Preview`, and `VideoCapture` use cases. It SHALL expose `suspend fun startRecording(outputFile: File)`, `fun stopRecording(): File`, and provide a `PreviewView` for the composable.

| Scenario | Given | When | Then |
|----------|-------|------|------|
| Start recording | Camera permissions granted, preview active | `startRecording(cacheFile)` called | `VideoCapture.startRecording(VideoCapture.OutputFileOptions)` invoked |
| Stop recording | Recording in progress for 30s | `stopRecording()` called | `VideoCapture.stopRecording()` invoked; output File returned |

**Acceptance**: Service is `@Singleton` with `@Inject constructor(@ApplicationContext context: Context)`.

### REQ-VID-003 — Temporary File Storage

Recorded video MUST save to `context.cacheDir` as a temporary file named `recording_{System.currentTimeMillis()}.mp4`. This file SHALL be deleted after successful Firebase Storage upload (REQ-VID-008). On upload failure, the file SHOULD remain for retry.

| Scenario | Given | When | Then |
|----------|-------|------|------|
| File created | Recording completes | Stop button tapped | File at `cacheDir/recording_1717000000000.mp4` exists and is non-zero |
| File survives app background | Recording saved to cacheDir | App goes to background and returns | File still present (cacheDir not auto-cleared unless system low storage) |

**Acceptance**: `File(cacheDir, "recording_${ts}.mp4").exists()` returns true after recording.

### REQ-VID-004 — MediaCodec Compression

`VideoCompressionService` MUST compress the raw recording using `MediaCodec` (H.264 encoder) targeting: max 1 Mbps bitrate, 720p max resolution (1280×720), 30 fps. Metadata (duration, resolution, bitrate) SHALL be validated post-compression.

| Scenario | Given | When | Then |
|----------|-------|------|------|
| 1080p input | Raw recording is 1920×1080 @ 15 Mbps | Compressed | Output is 1280×720 @ ≤1 Mbps; file size reduced ≥60% |
| Already within limits | Raw recording is 1280×720 @ 800 Kbps | Compressed | Output is near-identical; no re-compression if already ≤1 Mbps |
| Compression error | MediaCodec fails to configure | Exception thrown | `compressionError` state emitted; original file preserved for fallback upload |

**Acceptance**: Output file passes `MediaMetadataRetriever` checks: `BITRATE ≤ 1_000_000`, `VIDEO_WIDTH ≤ 1280`.

### REQ-VID-005 — Firebase Storage Upload

`VideoUploadService` MUST upload the compressed MP4 to Firebase Storage at path `videos/{teacherUsername}/{assignmentId}/{timestamp}.mp4`. Upload SHALL use `FirebaseStorage.getInstance().reference.child(path).putFile(fileUri)`.

| Scenario | Given | When | Then |
|----------|-------|------|------|
| Successful upload | Compressed file ready; network available | `uploadVideo(file, teacherUsername, assignmentId)` called | File at `videos/teacher_jane/5/1717000000000.mp4` in Storage bucket `violin-app-795ee` |
| Network failure | Device offline during upload | Upload called | `StorageException` caught; `uploadError` state emitted; retry option shown |

**Acceptance**: File visible in Firebase Console Storage under correct path. Download URL retrievable.

### REQ-VID-006 — Upload Progress

`VideoUploadService` SHALL emit upload progress via `StateFlow<Float>` (0.0 to 1.0) using `addOnProgressListener`. The UI MUST show a `LinearProgressIndicator` bound to this flow.

| Scenario | Given | When | Then |
|----------|-------|------|------|
| 50% uploaded | 5 MB of 10 MB file uploaded | Progress listener fires | `uploadProgress` emits 0.5; ProgressBar shows 50% fill |
| Upload completes | Progress reaches 1.0 | Final progress event | `uploadProgress` emits 1.0; ProgressBar animates to 100% then hides |

**Acceptance**: `uploadProgress` state correctly reflects `bytesTransferred / bytesTotal`. UI composable test verifies ProgressBar visibility.

### REQ-VID-007 — Video URL Shared as Chat Message

After successful upload, `VideoUploadService` MUST obtain the download URL via `StorageReference.downloadUrl`. This URL SHALL be sent as a chat message via `ChatRepository.sendMessage(attachmentUrl = url, attachmentType = "video")`.

| Scenario | Given | When | Then |
|----------|-------|------|------|
| Upload → message | Video uploaded successfully; download URL obtained | `onUploadSuccess(url)` called | Chat message document created with `attachmentUrl=url`, `attachmentType="video"`, `text=""` |

**Acceptance**: After upload completes, message appears in ChatScreen for that assignment with video attachment indicator.

### REQ-VID-008 — Temporary File Cleanup

After successful upload AND chat message confirmation, the temporary file in `cacheDir` MUST be deleted via `File.delete()`. On upload failure, the file SHALL NOT be deleted (allows retry).

| Scenario | Given | When | Then |
|----------|-------|------|------|
| Success cleanup | Upload succeeds; chat message saved | Cleanup runs | `cacheDir/recording_*.mp4` deleted; `File.exists()` returns false |
| Failure preservation | Upload fails with network error | Error handler runs | Temporary file NOT deleted; user can retry |

**Acceptance**: No orphaned `.mp4` files in `cacheDir` after successful upload flow.

### REQ-VID-009 — VideoRecordScreen Composable

`VideoRecordScreen` MUST display: CameraX `PreviewView` (full width), a record/stop `FloatingActionButton`, elapsed timer text ("00:00" / "02:59"), a "Cancel" `TextButton`, and post-recording options (upload/discard). After stop, a thumbnail or preview frame SHALL be shown.

| Scenario | Given | When | Then |
|----------|-------|------|------|
| Recording started | Screen opens; preview active | User taps record FAB | FAB changes to stop icon (red); timer starts incrementing mm:ss |
| Recording stopped | Recording 45s in progress | User taps stop FAB | Preview frame shown; "Upload" and "Discard" buttons appear |

**Acceptance**: Compose test verifies PreviewView, FAB state toggle, timer format, and post-recording button visibility.

---

## SPEC-3: Face Blur for Minor Students (face-blur)

### REQ-BLR-001 — ML Kit On-Device Face Detection

`FaceBlurProcessor` MUST use `FaceDetection.getClient(FaceDetectorOptions.Builder().setPerformanceMode(FAST).setContourMode(NO_CONTOURS).build())`. Processing SHALL run entirely on-device (no cloud ML Kit calls). Input is `InputImage.fromFilePath(context, videoFile.toUri())`.

| Scenario | Given | When | Then |
|----------|-------|------|------|
| Faces detected | Raw video has a person facing camera | `processVideo(inputFile, outputFile)` called | Face bounding boxes detected; output video has blurred face regions |
| No API key needed | ML Kit face detection model downloaded | Detector created | No cloud API call; on-device model used |

**Acceptance**: `FaceDetector` client configured with `FAST` performance mode. No `FirebaseVisionFaceDetectorOptions` (cloud).

### REQ-BLR-002 — Blur Before Compression

Face blur processing MUST execute before video compression. The pipeline SHALL be: raw recording → FaceBlurProcessor (if minor) → VideoCompressionService → upload.

| Scenario | Given | When | Then |
|----------|-------|------|------|
| Minor user | `isCurrentUserMinor == true`; raw recording exists | Pipeline runs | Blur FIRST, THEN compression, THEN upload |
| Non-minor user | `isCurrentUserMinor == false` | Pipeline runs | Blur SKIPPED; raw → compression → upload |

**Acceptance**: Order verified via logs: "BlurPhase: starting" → "CompressionPhase: starting". Non-minor skip logged.

### REQ-BLR-003 — Frame Sampling (Every 5th Frame)

Face detection MUST sample every 5th frame (not every frame) for performance. Detected face bounding boxes SHALL be interpolated for intermediate frames (frames 1-4 use frame 0's boxes; frames 6-9 use frame 5's boxes).

| Scenario | Given | When | Then |
|----------|-------|------|------|
| 30 fps, 90s video | 2700 total frames | Face detection runs | ~540 frames processed (every 5th); 2160 frames interpolated |
| Face moves between keyframes | Frame 45 detects face at (100,200,300,400); Frame 50 at (120,220,320,420) | Interpolation for frames 46-49 | Bounding boxes linearly interpolated between keyframe positions |

**Acceptance**: Processing time ≤2× video duration on mid-range device (REQ-BLR-008).

### REQ-BLR-004 — Gaussian Blur on Face Bounding Boxes

A Gaussian blur MUST be applied to each detected face bounding box region per frame. The blur kernel size SHOULD be adaptive based on face box size: `kernelSize = max(faceBox.width / 10, 15)`. Pixels outside face regions SHALL remain unmodified.

| Scenario | Given | When | Then |
|----------|-------|------|------|
| Single face | Frame has face box (200×200) at x=100,y=100 | Blur applied | Gaussian blur with kernel ~20 applied to region [100..300, 100..300]; rest of frame sharp |
| Multiple faces | Frame has 2 faces | Both boxes blurred | Each face region blurred independently; non-face areas unmodified |

**Acceptance**: Visual inspection: faces illegible, non-face content (instrument, room) clear.

### REQ-BLR-005 — Progress Reporting

`FaceBlurProcessor` MUST emit progress as `StateFlow<ProcessingProgress>` with `currentFrame: Int` and `totalFrames: Int`. The UI SHALL display "Processing frame {current}/{total}" text during blur processing.

| Scenario | Given | When | Then |
|----------|-------|------|------|
| Mid-processing | 5400 total frames, processing at frame 2000 | Progress flow collected | `ProcessingProgress(currentFrame=2000, totalFrames=5400)` emitted; UI shows "Processing frame 2000/5400" |
| Completion | Last frame processed | Progress emits final | `currentFrame == totalFrames`; progress UI dismissed |

**Acceptance**: `VideoRecordScreen` observes `blurProgress` and renders a `LinearProgressIndicator` + text label.

### REQ-BLR-006 — Conditional Blur Pipeline

Face blur processing MUST run ONLY when `SessionManager.isCurrentUserMinor` returns `true`. For non-minor users (teachers, adult students), the blur pipeline step SHALL be entirely skipped — no ML Kit model loading, no frame processing.

| Scenario | Given | When | Then |
|----------|-------|------|------|
| Minor student | `isCurrentUserMinor == true` | Recording stops, upload pipeline starts | FaceBlurProcessor runs; ML Kit model loaded; frames processed |
| Adult student | `isCurrentUserMinor == false` | Recording stops, upload pipeline starts | Blur step skipped; compression starts immediately; "Blur skipped (adult user)" logged |

**Acceptance**: Zero ML Kit initialization calls when `isMinor == false`. Pipeline time <30% of minor pipeline.

### REQ-BLR-007 — Face Detection Failure Fallback

If `FaceBlurProcessor` fails to detect ANY face in the first 30 seconds of sampled keyframes (90 frames sampled at 5th-frame rate), the video MUST proceed to upload WITHOUT blur. A `Snackbar` warning SHALL display: "Face detection incomplete — video uploaded without blur."

| Scenario | Given | When | Then |
|----------|-------|------|------|
| No faces detected | Minor user, face not in frame (violin-only shot) | First 30s keyframes processed | 0 face detections; warning shown; video uploaded without blur |
| ML Kit error | Face detector throws exception | Error caught | Video uploaded without blur; error logged; "Face blur unavailable" warning |

**Acceptance**: Pipeline does NOT block on blur failure. Video uploadable regardless. Warning visible to user.

### REQ-BLR-008 — Processing Time Target

Blur processing time MUST complete in less than 2× the video duration on a mid-range device (Snapdragon 7-series equivalent). A 3-minute (180s) video SHALL complete blur processing in under 360 seconds (6 minutes).

| Scenario | Given | When | Then |
|----------|-------|------|------|
| 3-min video | 5400 frames, every 5th sampled = 1080 keyframes | FaceBlurProcessor runs on Dispatchers.Default | Total blur time ≤ 360s on mid-range device |
| 30s video | 900 frames, 180 keyframes | Processing runs | Total blur time ≤ 60s |

**Acceptance**: Integration test logs `faceBlurElapsedMs`. Assert `elapsedMs < (videoDurationSeconds * 2 * 1000)`.

---

## SPEC-4: Cross-Cutting Requirements

### REQ-CC-001 — FirebaseModule Hilt Module

A `FirebaseModule` Hilt module (`@Module @InstallIn(SingletonComponent::class)`) MUST provide `FirebaseFirestore` and `FirebaseStorage` instances as `@Singleton @Provides`. Firestore settings SHOULD enable `setPersistenceEnabled(true)` for offline support.

| Scenario | Given | When | Then |
|----------|-------|------|------|
| Inject Firestore | `ChatRepository` requests `FirebaseFirestore` | Hilt resolves | Singleton `FirebaseFirestore.getInstance()` returned |
| Inject Storage | `VideoUploadService` requests `FirebaseStorage` | Hilt resolves | Singleton instance returned; bucket = `violin-app-795ee.firebasestorage.app` |

**Acceptance**: `FirebaseModule.kt` in `di/` package. Both provides tested via Hilt test.

### REQ-CC-002 — Room Migration 4→5 (CachedMessage)

Room database version MUST increment from 4 to 5. A `Migration(4, 5)` SHALL add the `cached_messages` table with columns: `id` INTEGER PRIMARY KEY AUTOINCREMENT, `assignmentId` TEXT NOT NULL, `senderUsername` TEXT NOT NULL, `senderRole` TEXT NOT NULL, `text` TEXT NOT NULL, `attachmentUrl` TEXT NOT NULL, `attachmentType` TEXT NOT NULL, `timestamp` INTEGER NOT NULL, `read` INTEGER NOT NULL DEFAULT 0.

| Scenario | Given | When | Then |
|----------|-------|------|------|
| Migration runs | DB at version 4 with data in 4 existing tables | Migration 4→5 executes | `cached_messages` table created; existing tables and data preserved |
| Fresh install | New install, no existing DB | Room creates DB at version 5 | All 5 tables created (including `cached_messages`) |

**Acceptance**: Migration tested with Robolectric `MigrationTestHelper`. Export schema v5 JSON committed.

### REQ-CC-003 — All Strings Localized

All new UI strings MUST be added to `Localization.kt` with English AND Spanish translations. New string keys required: `chat_button`, `chat_title`, `chat_empty`, `chat_hint`, `chat_send`, `record_video`, `recording_timer`, `uploading`, `upload_progress`, `video_time_limit_reached`, `blur_processing`, `blur_warning_no_faces`, `blur_warning_error`, `blur_skipped_adult`, `camera_permission_required`, `btn_upload`, `btn_discard`, `btn_retry`.

| Scenario | Given | When | Then |
|----------|-------|------|------|
| English display | App language is ENGLISH; ChatScreen renders | `Localization.get("chat_empty", ENGLISH)` called | Returns "No messages yet. Start the conversation!" |
| Spanish display | App language is SPANISH; ChatScreen renders | `Localization.get("chat_empty", SPANISH)` called | Returns Spanish translation (not English fallback) |

**Acceptance**: All 18+ new keys exist in both `en` and `es` maps. `en.keys` ∪ `es.keys` contains all new keys.

### REQ-CC-004 — CAMERA Permission at Runtime

The app MUST request `android.permission.CAMERA` at runtime before opening `VideoRecordScreen`. A rationale dialog SHOULD explain: "Camera access is needed to record practice demonstration videos." Denial SHALL show an error state; never crash.

| Scenario | Given | When | Then |
|----------|-------|------|------|
| First launch | CAMERA permission not granted | User taps "Record Video" | System permission dialog appears with rationale |
| Denied | User denies permission | Permission result `false` | Error text "Camera permission required to record videos" shown; recording blocked |
| Granted | User grants permission | Permission result `true` | Preview starts; recording enabled |

**Acceptance**: `ActivityResultContracts.RequestPermission` used. No crash on denial.

### REQ-CC-005 — RECORD_AUDIO Permission Reused

The existing `RECORD_AUDIO` permission (already required for `SmartTuner`) MUST be reused for video audio. If already granted, no re-request. If denied, video recording SHALL still proceed (silent video allowed with warning).

| Scenario | Given | When | Then |
|----------|-------|------|------|
| Already granted | RECORD_AUDIO granted for Tuner | VideoRecordScreen opens | No permission prompt; audio recording included in video |
| Denied | RECORD_AUDIO denied (user tapped "Deny & don't ask again") | VideoRecordScreen opens | Recording proceeds with video-only (no audio); warning: "Microphone unavailable — video will have no audio" |

**Acceptance**: No additional `<uses-permission>` needed. Existing permission check reused.

---

## Requirement Summary

| SPEC  | Requirements                                    | Count |
|-------|-------------------------------------------------|-------|
| SPEC-1 (Chat)          | REQ-CHAT-001 .. REQ-CHAT-008                    | 8     |
| SPEC-2 (Video)         | REQ-VID-001 .. REQ-VID-009                       | 9     |
| SPEC-3 (Face Blur)     | REQ-BLR-001 .. REQ-BLR-008                       | 8     |
| SPEC-4 (Cross-cutting) | REQ-CC-001 .. REQ-CC-005                         | 5     |
| **Total**              |                                                 | **30** |
