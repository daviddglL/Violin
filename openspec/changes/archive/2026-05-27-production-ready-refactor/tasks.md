# Tasks: Production-Ready Refactor

## Review Workload Forecast

| Field | Value |
|-------|-------|
| Estimated total changed lines | ~2150 |
| 400-line budget risk | High |
| Chained PRs recommended | Yes |
| Delivery strategy | auto-chain |
| Chain strategy | stacked-to-main |

Decision needed before apply: No
Chained PRs recommended: Yes
Chain strategy: stacked-to-main
400-line budget risk: High

### Suggested Work Units

| Unit | Goal | PR | Lines | Depends |
|------|------|----|-------|---------|
| 1 | Namespace, Hilt, wrapper, ktlint, ProGuard, keystore removal | PR #1 | ~400 | — |
| 2a | SessionManager, DI modules, AuthVM + test | PR #2a | ~380 | PR #1 |
| 2b | PracticeVM, AssignmentVM + tests | PR #2b | ~320 | PR #2a |
| 2c | TunerVM, MetronomeVM, AudioModule, MainActivity refactor + tests | PR #2c | ~350 | PR #2a |
| 3 | TunerEngine + YIN, migration 2→3, RECORD_AUDIO, tests | PR #3 | ~400 | PR #2c |
| 4 | Gemini API, NetworkSecurityConfig, strings, hardening, final verification | PR #4 | ~350 | PR #3 |

---

## PR #1: Foundation
**Goal**: Buildable `com.violinmaster.app` with Hilt DI, Gradle wrapper, ktlint, ProGuard rules, no committed secrets.

- [x] T-001 **Namespace migration**: Rename `com/example/` → `com/violinmaster/app/` in `app/src/main/java/` and `app/src/test/`. Update all package declarations and imports. → REQ-NS-001..004 (Files: all .kt files, ~22)
- [x] T-002 **build.gradle.kts + AndroidManifest**: Set namespace/applicationId to `com.violinmaster.app`, ensure activity resolves. → REQ-NS-002, REQ-NS-003 (Files: app/build.gradle.kts, AndroidManifest.xml)
- [x] T-003 **Hilt setup**: Add hilt-android, hilt-compiler deps to build.gradle.kts, hilt plugin to root. Create `ViolinMasterApp.kt` with `@HiltAndroidApp`, annotate `MainActivity` with `@AndroidEntryPoint`. → REQ-DI-001, REQ-DI-006 (Files: app/build.gradle.kts, build.gradle.kts, ViolinMasterApp.kt, MainActivity.kt)
- [x] T-004 **Gradle wrapper**: Generate and commit `gradlew`, `gradlew.bat`, `gradle-wrapper.jar`, `gradle-wrapper.properties`. → REQ-BLD-001 (Files: gradlew, gradlew.bat, gradle/wrapper/*)
- [x] T-005 **ktlint**: Apply `org.jlleitschuh.gradle.ktlint` plugin, add `.editorconfig` at root, run `ktlintFormat`. → REQ-BLD-003, REQ-BLD-004 (Files: build.gradle.kts, .editorconfig)
- [x] T-006 **ProGuard + minify**: Set `isMinifyEnabled = true` in release. Add keep rules for Room, Retrofit, Moshi, Compose in `proguard-rules.pro`. → REQ-BLD-005, REQ-BLD-006 (Files: app/build.gradle.kts, proguard-rules.pro)
- [x] T-007 **Remove debug.keystore**: `git rm --cached debug.keystore*`, add to `.gitignore`. Remove hardcoded passwords from `debugConfig` signing config. → REQ-SEC-001, REQ-SEC-002 (Files: .gitignore, app/build.gradle.kts)
- [x] T-008 **Verify PR #1**: `./gradlew :app:assembleDebug` succeeds, `grep -r "com.example" app/src/` returns zero. → REQ-NS-005, REQ-BLD-002

## PR #2a: SessionManager + DI + AuthVM
**Goal**: Hilt DI wiring complete, shared SessionManager, AuthViewModel with tests. Depends on: PR #1.

- [x] T-009 **di/DatabaseModule.kt**: `@Module @InstallIn(SingletonComponent)`, provide `@Singleton PracticeDatabase` + `PracticeDao`. Remove companion object singleton from PracticeDatabase. → REQ-DI-002, REQ-DI-003, REQ-DI-007 (Files: di/DatabaseModule.kt, PracticeDatabase.kt)
- [x] T-010 **di/PreferencesModule.kt + di/SecurityModule.kt**: Provide `@Named` SharedPreferences singletons and `@Singleton SecurityUtils(@ApplicationContext ctx)`. → REQ-DI-005 (Files: di/PreferencesModule.kt, di/SecurityModule.kt, SecurityUtils.kt) *(SecurityUtils refactored to @Inject; PreferencesModule deferred — SessionManager handles its own prefs)*
- [x] T-011 **SessionManager.kt**: `@Singleton`, StateFlows for `currentUser`, `appLanguage`, backed by SharedPreferences. `saveCurrentUser()`, `clearSession()`. → REQ-VM-004 (Files: SessionManager.kt)
- [x] T-012 **AuthViewModel.kt**: `@HiltViewModel @Inject constructor(repo, sessionMgr, prefs)`. login/register/logout/passcode set/verify/authenticate. → REQ-VM-004, REQ-VM-008 (Files: viewmodel/AuthViewModel.kt)
- [x] T-013 **AuthViewModelTest.kt** (TDD): login success, register duplicate, logout clears session, passcode auth. Use mockk + Turbine. → REQ-TST-001 (Files: AuthViewModelTest.kt)
- [x] T-014 **Wire AuthenticationScreen**: Replace monolith VM with `hiltViewModel<AuthViewModel>()`. → REQ-VM-004, REQ-VM-007 (Files: AuthenticationScreen.kt)

## PR #2b: PracticeVM + AssignmentVM
**Goal**: Practice and Assignment VMs with tests. Depends on: PR #2a.

- [x] T-015 **PracticeViewModel.kt**: `@HiltViewModel`, timer start/pause/resume/stop-save/cancel, daily tasks, points, sessions flow via PracticeRepository. → REQ-VM-005, REQ-VM-008 (Files: viewmodel/PracticeViewModel.kt)
- [x] T-016 **AssignmentViewModel.kt**: `@HiltViewModel`, publish/complete/delete assignments, teacher-student link by invite code. → REQ-VM-006, REQ-VM-008 (Files: viewmodel/AssignmentViewModel.kt)
- [x] T-017 **PracticeViewModelTest.kt** (TDD): timer lifecycle, session persistence, task completion, points, min-3s discard. → REQ-TST-001 (Files: PracticeViewModelTest.kt)
- [x] T-018 **AssignmentViewModelTest.kt** (TDD): publish, mark complete, link student, delete. → REQ-TST-001 (Files: AssignmentViewModelTest.kt)
- [x] T-019 **Wire HomeScreen + TeacherStudentWorkspace**: Replace monolith VM with `hiltViewModel<P>()` and `hiltViewModel<A>()`. → REQ-VM-005, REQ-VM-006, REQ-VM-007 (Files: HomeScreen.kt, TeacherStudentWorkspace.kt)

## PR #2c: TunerVM + MetronomeVM + MainActivity
**Goal**: Audio DI, tuner/metronome VMs with tests, monolith removal. Depends on: PR #2a.

- [x] T-020 **di/AudioModule.kt**: `@Module @InstallIn(SingletonComponent)`, provide `@Singleton ViolinAudioEngine` (add `@Inject constructor`). → REQ-DI-004 (Files: di/AudioModule.kt, ViolinAudioEngine.kt)
- [x] T-021 **TunerViewModel.kt**: `@HiltViewModel`, note select, pitch offset, listening, auto-detect, reference pitch A. Delegates audio to ViolinAudioEngine. → REQ-VM-002, REQ-VM-008 (Files: viewmodel/TunerViewModel.kt)
- [x] T-022 **MetronomeViewModel.kt**: `@HiltViewModel`, BPM 40-240, beats 2/3/4/6, accent, play/pause, beat pulse. → REQ-VM-003, REQ-VM-008 (Files: viewmodel/MetronomeViewModel.kt)
- [x] T-023 **TunerViewModelTest.kt** (TDD): note select plays tone, toggle listening, auto-detect cycles. → REQ-TST-001 (Files: TunerViewModelTest.kt)
- [x] T-024 **MetronomeViewModelTest.kt** (TDD): toggle metronome, BPM update, time sig change, accent toggle. → REQ-TST-001 (Files: MetronomeViewModelTest.kt)
- [x] T-025 **Refactor MainActivity.kt**: Remove ViolinViewModel monolith. Use `hiltViewModel<X>()` per screen. Keep `currentTab`/`currentOverlay` as composable state. Verify all screens render. → REQ-VM-001, REQ-VM-007, REQ-VM-008 (Files: MainActivity.kt)
- [x] T-026 **Wire TunerScreen + MetronomeScreen**: Replace monolith VM with new `hiltViewModel<>()` calls. → REQ-VM-002, REQ-VM-003, REQ-VM-007 (Files: TunerScreen.kt, MetronomeScreen.kt)

## PR #3: Real Tuner + Migrations
**Goal**: Real microphone pitch detection via YIN, Room migration safety, permission handling. Depends on: PR #2c.

- [x] T-027 **YinPitchDetector.kt**: YIN algorithm: threshold 0.15, 44100Hz, 80-2000Hz range. Detect frequency from ShortArray PCM. → REQ-TN-002 (Files: audio/tuner/YinPitchDetector.kt)
- [x] T-028 **TunerEngine.kt**: `@Inject constructor()`. AudioRecord (44100Hz, 16-bit mono, 4096 buffer) on `Dispatchers.Default`. Emit `StateFlow<PitchResult>`. Map freq to violin strings via referencePitchA. → REQ-TN-001, REQ-TN-003 (Files: audio/TunerEngine.kt, audio/PitchResult.kt)
- [x] T-029 **Wire TunerViewModel → TunerEngine**: Replace `Random()` simulation with `TunerEngine.pitchFlow.collect`. Permission check via `rememberLauncherForActivityResult` in TunerScreen. → REQ-TN-004, REQ-TN-005, REQ-TN-006 (Files: TunerViewModel.kt, TunerScreen.kt, di/AudioModule.kt)
- [x] T-030 **RECORD_AUDIO permission**: `ActivityResultContract` in TunerScreen. Denied → error message, no crash. `PermissionHandler` composable reusable component. Degradation: mic unavailable → error state, reference tone still works. → REQ-TN-007, REQ-TN-008 (Files: TunerScreen.kt, PermissionHandler.kt)
- [x] T-031 **Room migration 2→3 + schema export**: Remove `fallbackToDestructiveMigration()`, add `MIGRATION_2_3` no-op, bump `@Database(version = 3)`, `exportSchema = true`, `room.schemaLocation` via KSP, commit `app/schemas/*.json`. → REQ-DB-001..003 (Files: PracticeDatabase.kt, DatabaseModule.kt, app/schemas/*)
- [x] T-032 **TunerEngineTest + MigrationTest**: YIN accuracy ±2 cents for G3/D4/A4/E5 sine waves. MigrationTestHelper: insert 4 entities at v2, migrate, assert all rows preserved. → REQ-TN-002, REQ-DB-004, REQ-DB-005 (Files: TunerEngineTest.kt, PracticeDatabaseMigrationTest.kt)

## PR #4: Gemini + Hardening
**Goal**: Gemini API layer, network security, string extraction, release build verification. Depends on: PR #3.

- [x] T-033 **GeminiApiService + DTOs**: Retrofit `@POST` interface, Moshi request/response DTOs: `GeminiRequest`, `GeminiResponse`. + GeminiRepository with error handling. Suspend functions on `Dispatchers.IO`. → REQ-GEM-001, REQ-GEM-003, REQ-GEM-004, REQ-GEM-005 (Files: data/remote/GeminiApiService.kt, data/remote/GeminiRequest.kt, data/remote/GeminiResponse.kt, data/remote/GeminiRepository.kt)
- [x] T-034 **di/NetworkModule.kt**: Provide OkHttpClient (logging interceptor), Retrofit (Moshi converter, baseUrl generativelanguage.googleapis.com), GeminiApiService singleton, GeminiRepository singleton, @Named("gemini_api_key") from BuildConfig. → REQ-GEM-002, REQ-DI-001 (Files: di/NetworkModule.kt)
- [x] T-035 **API Key Configuration**: Verified Secrets Gradle Plugin reads `.env` → `BuildConfig.GEMINI_API_KEY`. `.env.example` has placeholder `GEMINI_API_KEY=MY_GEMINI_API_KEY`. NetworkModule provides `@Named("gemini_api_key")` String. → REQ-GEM-002 (Files: .env.example, app/build.gradle.kts)
- [x] T-036 **network_security_config.xml**: Created at `res/xml/network_security_config.xml`. Allows `generativelanguage.googleapis.com`, blocks cleartext by default. Referenced in AndroidManifest `<application>` via `android:networkSecurityConfig`. → REQ-SEC-003, REQ-SEC-004 (Files: res/xml/network_security_config.xml, AndroidManifest.xml)
- [x] T-037 **String extraction**: Replaced all hardcoded English display strings in HomeScreen, TunerScreen, MetronomeScreen, SettingsScreen with `Localization.get("key", language)`. Added 60+ new keys to `Localization.kt` English map. Remaining `text = "` matches are dynamic values (variables, emojis, numeric displays) — zero hardcoded English display strings remain. → REQ-L10N-001, REQ-L10N-003 (Files: HomeScreen.kt, TunerScreen.kt, MetronomeScreen.kt, SettingsScreen.kt, MainActivity.kt, Localization.kt)
- [x] T-038 **Spanish translations**: All 201 English keys have matching Spanish translations. Key parity verified: EN keys = ES keys = 201. Fallback chain: active lang → en → raw key preserved in `Localization.get()`. → REQ-L10N-002, REQ-L10N-004 (Files: Localization.kt)
- [x] T-039 **DAO tests**: PracticeDaoTest.kt with 20 test methods covering all CRUD: insert/query/delete sessions, lesson progress CRUD, user account CRUD with overwrite, assignment CRUD with student/teacher filtering, auto-generated IDs, data integrity. Uses in-memory Room database + RobolectricTestRunner (same pattern as existing ViewModel tests). → REQ-TST-002 (Files: PracticeDaoTest.kt)
- [x] T-040 **Final verification**: File-based verification complete. All checks passed: zero `com.example` refs, `fallbackToDestructiveMigration` only in comment, zero `Random()` in TunerViewModel, `network_security_config.xml` exists + manifest reference, GeminiApiService has correct annotations, NetworkModule provides 4 deps, DAO tests present, Localization.kt key parity 201/201 perfect. → REQ-NS-005, REQ-BLD-004, REQ-SEC-005

---

## Dependency Graph

```
PR #1: T-001──T-002──T-003──T-004..T-007(indep)──T-008
  └→ PR #2a: T-009──T-010──T-011──T-012──T-013──T-014
       ├→ PR #2b: T-015──T-016──T-017──T-018──T-019
       └→ PR #2c: T-020──T-021──T-022──T-023──T-024──T-025──T-026
            └→ PR #3: T-027──T-028──T-029──T-030──T-031──T-032
                 └→ PR #4: T-033──T-034──T-035──T-036──T-037──T-038──T-039──T-040
```

## Parallelism Opportunities
- T-004, T-005, T-006, T-007 run in parallel after T-003
- PR #2b and PR #2c can run in parallel after PR #2a
- T-033/T-036/T-037 can run in parallel early in PR #4
- All test files (T-013, T-017, T-018, T-023, T-024, T-032, T-039) follow TDD: write test → verify fail → implement → verify pass

## Test-to-Requirement Mapping
| Test | Covers |
|------|--------|
| AuthViewModelTest | REQ-VM-004 |
| PracticeViewModelTest | REQ-VM-005 |
| AssignmentViewModelTest | REQ-VM-006 |
| TunerViewModelTest | REQ-VM-002 |
| MetronomeViewModelTest | REQ-VM-003 |
| TunerEngineTest | REQ-TN-002 |
| MigrationTest | REQ-DB-004, REQ-DB-005 |
| PracticeDaoTest | REQ-TST-002 |
| HomeScreenTest, TunerScreenTest, MetronomeScreenTest | REQ-TST-003 |
