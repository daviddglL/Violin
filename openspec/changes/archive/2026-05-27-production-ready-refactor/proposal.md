# Proposal: Production-Ready Refactor

## Intent

Transform Violin Master from an AI Studio-generated prototype into a production-ready Android app suitable for Google Play release. The current codebase is functional as a demo but has critical blockers: fake tuner (Random-based simulation), monolithic ViewModel (804 lines), no real tests, and namespace/security issues preventing store submission.

## Scope

### In Scope

- **Namespace migration**: `com.example` → `com.violinmaster.app` across all manifests, build files, and package declarations
- **ViewModel decomposition**: Split 804-line `ViolinViewModel.kt` into focused ViewModels per domain (Tuner, Metronome, Auth, Practice, Security)
- **Real tuner implementation**: Replace `Random()` simulation with Android `AudioRecord`-based pitch detection
- **Room migration safety**: Replace `fallbackToDestructiveMigration()` with proper `Migration` objects and versioned schema
- **Dependency Injection**: Add Hilt DI, refactor object singletons → injectable services
- **Security hardening**: Remove `debug.keystore` from repo, add `network_security_config.xml`, rotate hardcoded passwords
- **Test coverage**: Add behavioral unit tests for all ViewModels, DAO tests, and composable UI tests using existing Robolectric + Roborazzi setup
- **String extraction**: Move all hardcoded strings to `Localization.kt` with ES translations
- **Build tooling**: Add Gradle wrapper (`gradlew`), enable `isMinifyEnabled` for release, add ktlint
- **Gemini API integration**: Scaffold real API call layer for the `MAJOR_CAPABILITY_SERVER_SIDE_GEMINI_API`

### Out of Scope

- CameraX-based video recording (dependencies already commented out; future feature)
- Real secure video streaming backend (service stub remains; CDN integration is separate project)
- Firebase Auth/cloud sync migration
- Google Play Console submission process itself
- UI redesign or feature additions beyond what refactoring requires

## Capabilities

### New Capabilities

- `di-container`: Dependency injection via Hilt across all layers
- `pitch-detection`: Real microphone-based pitch detection replacing simulated tuner
- `database-migrations`: Proper Room migration path with versioned schema
- `gemini-api`: Server-side Gemini API integration layer
- `network-security`: Network security configuration for API traffic

### Modified Capabilities

None (all existing capabilities preserve behavior; implementation details change).

## Approach

**Architecture**: Apply Clean Architecture + MVVM layers:

```
domain/          <- Entities, UseCases, Repository interfaces (pure Kotlin)
data/            <- Room DAOs, Repository implementations, API clients
di/              <- Hilt modules wiring everything
ui/              <- Compose screens + ViewModels (one per screen)
audio/           <- AudioEngine, TunerEngine (real pitch detection)
security/        <- SecurityUtils, VideoSecurityService
```

**Stacked PR strategy** (auto-chain): PR #1 (foundation: namespace + DI + Gradle), PR #2 (ViewModel split + tests), PR #3 (tuner + migrations), PR #4 (Gemini + security hardening). Each PR delivers a buildable, testable increment.

## Affected Areas

| Area | Impact | Description |
|------|--------|-------------|
| `app/src/main/java/com/example/` | Renamed | Package migration to `com.violinmaster.app/` |
| `app/build.gradle.kts` | Modified | namespace, DI config, ProGuard, ktlint |
| `app/src/main/java/.../viewmodel/` | Split | One ViewModel → 5 focused ViewModels |
| `app/src/main/java/.../audio/` | Modified | Add real AudioRecord pitch detection |
| `app/src/main/java/.../data/` | Modified | Migration strategy, Hilt module |
| `app/src/main/java/.../security/` | Modified | Remove debug cert, add network config |
| `app/src/main/java/.../ui/` | Modified | Extract hardcoded strings to Localization |
| `app/src/test/`, `app/src/androidTest/` | Expanded | Real behavioral tests |
| `gradle/`, `gradlew`, `gradlew.bat` | Added | Gradle wrapper files |
| `debug.keystore` | Removed | Committed secret; use env-based signing |

## Risks

| Risk | Likelihood | Mitigation |
|------|------------|------------|
| Namespace migration breaks imports | High | Automate via IDE refactor; verify build per commit |
| ViewModel split introduces regressions | Medium | Write tests BEFORE splitting; verify behavior preservation |
| Real pitch detection performance on low-end devices | Medium | Benchmark on API 24 device; keep simulation fallback as debug toggle |
| Room migration may corrupt existing dev data | Low | Devs lose prototype data only (acceptable); add schema export |
| DI wiring complexity | Low | Single Hilt module per layer; follow Android Hilt guide |

## Rollback Plan

Each stacked PR is independently revertible. If a PR introduces a blocker: revert that PR to `main`; dependent PRs rebase. For full rollback: revert all four PRs in reverse order, restoring the prototype baseline.

## Dependencies

- Hilt library addition (no breaking changes to existing deps)
- Android SDK 36 + NDK (for audio processing; already present in build config)

## Success Criteria

- [ ] `com.example` fully replaced with `com.violinmaster.app`; `gradlew assembleDebug` succeeds
- [ ] No single class exceeds 300 lines (SRP; down from 804)
- [ ] Tuner reacts to real microphone input, not `Random()`
- [ ] `fallbackToDestructiveMigration()` removed; migration path tested
- [ ] 70%+ line coverage on ViewModels and DAOs
- [ ] All UI strings localized in EN/ES via `Localization.kt`
- [ ] `gradlew lint` and `gradlew ktlintCheck` pass with zero errors
- [ ] Release build with `isMinifyEnabled = true` succeeds
- [ ] `debug.keystore` removed from repo; `.gitignore` verified
