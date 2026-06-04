# Proposal: Multi-Instrument Support (Violin + Viola + Cello)

## Intent

The app currently hardcodes violin as the only instrument across all layers: pitch detection, audio playback, UI note selection, and user-facing strings. Users need to switch between violin, viola, and cello so the tuner maps detected frequencies to the correct instrument strings.

## Scope

### In Scope
- `Instrument` enum (VIOLIN, VIOLA, CELLO) with per-instrument string definitions (names + frequency ratios)
- Instrument selector in Settings screen
- Persist selected instrument via `UserPreferencesManager` (SharedPreferences)
- `YinPitchDetector` reads active instrument to map frequencies to correct notes
- `ViolinAudioEngine.playStringTone()` uses active instrument for reference tones
- `NoteTargetSelector` renders instrument-specific string buttons
- Lower YIN `minFrequency` from 80 Hz → 50 Hz for cello C2 (65.4 Hz)
- Unit tests for viola/cello pitch detection and ViewModel instrument switching

### Out of Scope
- Per-instrument VirtualFingerboard fingering maps (open strings only; viola/cello finger positions deferred)
- Rebranding "Violin Master Pro" → generic name (product decision, not engineering)
- Instrument-specific practice tips or lesson content
- Instrument-specific tuning presets (presets remain instrument-agnostic)

## Capabilities

### New Capabilities
- `instrument-selection`: User selects violin/viola/cello; tuner adapts note mapping, reference tones, and UI per instrument.

### Modified Capabilities
- `app` (SPEC-3, REQ-TN-003): Note mapping expands from "violin strings only" to "selected instrument's strings."
- `tuner-configuration` (SPEC-TN-CFG): `TuningWheel` and config sheet remain instrument-agnostic; no spec-level change required.

## Approach

**Instrument Enum + Dynamic String Maps** (recommended from exploration).

Single `Instrument` enum carries all per-instrument data (string labels, frequency ratios, display order). Stored in `UserPreferencesManager`. All consumers read the active instrument at runtime — no new audio engines, no DI changes beyond the preferences manager.

## Affected Areas

| Area | Impact | Change |
|------|--------|--------|
| `audio/tuner/YinPitchDetector.kt` | Modified | Replace `VIOLIN_NOTE_RATIOS` with instrument-aware lookup |
| `audio/ViolinAudioEngine.kt` | Modified | `playStringTone()` accepts instrument parameter |
| `audio/TunerEngine.kt` | Modified | `minFrequency` lowered to 50 Hz |
| `ui/component/NoteTargetSelector.kt` | Modified | Render strings dynamically per instrument |
| `ui/viewmodel/TunerViewModel.kt` | Modified | Read instrument from preferences; pass to engine/detector |
| `di/UserPreferencesManager.kt` | Modified | Add `selectedInstrument` persistence |
| `ui/screens/SettingsScreen.kt` | Modified | New instrument selector card |
| `model/Instrument.kt` | **New** | Enum with string definitions |
| `app/src/test/**` | Modified | New test cases for viola/cello |

## Risks

| Risk | Likelihood | Mitigation |
|------|------------|------------|
| Cello C2 (65.4 Hz) below YIN min 80 Hz | High | Lower `minFrequency` to 50 Hz — one-line change with test |
| String label overlap (violin/viola share G, D, A) | Medium | Append octave indicator (G3, D4) in display |
| YIN accuracy degradation at low frequencies | Low | Validate detection within ±2 cents at 65.4 Hz in tests |
| VirtualFingerboard fingering data incomplete | Low | Scoped out — display open strings only for non-violin instruments |

## Rollback Plan

1. Set `selectedInstrument` default to VIOLIN — all behavior unchanged for violin users.
2. If rollback needed: revert `YinPitchDetector` to hardcoded `VIOLIN_NOTE_RATIOS`, remove Settings card, delete `Instrument.kt`.
3. No DB schema change, no migration — pure SharedPreferences + code.

## Dependencies

- None (no external API, no library, no other SDD change)

## Success Criteria

- [ ] User can select violin, viola, or cello in Settings; choice persists across restarts
- [ ] Tuner maps G3(196 Hz) → "G" for violin, but C3(130.8 Hz) → "C" for viola
- [ ] Reference tone buttons show correct strings per instrument (C-G-D-A for viola/cello)
- [ ] Cello C2 detection works at minFrequency=50 Hz
- [ ] All existing violin-only tests pass unchanged
- [ ] New tests: `YinPitchDetectorTest` with viola/cello scenarios, `TunerViewModelTest` with instrument switching
