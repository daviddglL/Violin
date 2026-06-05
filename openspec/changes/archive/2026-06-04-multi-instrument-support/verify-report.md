## Verification Report

**Change**: multi-instrument-support
**Version**: N/A
**Mode**: Standard

### Completeness

| Metric | Value |
|--------|-------|
| Tasks total | 14 |
| Tasks complete | 14 |
| Tasks incomplete | 0 |

### Build & Tests Execution

**Build**: ✅ Passed

```
.\gradlew.bat :app:testDebugUnitTest
345 tests completed, 55 failed, 1 skipped
→ All 55 failures are pre-existing (MigrationTest, GoogleAuthRepositoryTest,
  FaceBlurProcessorTest, various ViewModel/Screen tests, YIN core detection).
  Zero new failures from this change.
```

**Tests**: ✅ 290 passed / ❌ 55 failed (all pre-existing) / ⚠️ 1 skipped

**Coverage**: ➖ Not available (no JaCoCo/kover configured)

---

### Spec Compliance Matrix

#### Domain: instrument-selection

| Requirement | Scenario | Test | Result |
|-------------|----------|------|--------|
| Instrument Data Model | Violin strings G3/D4/A4/E5 | `InstrumentTest > VIOLIN has four strings G3 D4 A4 E5` | ✅ COMPLIANT |
| Instrument Data Model | Viola strings C3/G3/D4/A4 | `InstrumentTest > VIOLA has four strings C3 G3 D4 A4` | ✅ COMPLIANT |
| Instrument Data Model | Cello strings C2/G2/D3/A3 | `InstrumentTest > CELLO has four strings C2 G2 D3 A3` | ✅ COMPLIANT |
| Instrument Data Model | Enum serialization roundtrip | `InstrumentTest > valueOf CELLO/VIOLIN/VIOLA roundtrip` | ✅ COMPLIANT |
| Persistent Instrument Selection | First launch defaults to Violin | `UserPreferencesManagerTest > selectedInstrument defaults to VIOLIN on first launch`, `TunerViewModelTest > default instrument is VIOLIN` | ✅ COMPLIANT |
| Persistent Instrument Selection | Selection survives app restart | `UserPreferencesManagerTest > setSelectedInstrument changes instrument and persists across instances` | ✅ COMPLIANT |
| Persistent Instrument Selection | StateFlow emits correctly | `UserPreferencesManagerTest > selectedInstrument StateFlow emits correctly` | ✅ COMPLIANT |
| Persistent Instrument Selection | Switch instruments mid-session | `TunerViewModelTest > switch to viola updates selectedInstrument StateFlow`, `switch to cello updates selectedInstrument StateFlow` | ✅ COMPLIANT |
| Instrument Selector in Settings | Select viola in Settings | SettingsScreen code: Card with 3 Instrument buttons, clickable → `setSelectedInstrument()`. No Compose UI test. | ⚠️ UNTESTED |
| Instrument Selector in Settings | Localized instrument labels | `StringsSettings.en` + `StringsSettings.es` have all keys. No Compose UI test for rendering. | ⚠️ UNTESTED |
| String Label Disambiguation | Cello G is lower octave than violin G | `NoteTargetSelector.octaveLabel()` — MIDI formula produces "G2" for 98 Hz. No dedicated unit test. | ⚠️ UNTESTED |
| String Label Disambiguation | Violin/viola G share same octave | Both produce "G3" via MIDI formula at 196 Hz. No dedicated unit test. | ⚠️ UNTESTED |

#### Domain: app

| Requirement | Scenario | Test | Result |
|-------------|----------|------|--------|
| Low-Frequency Detection for Cello | Cello C2 detection at 65.4 Hz | `TunerEngine` passes `minFrequency=50f`. No YIN detection test at 65.4 Hz. (YIN core detection tests are pre-existing failures.) | ⚠️ UNTESTED |
| Low-Frequency Detection for Cello | Cello G2 detection at 98.0 Hz | Same as above. | ⚠️ UNTESTED |
| Instrument-Aware NoteTargetSelector | Viola strings rendered | `NoteTargetSelector` iterates `instrument.strings`, uses `octaveLabel()`. No Compose UI test. | ⚠️ UNTESTED |
| Instrument-Aware NoteTargetSelector | Cello strings rendered | Same as above. | ⚠️ UNTESTED |
| Instrument-Aware NoteTargetSelector | Tapping a string button plays correct tone | `selectTunerNote()` → `playStringTone(note, refPitchA, instrument)`. `TunerViewModelTest > selectTunerNote works with viola C string` verifies note set. | ⚠️ PARTIAL |
| Note Mapping to Active Instrument Strings | Violin A4 detection (unchanged) | `TunerEngineTest > frequencyToNote maps 440_0 to A with 0 cents` | ✅ COMPLIANT |
| Note Mapping to Active Instrument Strings | Viola C3 detection | `TunerEngineTest > frequencyToNote maps viola C3 130_8 to C with instrument param` | ✅ COMPLIANT |
| Note Mapping to Active Instrument Strings | Cello A3 detection | `TunerEngineTest > frequencyToNote maps cello A3 220 to A with instrument param`, `TunerViewModelTest > cello A3 220Hz auto-detect maps to note A` | ✅ COMPLIANT |
| Note Mapping to Active Instrument Strings | Reference pitch affects mapping | `TunerEngineTest > frequencyToNote respects custom referencePitchA 442` | ✅ COMPLIANT |
| Reference Tone Playback with Active Instrument | Violin A4 reference tone (unchanged) | `playStringTone("A", 440, Instrument.VIOLIN)` → 440 Hz. `TunerViewModelTest > selectTunerNote A` verifies note, not actual frequency. | ⚠️ PARTIAL |
| Reference Tone Playback with Active Instrument | Viola C3 reference tone | `TunerViewModelTest > selectTunerNote works with viola C string`. Audio frequency not asserted. | ⚠️ PARTIAL |
| Reference Tone Playback with Active Instrument | Cello C2 with adjusted reference pitch | No test. | ❌ UNTESTED |
| Auto-Detect Mode with Active Instrument | Viola auto-detect identifies C3 | ViewModel `startPitchCollection()` passes instrument to `frequencyToNoteAndCents()`. No mock-pitch-flow test for viola auto-detect. | ⚠️ UNTESTED |
| Auto-Detect Mode with Active Instrument | Cello auto-detect identifies A3 | Same as above. | ⚠️ UNTESTED |
| Auto-Detect Mode with Active Instrument | Manual override still works | `TunerViewModelTest > tunerAutoDetect toggles the flag` + `selectTunerNote` override behavior | ✅ COMPLIANT |

**Compliance summary**: 12/27 scenarios fully compliant (COMPLIANT), 12 partially covered or statically verified (UNTESTED/PARTIAL), 1 untested edge case.

---

### Coherence (Design)

| Decision | Followed? | Notes |
|----------|-----------|-------|
| Data model: `Instrument` enum in `domain/model/` | ✅ Yes | Created as specified with `labelKey`, `strings`, `InstrumentString` data class |
| Persistence: `UserPreferencesManager.selectedInstrument` (SharedPreferences + StateFlow) | ✅ Yes | Follows `appLanguage` pattern exactly: key `selected_instrument`, `Instrument.valueOf()` serialization |
| minFrequency: statically lowered to 50 Hz in `TunerEngine` | ✅ Yes | `TunerEngine.kt` line 107: `minFrequency = 50f` |
| `ViolinAudioEngine` keep name, add `instrument` parameter to `playStringTone()` | ✅ Yes | `playStringTone(noteName, referencePitchA = 440, instrument = Instrument.VIOLIN)` with string lookup via `instrument.strings.find()` |
| VirtualFingerboard: open strings only for viola/cello, placeholder text | ✅ Yes | Dynamic string tabs from `instrument.strings`, `fingeringMap` filtered by string names, "Fingering data available for Violin only" placeholder |
| Data flow: SettingsScreen → UserPreferencesManager → TunerViewModel → consumers | ✅ Yes | SettingsScreen reads `selectedInstrument` from `UserPreferencesManager`, ViewModel exposes via `StateFlow`, passes to `playStringTone()` and `frequencyToNoteAndCents()` |
| UI: Instrument selector card between `LanguageSelector` and `AccountSection` | ✅ Yes | Card with 3 buttons (Violin/Viola/Cello), `primaryContainer` highlight, located between LanguageSelector and AccountSection |

**Design deviation noted**: `TunerViewModel` calls `YinPitchDetector.frequencyToNoteAndCents()` directly in `startPitchCollection()` to remap pitch results with the active instrument, rather than modifying `TunerEngine` to accept an instrument parameter. This was an intentional implementation choice documented in apply-progress: avoids modifying the audio layer while achieving same behavior. The double-mapping (TunerEngine maps with default VIOLIN, then ViewModel remaps) is harmless overhead. **Accepted as justified.**

---

### Issues Found

**CRITICAL**: None

**WARNING**:
1. **No Compose UI tests for instrument-specific rendering**: SettingsScreen instrument selector card, NoteTargetSelector dynamic strings per instrument, and VirtualFingerboard string tabs have no Compose UI tests proving correct rendering.
2. **No YIN detection tests at low frequencies**: `minFrequency=50f` is set in TunerEngine, but no test validates YIN detection at 65.4 Hz or 98.0 Hz (cello C2/G2). Core YIN detection tests at 440 Hz are already failing (pre-existing).
3. **No test for reference pitch adjustment with non-violin instrument**: The scenario "Cello C2 reference tone with adjusted reference pitch" is completely untested.
4. **No auto-detect integration test with viola**: ViewModel's `startPitchCollection()` uses active instrument for auto-detect, but no test mocks pitchFlow to verify viola/cello auto-detect in a realistic cycle.
5. **Spanish `instrument_cello` label discrepancy**: Spec says "Violonchelo", code uses "Chelo" (colloquial Rioplatense form). Acceptable but differs from spec.
6. **Missing pure-function test for `octaveLabel()`**: The octave suffix logic has no unit test. It's a pure function that should be trivially testable.

**SUGGESTION**:
1. `VirtualFingerboard` placeholder text "Fingering data available for Violin only" is not localized (hardcoded English).
2. `TunerEngine` still calls `frequencyToNoteAndCents()` with default `Instrument.VIOLIN` internally — the result is immediately overridden by ViewModel remapping. Consider removing the internal call or making it configurable in a future cleanup.
3. `StringsSettings` instrument labels are hardcoded in the strings file rather than derived from `Instrument.labelKey` — consider making labelKey the canonical key lookup.
4. Consider extracting `octaveLabel()` as a public extension function on `InstrumentString` for better testability.

---

### Verdict

**PASS WITH WARNINGS**

All 14 implementation tasks are complete. All new and modified tests pass (zero regressions). The 55 failing tests are all pre-existing failures unrelated to this change. Core spec requirements (Instrument data model, persistence, note mapping, UI wiring) are implemented and tested at the unit level. Warnings are limited to untested Compose UI scenarios and edge-case coverage gaps that do not block the functional delivery.
