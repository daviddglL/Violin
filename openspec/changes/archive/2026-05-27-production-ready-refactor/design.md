# Design: Production-Ready Refactor

## Technical Approach

Apply Clean Architecture + MVVM via 4 stacked PRs: foundation (namespace + DI + tooling), ViewModel split, real tuner + migrations, Gemini + hardening. All 58 spec requirements map to concrete file changes. Behavioral preservation is the non-negotiable constraint — every split ViewModel replicates the monolith's exact logic.

## Architecture Decisions

| Decision | Choice | Rejected | Rationale |
|---|---|---|---|
| Pitch detection algorithm | YIN (autocorrelation-based) | FFT with HPS | YIN is purpose-built for monophonic instruments; lower latency at 44100 Hz; ±2 cent accuracy proven on violin |
| Shared state pattern | SessionManager @Singleton injected into all VMs | SharedFlow in companion object | Hilt-native; survives process death via SharedPreferences; testable with mock injection |
| DI framework | Hilt (Dagger-based) | Koin, manual DI | Google-recommended for Android; compile-time safety; existing Room/Compose ecosystem pairs well |
| Room version bump | 2→3 as no-op migration | Add new columns | No schema changes needed in this refactor; preserves all user data; migration test validates integrity |
| String extraction | Extend existing Localization.kt | strings.xml per locale | Project already uses Localization.kt pattern; avoids two localization systems |
| SecurityUtils refactor | Add @Inject constructor, keep as injectable singleton | Convert to object with Hilt-provided Context | Preserves existing crypto logic; makes testable by injecting Context |
| R8 config | Keep rules per library | Use single consumer rule file | Retrofit + Room + Moshi each need specific keep rules; granular control prevents runtime crashes |

## Package Structure

```
com.violinmaster.app/
  ViolinMasterApp.kt          — @HiltAndroidApp Application class
  di/
    DatabaseModule.kt         — @Singleton PracticeDatabase, PracticeDao
    AudioModule.kt            — @Singleton ViolinAudioEngine, TunerEngine
    RepositoryModule.kt       — @Singleton PracticeRepository
    NetworkModule.kt          — OkHttpClient, Retrofit, GeminiApiService
    SecurityModule.kt         — SecurityUtils, VideoSecurityService
    PreferencesModule.kt      — SharedPreferences (app_settings, secure_user_prefs)
  domain/
    model/                    — Domain entities (mirror Room entities for now)
    repository/PracticeRepository.kt  — Interface (extracted from class)
    usecase/                  — Future use cases (empty for this refactor)
  data/
    local/                    — Room DB, DAOs, entities (existing, renamed)
    remote/                   — GeminiApiService, request/response DTOs
    PracticeRepositoryImpl.kt — Implements domain interface
  ui/
    theme/                    — Color, Type, Theme, Localization
    screens/                  — Home, Tuner, Metronome, Settings, Lessons, Stats, Auth, Workspace
    navigation/               — (future)
  audio/
    ViolinAudioEngine.kt      — Existing AudioTrack playback (tone + metronome)
    TunerEngine.kt            — AudioRecord capture + YIN pitch detection
    tuner/YinPitchDetector.kt — YIN algorithm implementation
  security/                   — SecurityUtils, VideoSecurityService (existing, refactored)
  util/                       — Extensions, DateFormatters
```

## ViewModel Decomposition

**Shared state**: `SessionManager` (@Singleton) holds `appLanguage`, `currentUser` via StateFlows backed by SharedPreferences. Injected into all VMs.

| ViewModel | Lines | Owns | From monolith (lines) |
|---|---|---|---|
| TunerViewModel | ~180 | tunerSelectedNote, pitchOffset, isListening, autoDetect, referencePitch | 77-96, 371-453 |
| MetronomeViewModel | ~100 | bpm, beats, accent, isPlaying, beatPulse | 316-370 |
| AuthViewModel | ~200 | login/register/logout, passcode lock/verify, currentUser flow (via SessionManager) | 44-51, 523-700 |
| PracticeViewModel | ~250 | timer, sessions, daily tasks, points, lessons, demo history | 77-310, 456-510, 756-790 |
| AssignmentViewModel | ~120 | teacher/student assignments, publish, complete, link | 53-58, 713-750 |

**State flow**: `TunerEngine` → Flow<Float> → TunerViewModel → StateFlow → TunerScreen. `MetronomeViewModel` and `TunerViewModel` both reference `@Singleton ViolinAudioEngine`.

**Navigation**: `currentTab` and `currentOverlay` stay in `MainActivity` composable state — they control which Screen renders and which ViewModel is via `hiltViewModel()`.

## Hilt DI Design

```kotlin
@HiltAndroidApp
class ViolinMasterApp : Application()

@Module @InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides @Singleton
    fun provideDatabase(@ApplicationContext ctx: Context): PracticeDatabase =
        Room.databaseBuilder(ctx, PracticeDatabase::class.java, "violin_master_database")
            .addMigrations(MIGRATION_2_3)
            .build()
    @Provides fun provideDao(db: PracticeDatabase): PracticeDao = db.practiceDao()
}

@Module @InstallIn(SingletonComponent::class)
object PreferencesModule {
    @Provides @Singleton @Named("app_settings")
    fun appPrefs(@ApplicationContext ctx: Context) = ctx.getSharedPreferences("app_settings", MODE_PRIVATE)
    @Provides @Singleton @Named("secure_prefs")
    fun securePrefs(@ApplicationContext ctx: Context) = ctx.getSharedPreferences("secure_user_prefs", MODE_PRIVATE)
}
```

## Real Tuner Architecture

**TunerEngine**: `AudioRecord` on dedicated thread (`Dispatchers.Default`), sample rate 44100 Hz, 16-bit mono, buffer 4096 samples (~93ms). YIN algorithm on computation thread via `flow { }`. Emits `TunerResult(noteName: String?, cents: Float)` as `SharedFlow`.

**Permission flow**: `TunerViewModel` checks `ContextCompat.checkSelfPermission(RECORD_AUDIO)`. Not granted → `ActivityResultContract.RequestPermission`. Denied → `tunerError` state = "Microphone permission required". `AudioRecord` init failure → caught, no crash, `tunerError` set.

**YIN implementation**: threshold 0.15, sample rate 44100, min frequency 80 Hz, max 2000 Hz. Wraps results in violin string mapping (G3=196, D4=293.66, A4=440, E5=659.25) adjusted by `referencePitchA` multiplier.

**Degradation**: if `AudioRecord` constructor throws → `tunerError = "Microphone unavailable"`. Reference tone playback (AudioTrack) remains functional — separate code path, unaffected.

## Room Migration (2→3)

```kotlin
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // No-op: schema unchanged. Data preserved.
    }
}
```

`@Database(version = 3, exportSchema = true)`. Schema JSON exported to `app/schemas/`. Tested with `MigrationTestHelper`: insert 4 entities at v2, migrate to v3, assert all rows queryable. No `fallbackToDestructiveMigration()`.

## Data Flow Diagrams

**Tuner flow**: Mic → AudioRecord (dedicated thread) → 4096-sample buffer → YIN algorithm → `TunerResult(note, cents)` → TunerViewModel (combines with referencePitchA, autoDetect) → `tunerPitchOffsetCents` StateFlow → TunerScreen Canvas needle.

**Practice flow**: "Start" → PracticeViewModel.startPracticeTimer(category) → coroutine timer (delay 1s) → stopAndSave → PracticeRepository.insertSession → Room insert → allSessions Flow emits → HomeScreen recomposes.

**Auth flow**: login(username, pin) → AuthViewModel → repository.getUserByUsername → SecurityUtils.verifyHash → SessionManager.saveCurrentUser(user) → SharedPreferences persist → currentUser StateFlow emits → MainLayout shows authenticated UI.

## PR Slice Strategy (Stacked to Main)

| PR | Scope | Files | Est. Lines | Builds? | Tests |
|---|---|---|---|---|---|
| #1 Foundation | Namespace, Hilt, wrapper, ktlint, ProGuard | ~30 | ~400 | ✅ assembleDebug | Placeholder tests pass |
| #2 ViewModel Split | 5 VMs + SessionManager + MainActivity refactor | ~20 | ~900 | ✅ | 5 VM test files, all pass |
| #3 Real Tuner | TunerEngine + YIN + permission + migration | ~10 | ~500 | ✅ | TunerEngine unit tests, migration test |
| #4 Gemini + Hardening | API layer, strings, security config | ~15 | ~350 | ✅ | API error handling tests |

**Total estimated**: ~2150 changed lines across 4 PRs. Chained PRs recommended for PR #2 (900 lines exceeds 400-line budget). Each PR independently buildable and revertible.

## Testing Strategy

| Layer | What | How |
|---|---|---|
| Unit: ViewModels | State emissions, method calls | JUnit4 + Turbine + mockk, inject fake dependencies |
| Unit: TunerEngine | YIN accuracy with sine waves | Generate PCM ShortArray at known freqs, assert ±2 cents |
| Unit: DAO | CRUD operations | Robolectric + in-memory Room, insert → query → assert |
| UI: Screens | Element existence, click behavior | `createComposeRule()` + `onNodeWithTag()`, mock VMs |
| Migration | V2→V3 data preservation | `MigrationTestHelper`, insert at v2, migrate, verify counts |

## Risk Mitigation

| Risk | Mitigation |
|---|---|
| YIN accuracy on real violin | Test with pre-recorded WAV samples at G3/D4/A4/E5; benchmark before commit |
| DB schema drift | `exportSchema = true` → JSON committed; CI diffs schema on PR |
| Direct Context accesses | Identified 8 sites in VM; replaced with `@ApplicationContext` injection or SessionManager |
| 900-line PR #2 exceeds budget | Auto-chain into sub-PRs: SessionManager + Auth VM → Practice + Assignment VMs → Tuner + Metronome VMs + MainActivity |

## Open Questions

None — all decisions have clear rationale from codebase inspection.
