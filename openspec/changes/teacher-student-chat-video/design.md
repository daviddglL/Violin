# Design: Teacher-Student Chat & Video Sharing

## Technical Approach

Three-phase pipeline integrating Firestore real-time chat, CameraX recording, and on-device face blur into the existing Hilt/Room/Compose architecture. Follows existing: `@Module`/`@InstallIn(SingletonComponent::class)` object modules, Ro-bolectric Room tests, and `Localization` string registry.

## Architecture Decisions

| Decision | Choice | Rejected | Why |
|----------|--------|----------|-----|
| Firestore path | `assignments/{assignmentId}/messages/{autoId}` | Top-level `/messages` with `assignmentId` filter | Path scoping = free Firestore isolation; no cross-assignment query |
| Message caching | Room `cached_messages` table + Firestore offline persistence (both) | Room-only or Firestore-only | Dual cache: Firestore handles real-time sync; Room survives local DB clear |
| Face blur: frame sampling | Every 5th frame with linear interpolation | Every frame (too slow) or keyframe-only (misses fast motion) | Balances detection cost with temporal continuity |
| Compression position | After blur (when applicable) | Before blur | Blur on raw preserves quality; compress once after all processing |
| Hilt module | Single `FirebaseModule` for both Firestore + Storage | Two separate modules | Both are Firebase SDK singletons; one module is simpler DI |
| Repository pattern | `ChatRepository` injects `FirebaseFirestore` + `PracticeDao` directly | Layer with separate FirestoreService | Lean: Firestore SDK IS the API; no wrapper needed |

## Data Flow

### Send text message
```
ChatViewModel.sendMessage(text)
  → ChatRepository.sendMessage(assignmentId, Message(...))
    → firestore.collection("assignments/$id/messages").add(msg)  [Dispatchers.IO]
      → Firestore snapshot listener fires → ChatRepository caches to Room
        → Flow emits updated List<Message> → ChatScreen recomposes
```

### Teacher records + uploads
```
VideoRecordScreen: CameraX preview + record button
  → VideoRecordingService.startRecording(cacheFile)  [Main thread for CameraX]
    → stopRecording() → raw .mp4 in cacheDir
  → VideoCompressionService.compress(rawFile)  [Dispatchers.Default]
    → MediaCodec async → compressedFile
  → VideoUploadService.upload(compressedFile, teacher, assignmentId)  [Dispatchers.IO]
    → StorageReference.putFile(uri) with progress listener
    → getDownloadUrl() → ChatRepository.sendMessage(attachmentUrl=url, type="video")
    → delete temp file
```

### Student records with blur
```
Same as teacher, with FaceBlurProcessor inserted before compression:
  rawFile → FaceBlurProcessor.process(rawFile)  [Dispatchers.Default]
    → MediaExtractor extracts frames → ML Kit detect every 5th
    → Canvas Gaussian blur on face boxes → MediaCodec re-encode
  → compressedFile → VideoCompressionService → upload
  Gated by: SessionManager.isCurrentUserMinor
  Fallback: if 0 faces in first 30s, skip blur with warning Snackbar
```

## File Changes

| File | Action | Description |
|------|--------|-------------|
| `di/FirebaseModule.kt` | Create | `@Provides @Singleton` for `FirebaseFirestore` + `FirebaseStorage` |
| `data/local/CachedMessage.kt` | Create | Room entity mirroring Firestore message fields |
| `data/remote/ChatRepository.kt` | Create | Firestore listener → Room sync; `sendMessage()`, `observeMessages()` |
| `data/remote/VideoUploadService.kt` | Create | Firebase Storage `putFile` + progress `StateFlow` |
| `service/VideoRecordingService.kt` | Create | CameraX `VideoCapture` + `Preview` wrapper |
| `service/VideoCompressionService.kt` | Create | `MediaCodec` async H.264 compression (720p, 1Mbps) |
| `service/FaceBlurProcessor.kt` | Create | ML Kit face detect every 5th frame → Gaussian blur → re-encode |
| `ui/screens/ChatScreen.kt` | Create | `LazyColumn` + `OutlinedTextField` + sender-aligned bubbles |
| `ui/screens/VideoRecordScreen.kt` | Create | CameraX `PreviewView`, FAB, timer, upload/discard |
| `ui/viewmodel/ChatViewModel.kt` | Create | `@HiltViewModel` with messages `StateFlow` + `sendMessage()` |
| `ui/viewmodel/VideoUploadViewModel.kt` | Create | Recording/compression/upload state machine via `StateFlow` |
| `ui/components/MessageBubble.kt` | Create | Composable for teacher (right, primary) / student (left, surface) |
| `ui/components/VideoPreview.kt` | Create | Post-recording thumbnail + upload/discard/retry buttons |
| `data/PracticeDatabase.kt` | Modify | v4→v5: add `CachedMessage` entity, `CachedMessageDao`, `MIGRATION_4_5` |
| `data/PracticeDao.kt` | Modify | Add `CachedMessageDao` interface |
| `ui/screens/TeacherStudentWorkspace.kt` | Modify | Add "Chat" `TextButton` per assignment card (teacher + student tabs) |
| `ui/theme/Localization.kt` | Modify | Add 18+ string keys in `en` + `es` maps |
| `app/proguard-rules.pro` | Verify | Rules for Firebase + ML Kit already present (lines 107-115) |

## Message Data Model (shared Firestore ↔ Room)

```kotlin
data class Message(
    val id: String = "",           // Firestore document ID
    val assignmentId: String,
    val senderUsername: String,
    val senderRole: String,        // "TEACHER" | "STUDENT"
    val text: String,
    val attachmentUrl: String = "",
    val attachmentType: String = "",  // "" or "video"
    val timestamp: Long,           // epoch millis
    val read: Boolean = false
)
```

## Room Migration 4→5

```sql
CREATE TABLE cached_messages (
    id TEXT NOT NULL PRIMARY KEY,
    assignmentId TEXT NOT NULL,
    senderUsername TEXT NOT NULL,
    senderRole TEXT NOT NULL,
    text TEXT NOT NULL,
    attachmentUrl TEXT NOT NULL DEFAULT '',
    attachmentType TEXT NOT NULL DEFAULT '',
    timestamp INTEGER NOT NULL,
    read INTEGER NOT NULL DEFAULT 0
);
CREATE INDEX idx_cached_msg_assignment ON cached_messages(assignmentId, timestamp ASC);
```

**Firestore composite index**: `assignments/{aId}/messages` → `timestamp ASC`, `read ASC` (for "unread first" queries).

## ViewModel Design

| VM | Key State | Dependencies | Threading |
|----|-----------|--------------|-----------|
| `ChatViewModel` | `messages: StateFlow<List<Message>>`, `isLoading`, `sendError` | `ChatRepository`, `SessionManager` | `sendMessage()` on `viewModelScope` (repo does IO internally) |
| `VideoUploadViewModel` | `recordingState` (IDLE/RECORDING/DONE), `compressionProgress`, `uploadProgress`, `error` | `VideoRecordingService`, `VideoCompressionService`, `FaceBlurProcessor`, `VideoUploadService`, `SessionManager` | Cam-eraX on Main; compress/blur on `Dispatchers.Default`; upload on `Dispatchers.IO` |

## Testing Strategy

| Layer | Target | Approach |
|-------|--------|----------|
| Unit | `ChatViewModel` | In-memory Room + fake `FirebaseFirestore` (mock) |
| Unit | `VideoUploadViewModel` | Test state machine transitions with fake services |
| Unit | `ChatRepository` | Mock `FirebaseFirestore` + in-memory `PracticeDao` |
| Integration | `VideoCompressionService` | Short synthetic video (1s), verify bitrate/resolution post-compression |
| Integration | `FaceBlurProcessor` | Robolectric + ML Kit with sample image frames; verify blur applied |
| Integration | Room Migration 4→5 | `MigrationTestHelper` — verify `cached_messages` table created, existing data intact |
| Compose | `ChatScreen` | Roborazzi screenshot test: teacher/student alignment, empty state |
| Compose | `VideoRecordScreen` | PreviewView + FAB state toggle; timer format |

## Risk Mitigation

| Risk | Mitigation | Where |
|------|-----------|-------|
| Face blur: 5400 frames too slow | Every-5th-frame sampling (~1080 detections), FAST mode | `FaceBlurProcessor` |
| Firestore read costs | Room cache + single-assignment listener scope | `ChatRepository` |
| Storage bloat | 720p@1Mbps compression; delete on assignment removal | `VideoUploadService` + delete hook |
| ML Kit false negatives | Skip blur if 0 faces in first 30s + warning | `FaceBlurProcessor` |
| ProGuard strips Firebase/ML | Keep rules already in place (lines 107-115) | `proguard-rules.pro` |
| CameraX OOM on low-end | Surface encoder (MediaCodec) avoids buffer copies | `VideoRecordingService` |

## Open Questions

- None — all dependencies verified present in `build.gradle.kts`
