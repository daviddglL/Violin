# Exploration: Multi-Instrument Support (Viola + Cello)

## Current State

The Violin Master app is **deeply coupled to violin as the sole instrument** across four layers:

### Layer 1: Pitch Detection (root cause)
`audio/tuner/YinPitchDetector.kt` — lines 21-26 hardcode a `VIOLIN_NOTE_RATIOS` map:

```kotlin
private val VIOLIN_NOTE_RATIOS = mapOf(
    "G" to 196.00 / 440.00,  // G3
    "D" to 293.66 / 440.00,  // D4
    "A" to 1.0,              // A4
    "E" to 659.25 / 440.00   // E5
)
```

The `frequencyToNoteAndCents()` method at line 132 iterates ONLY this map. Any detected frequency will always be mapped to G, D, A, or E — there is no mechanism to recognize C, G, D, A (viola) or C, G, D, A (cello, one octave down).

### Layer 2: Audio Playback
`audio/ViolinAudioEngine.kt` — `playStringTone()` (line 147-158) hardcodes violin frequencies:

```kotlin
val ratioToA = when (noteName.uppercase()) {
    "G" -> 196.0 / 440.0
    "D" -> 293.66 / 440.0
    "A" -> 1.0
    "E" -> 659.25 / 440.0
    else -> 1.0
}
```

The class name itself (`ViolinAudioEngine`) signals single-instrument coupling. It contains both string-tone playback AND metronome functionality. The metronome is instrument-agnostic; only `playStringTone()` is violin-specific.

### Layer 3: UI Components
- **`NoteTargetSelector.kt`** (line 39-44): Hardcodes G/D/A/E with frequencies.
- **`VirtualFingerboard.kt`** (line 52-85): Full fingering map with violin-specific descriptions like "Base note G of the violin wood".
- **`PitchDisplay.kt`** (line 70): Displays the selected note letter only — neutral by itself but fed by violin-only data.
- **`TunerViewModel.kt`** (line 157): Comment says "violin string". References `ViolinAudioEngine` directly.

### Layer 4: User-Facing Strings
All in `ui/theme/` string objects. Hardcoded "violin" references:

| Key | English text | File |
|-----|-------------|------|
| `app_name` | "Violin Master Pro" | StringsHome.kt |
| `app_title_auth` | "VIOLIN STUDIO PRO" | StringsSettings.kt |
| `violin_tips_title` | "VIOLIN METICULOUS PRACTICE TIPS" | StringsSettings.kt |
| `violin_tips_text` | Full violin-specific tip text | StringsSettings.kt |
| `login_required` | "Violin Studio Authentication" | StringsAuth.kt |
| `lessons_header_freelancer` | "Violin Curriculum" | StringsAuth.kt |
| `select_current_string` | "SELECT CURRENT VIOLIN STRING:" | StringsLessons.kt |
| `listen_and_match` | References "violin" | StringsLessons.kt |
| `encouragement_virtuoso` | "acoustic violin" | StringsLessons.kt |
| **Hardcoded in code** | `"VIOLIN STUDIO PRO"` | MainActivity.kt line 125 |

### What is Instrument-Agnostic (no changes needed)
- **TunerEngine.kt**: Pure microphone capture and YIN detection loop — works with any frequency.
- **TuningWheel.kt**: Gauge rendering — driven by cents offset, not note names.
- **TuningConfiguration.kt**: Stores reference pitch and max cents — no instrument concept.
- **TuningPreferencesManager.kt**: Preset CRUD — instrument-independent.
- **Metronome** (inside ViolinAudioEngine): BPM/beat logic — instrument-independent.
- **Theme**: Color.kt, Type.kt, Theme.kt contain zero "violin" references.
- **Data layer**: No Room entity has an instrument column. PracticeSession.category is free-form String.
- **SettingsScreen.kt**: Currently has no instrument section — ready for a new card.

---

## Affected Areas

| File | Impact | Why |
|------|--------|-----|
| `audio/tuner/YinPitchDetector.kt` | **HIGH** | Hardcoded `VIOLIN_NOTE_RATIOS` map. Must become instrument-aware. |
| `audio/ViolinAudioEngine.kt` | **HIGH** | Hardcoded string-to-frequency mapping in `playStringTone()`. Class name should stay but method must accept instrument parameter. |
| `ui/component/NoteTargetSelector.kt` | **HIGH** | Hardcodes violin strings G/D/A/E as a static list. Must render dynamically per instrument. |
| `ui/component/VirtualFingerboard.kt` | **MEDIUM** | `fingeringMap` is violin-only. Viola/cello have different fingerings. |
| `ui/viewmodel/TunerViewModel.kt` | **MEDIUM** | Needs to read active instrument and pass it to audio engine and pitch detector. |
| `di/UserPreferencesManager.kt` | **MEDIUM** | Must persist selected instrument. |
| `ui/screens/SettingsScreen.kt` | **MEDIUM** | Needs new instrument selector card. |
| `ui/screens/TunerScreen.kt` | **LOW** | Pass-through — receives note from ViewModel, no direct coupling. |
| `ui/theme/StringsSettings.kt` | **MEDIUM** | Replace "violin" in tips with generic strings, or add instrument-specific tips. |
| `ui/theme/StringsHome.kt` | **LOW** | `app_name` string. Branding question. |
| `ui/theme/StringsAuth.kt` | **LOW** | "Violin Studio" and "Violin Curriculum" branding strings. |
| `ui/theme/StringsLessons.kt` | **LOW** | "SELECT CURRENT VIOLIN STRING:" and "violin" text references. |
| `MainActivity.kt` | **LOW** | Hardcoded "VIOLIN STUDIO PRO" in top bar. |
| `audio/PitchResult.kt` | **LOW** | KDoc mentions "violin string note" — update to generic. |
| `di/AudioModule.kt` | **NONE** | No changes needed; injection is constructor-based. |
| `app/src/test/**` | **HIGH** | `YinPitchDetectorTest`, `TunerViewModelTest`, `TuningPreferencesManagerTest` need new instrument scenarios. |

---

## Approaches

### Approach 1: Instrument Enum + Dynamic String Maps (Recommended)

Add an `Instrument` enum with VIOLIN, VIOLA, CELLO. Each carries its string definitions (names + frequency ratios). The enum is stored in `UserPreferencesManager`. All consumers (YinPitchDetector, AudioEngine, NoteTargetSelector, VirtualFingerboard) read the current instrument and use its string map.

**String definitions:**

| Instrument | String 1 | String 2 | String 3 | String 4 |
|-----------|----------|----------|----------|----------|
| Violin    | G3 (196) | D4 (293.7) | A4 (440) | E5 (659.3) |
| Viola     | C3 (130.8) | G3 (196) | D4 (293.7) | A4 (440) |
| Cello     | C2 (65.4) | G2 (98) | D3 (146.8) | A3 (220) |

- **Pros**: Minimal structural change. Single source of truth per instrument. The audio processing pipeline (TunerEngine, YIN algorithm) stays untouched — only the note-mapping step changes. Easy to add more instruments later. Settings UI is a simple selector.
- **Cons**: `VirtualFingerboard` fingering map needs per-instrument data (viola/cello have different finger positions). String rebranding (`app_name` etc.) requires product decision. The `ViolinAudioEngine` class name becomes misleading but renaming it would touch 5+ files.
- **Effort**: Medium (~15 files changed, ~5 new test cases)

### Approach 2: Separate Audio Engines per Instrument

Create `ViolaAudioEngine` and `CelloAudioEngine` alongside `ViolinAudioEngine`, all implementing an `AudioEngine` interface. Each engine encapsulates its own string definitions and playback logic.

- **Pros**: Clean separation. Each instrument is self-contained. No conditional logic in a single engine class.
- **Cons**: Massive code duplication (metronome logic copy-pasted 3x). DI wiring complexity (Hilt qualifiers needed). `TunerViewModel` must switch engines at runtime. Over-engineering for what's essentially different lookup tables.
- **Effort**: High (~25+ files, significant test duplication)

### Approach 3: Instrument as TunerViewModel-only State

Store instrument selection ONLY in TunerViewModel state (not persisted). TunerViewModel passes the instrument to `YinPitchDetector.frequencyToNoteAndCents()` and `ViolinAudioEngine.playStringTone()`.

- **Pros**: Simplest code change. No persistence layer changes.
- **Cons**: Resets to violin on every app restart. Users would need to re-select every session. Violates user expectation. The Settings UI still needs the selector anyway.
- **Effort**: Low (~8 files) but poor UX

---

## Recommendation

**Approach 1: Instrument Enum + Dynamic String Maps**

Rationale:
1. The pitch detection pipeline (`TunerEngine`, YIN algorithm, `detectPitch()`) is already instrument-agnostic — it just detects frequency. Only `frequencyToNoteAndCents()` needs the instrument's string map.
2. The audio playback engine (`ViolinAudioEngine.playStringTone()`) just needs to know which frequencies to synthesize — the same waveform generation works for all instruments.
3. Persisting the choice in `UserPreferencesManager` (SharedPreferences) is a 20-line addition and follows the existing pattern used for `appLanguage`.
4. Adding a card to `SettingsScreen` follows the existing pattern of `LanguageSelector`, `SecuritySection`, etc.
5. The `Instrument` enum can carry ALL instrument-specific data (string names, ratios, display order), making it a single extension point for future instruments (double bass, etc.).

---

## Risks

1. **Fingering Map Complexity**: Viola and cello have different finger positions than violin. The `VirtualFingerboard` `fingeringMap` needs complete per-instrument data. Could be scoped as a follow-up change or implemented with basic open-string data initially.
2. **String Label Overlap**: Both violin and viola have "G", "D", "A" strings. The UI must distinguish them — possibly with octave indicators (G3 vs G2) or instrument prefix. The `PitchResult.note` field is just the letter, which may be ambiguous.
3. **Branding Decision**: Changing "Violin Master Pro" to a more inclusive name is a product/business decision, not a technical one. The exploration covers the technical scope; the branding question should be raised in the proposal.
4. **Cello Frequency Range**: Cello's lowest string (C2 ≈ 65.4 Hz) is below the YIN detector's default `minFrequency = 80 Hz`. The `detectPitch()` call in `TunerEngine` would need `minFrequency` lowered to ~50 Hz for cello to work. This is a one-line parameter change.
5. **Test Coverage**: `YinPitchDetectorTest` has 8 tests, all violin-only. Adding viola/cello scenarios requires ~5 new parameterized test cases.

---

## Ready for Proposal

**Yes** — the exploration is complete. The codebase has been thoroughly read. All hardcoded violin dependencies have been identified. A clear, minimal-change approach (Instrument enum + dynamic string maps) is recommended.

The orchestrator should proceed to **sdd-propose** with this exploration as context.
