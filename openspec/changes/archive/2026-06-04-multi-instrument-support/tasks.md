# Tasks: Multi-Instrument Support

## Review Workload Forecast

| Field | Value |
|-------|-------|
| Estimated changed lines | ~500 (210 + 130 + 160) |
| 400-line budget risk | High |
| Chained PRs recommended | Yes |
| Delivery strategy | auto-chain |
| Chain strategy | feature-branch-chain |

Decision needed before apply: No
Chained PRs recommended: Yes
Chain strategy: feature-branch-chain
400-line budget risk: High

### Suggested Work Units

| Unit | Goal | PR | Base | Lines |
|------|------|-----|------|-------|
| 1 | Instrument model + persistence + strings | PR 1 | `feature/multi-instrument-support` | ~210 |
| 2 | Pitch detection + audio engine adaptation | PR 2 | PR 1 branch | ~130 |
| 3 | ViewModel wiring + UI + Settings card | PR 3 | PR 2 branch | ~160 |

---

## Phase 1: Foundation (PR 1)

- [x] 1.1 **RED**: Create `domain/model/InstrumentTest.kt` — test enum values, `InstrumentString` lists per instrument, `valueOf()` roundtrip
  > Spec: instrument-selection — Instrument Data Model
- [x] 1.2 **GREEN**: Create `domain/model/Instrument.kt` — enum VIOLIN/VIOLA/CELLO with `labelKey: String`, `strings: List<InstrumentString>`, data class `InstrumentString(name: String, frequency: Double)`
  > Spec: instrument-selection — Instrument Data Model
- [x] 1.3 **RED**: Add tests to `di/UserPreferencesManagerTest.kt` — default VIOLIN on first launch, set/persist CELLO roundtrip across new instance, `selectedInstrument` StateFlow emits correctly
  > Spec: instrument-selection — Persistent Instrument Selection
- [x] 1.4 **GREEN**: Add `selectedInstrument: StateFlow<Instrument>` and `setSelectedInstrument(instrument)` to `di/UserPreferencesManager.kt` — key `selected_instrument`, serialized via `Instrument.name`, follows `appLanguage` pattern
  > Spec: instrument-selection — Persistent Instrument Selection
- [x] 1.5 Add keys to `ui/theme/StringsSettings.kt`: `instrument_label`, `instrument_violin`, `instrument_viola`, `instrument_cello` (EN + ES maps)
  > Spec: instrument-selection — Instrument Selector in Settings (localized labels)

## Phase 2: Audio Core (PR 2)

- [x] 2.1 **RED**: Add viola/cello tests to `YinPitchDetectorTest.kt` — VIOLA C3(130.8Hz)→"C", CELLO C2(65.4Hz)→"C", CELLO A3(220Hz)→"A", VIOLIN D4(293.7Hz)→"D" unchanged
  > Spec: app — Note Mapping to Active Instrument Strings
- [x] 2.2 **GREEN**: Add `instrument: Instrument = VIOLIN` param to `YinPitchDetector.frequencyToNoteAndCents()` — iterate `instrument.strings`, remove `VIOLIN_NOTE_RATIOS`, update KDoc
  > Spec: app — Note Mapping to Active Instrument Strings
- [x] 2.3 In `audio/TunerEngine.kt` line 103, pass `minFrequency = 50f` to `detectPitch()` for cello C2 (65.4Hz)
  > Spec: app — Low-Frequency Detection for Cello
- [x] 2.4 Add `instrument: Instrument = VIOLIN` param to `ViolinAudioEngine.playStringTone()` — resolve frequency ratio from `instrument.strings` instead of hardcoded `when` block
  > Spec: app — Reference Tone Playback with Active Instrument
- [x] 2.5 Update `audio/PitchResult.kt` KDoc: "violin string note" → "instrument string note"
  > Spec: app MODIFIED — Note Mapping

## Phase 3: Integration & UI (PR 3)

- [x] 3.1 **RED**: Add instrument switching tests to `TunerViewModelTest.kt` — switch to viola updates `selectedInstrument`, `selectTunerNote` passes instrument to engine, auto-detect works for cello A3
  > Spec: app — Auto-Detect Mode with Active Instrument
- [x] 3.2 **GREEN**: Inject `UserPreferencesManager` into `TunerViewModel` — expose `selectedInstrument` StateFlow, pass instrument to `playStringTone()` and `frequencyToNoteAndCents()`. Update test constructor call
  > Spec: instrument-selection — Persistent Instrument Selection
- [x] 3.3 Add `instrument: Instrument` param to `NoteTargetSelector` — render buttons from `instrument.strings`, apply octave suffix (G3/G2) for disambiguation where note names overlap
  > Spec: app — Instrument-Aware NoteTargetSelector
- [x] 3.4 Add `instrument: Instrument` param to `VirtualFingerboard` — filter `fingeringMap` by instrument string names; show "Fingering data available for Violin only" placeholder for viola/cello
  > Spec: proposal (out of scope guard: open strings only, full fingering deferred)
- [x] 3.5 Insert instrument selector card in `SettingsScreen` between `LanguageSelector` and `AccountSection` — 3-button Row, selected highlighted with primary color, tap calls `userPreferencesManager.setSelectedInstrument()`, labels via `Localization.get()`
  > Spec: instrument-selection — Instrument Selector in Settings
