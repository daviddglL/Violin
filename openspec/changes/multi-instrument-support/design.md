# Design: Multi-Instrument Support

## Technical Approach

Add an `Instrument` enum carrying per-instrument string definitions (names, frequencies, display order). Persist selection via `UserPreferencesManager` (SharedPreferences, following the `appLanguage` pattern). Consumers (`YinPitchDetector`, `ViolinAudioEngine`, `NoteTargetSelector`, `TunerViewModel`) read the active instrument at runtime. No new engines, no DB migration, no DI module changes.

## Architecture Decisions

| Decision | Choice | Rejected | Rationale |
|----------|--------|----------|-----------|
| Data model | `Instrument` enum in `domain/model/` | Separate engines per instrument, ViewModel-only state | Enum is single source of truth, follows `TuningConfiguration` placement, trivial to extend |
| Persistence | `UserPreferencesManager.selectedInstrument` (SharedPreferences + StateFlow) | Room entity column, separate prefs file | Follows existing `appLanguage` pattern (StateFlow + putString); no schema migration needed |
| minFrequency | Statically lowered to 50 Hz in `TunerEngine` | Per-instrument parameter | Cello C2=65.4Hz needs ~50Hz floor; all three instruments covered; no runtime overhead |
| ViolinAudioEngine rename | Keep class name, add `instrument` parameter to `playStringTone()` | Rename to `AudioEngine` | Rename touches 5+ files, metronome logic is reusable, method parameter is minimal |
| VirtualFingerboard | Display open strings only for viola/cello; full fingering maps deferred | Full fingering maps in v1 | Fingering data for viola/cello is an entire follow-up work unit (20+ positions per instrument) |

## Data Model: Instrument Enum

```
enum class Instrument(val labelKey: String, val strings: List<InstrumentString>)
  VIOLIN -> strings: G3(196), D4(293.7), A4(440), E5(659.3)
  VIOLA  -> strings: C3(130.8), G3(196), D4(293.7), A4(440)
  CELLO  -> strings: C2(65.4), G2(98), D3(146.8), A3(220)

data class InstrumentString(val name: String, val frequency: Double)
```

`name` is the note letter only (G, D, A, E, C). Overlapping names (G/D/A on both violin and viola) are resolved at display time via octave suffix in the UI layer (G3 vs G2).

## Data Flow

```
SettingsScreen ──(select)─→ UserPreferencesManager.selectedInstrument (SharedPreferences + StateFlow)
                                    │
                    TunerViewModel ◄─┘ (collectAsState)
                      │         │
          ┌───────────┘         └───────────┐
          ▼                                 ▼
  YinPitchDetector                        ViolinAudioEngine
  .frequencyToNoteAndCents(freq,         .playStringTone(note, refPitch,
   refPitch, maxCents, instrument)        instrument)
          │                                 │
          ▼                                 ▼
  NoteTargetSelector ◄── instrument.strings
```

`TunerEngine` passes `minFrequency=50` when calling `detectPitch()` to cover cello C2.

## File Changes

| File | Action | Description |
|------|--------|-------------|
| `domain/model/Instrument.kt` | **Create** | Enum + InstrumentString data class |
| `audio/tuner/YinPitchDetector.kt` | Modify | Replace `VIOLIN_NOTE_RATIOS` with `instrument.strings` iteration in `frequencyToNoteAndCents()`; add `instrument: Instrument = Instrument.VIOLIN` parameter |
| `audio/ViolinAudioEngine.kt` | Modify | Add `instrument: Instrument = Instrument.VIOLIN` param to `playStringTone()`; use `instrument.strings` for ratio lookup |
| `audio/TunerEngine.kt` | Modify | Line 103: pass `minFrequency = 50f` to `detectPitch()` |
| `ui/component/NoteTargetSelector.kt` | Modify | Add `instrument: Instrument` param; render buttons from `instrument.strings` instead of hardcoded G/D/A/E/frequencies |
| `ui/component/VirtualFingerboard.kt` | Modify | Add `instrument: Instrument` param; filter `fingeringMap` by instrument string names; show placeholder for non-violin instruments |
| `ui/viewmodel/TunerViewModel.kt` | Modify | Inject `UserPreferencesManager`; read `selectedInstrument` StateFlow; pass to `playStringTone()`, `frequencyToNoteAndCents()` |
| `di/UserPreferencesManager.kt` | Modify | Add `selectedInstrument: StateFlow<Instrument>`, `setSelectedInstrument()`, persistence key `selected_instrument` |
| `ui/screens/SettingsScreen.kt` | Modify | Add instrument selector card (dropdown or row) after `LanguageSelector` |
| `ui/theme/StringsSettings.kt` | Modify | Add: `instrument_label`, `instrument_violin`, `instrument_viola`, `instrument_cello` keys (EN + ES) |
| `audio/PitchResult.kt` | Modify | KDoc: "violin string note" → "instrument string note" |

## Persistence

```
UserPreferencesManager:
  private const val KEY_SELECTED_INSTRUMENT = "selected_instrument"
  
  val selectedInstrument: StateFlow<Instrument>  // default Instrument.VIOLIN
  
  fun setSelectedInstrument(instrument: Instrument) {
    _selectedInstrument.value = instrument
    prefs.edit().putString(KEY_SELECTED_INSTRUMENT, instrument.name).apply()
  }
```

Follows the exact pattern of `appLanguage`: lazy init from prefs, StateFlow exposure, String-based serialization via `Instrument.valueOf()`.

## UI: Instrument Selector Card

Inserted in `SettingsScreen` between `LanguageSelector` and `AccountSection`. Three buttons in a `Row` (same pattern as `NoteTargetSelector` strings row), each with instrument icon placeholder text + localized label. Selected instrument has `primary` background. Tap calls `userPreferencesManager.setSelectedInstrument(instrument)`.

## Testing Strategy

| Layer | What | Approach |
|-------|------|----------|
| Unit — `InstrumentTest` | Enum values, string maps, `valueOf` roundtrip | JUnit, no Android deps |
| Unit — `YinPitchDetectorTest` | Viola C3(130.8) → "C", cello C2(65.4) → "C", shared note G → correct instrument | Add 3 parameterized tests with `Instrument.VIOLA`/`CELLO` |
| Unit — `TunerViewModelTest` | Instrument switching updates selected instrument, `selectTunerNote` passes correct instrument | Add 2 tests: switch instrument, verify note strings update |
| Unit — `UserPreferencesManagerTest` | Default VIOLIN, set/persist/roundtrip | Add 3 tests following existing `appLanguage` test pattern |
| Compose UI — `SettingsScreenTest` | Instrument selector renders, tap persists, label shows for each instrument | Robolectric + Compose UI Test |
| Compose UI — `NoteTargetSelector` | Renders C/G/D/A for viola, frequencies correct | Screenshot test with Roborazzi (per instrument) |

## Migration

- `selectedInstrument` defaults to `Instrument.VIOLIN` — all existing users (violin-only) see zero change.
- No Room schema migration (no new entity columns).
- No data conversion (SharedPreferences key is new, no old data to migrate).
- Rollback: delete `Instrument.kt`, revert `YinPitchDetector` to `VIOLIN_NOTE_RATIOS`, remove Settings card, delete prefs key. No user data loss.

## Open Questions

None — all technical decisions are resolved from codebase analysis and exploration findings.
