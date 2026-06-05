# Delta Spec: Multi-Instrument Support for App Domain

## Purpose

Extend SPEC-3 tuner behavior from violin-only note mapping and reference tones to support violin, viola, and cello via the active instrument from `instrument-selection`.

## ADDED Requirements

### Requirement: Low-Frequency Detection for Cello

The YIN pitch detector minimum frequency MUST be 50 Hz (lowered from 80 Hz) to cover cello C2 at 65.4 Hz. Detection accuracy SHALL remain within ±2 cents for all cello open strings.

#### Scenario: Cello C2 detection at 65.4 Hz

- GIVEN a 65.4 Hz sine wave input and minFrequency=50 Hz
- WHEN pitch detection runs
- THEN detected frequency is between 65.15 Hz and 65.65 Hz (±2 cents)

#### Scenario: Cello G2 detection at 98.0 Hz

- GIVEN a 98.0 Hz sine wave input and minFrequency=50 Hz
- WHEN pitch detection runs
- THEN detected frequency is within ±2 cents of 98.0 Hz

### Requirement: Instrument-Aware NoteTargetSelector

The `NoteTargetSelector` composable MUST render string buttons dynamically from the active instrument's `strings` list. Button labels SHALL include octave suffix where needed for disambiguation. Tapping a button SHALL call `TunerViewModel.selectTunerNote()` with the correct frequency.

#### Scenario: Viola strings rendered

- GIVEN active instrument is Viola
- WHEN NoteTargetSelector renders
- THEN four buttons appear: C3, G3, D4, A4 with frequencies 130.8, 196.0, 293.7, 440.0 Hz

#### Scenario: Cello strings rendered

- GIVEN active instrument is Cello
- WHEN NoteTargetSelector renders
- THEN four buttons appear: C2, G2, D3, A3 with frequencies 65.4, 98.0, 146.8, 220.0 Hz

#### Scenario: Tapping a string button plays correct tone

- GIVEN active instrument is Viola and user taps the C3 button
- WHEN `selectTunerNote` is called
- THEN `ViolinAudioEngine.playStringTone()` plays a 130.8 Hz reference tone

## MODIFIED Requirements

### Requirement: Note Mapping to Active Instrument Strings

The detected frequency MUST map to the closest string note of the ACTIVE instrument (violin, viola, or cello), not exclusively violin strings. The `YinPitchDetector.frequencyToNoteAndCents()` function SHALL accept an `instrument: Instrument` parameter and iterate `instrument.strings` instead of hardcoded `VIOLIN_NOTE_RATIOS`. Mapping SHALL respect the current reference pitch A value (default 440 Hz, adjustable).
(Previously: Note mapping was hardcoded to violin strings only — G3, D4, A4, E5.)

#### Scenario: Violin A4 detection (unchanged behavior)

- GIVEN detected frequency of 438 Hz and active instrument is Violin
- WHEN auto-detect is enabled
- THEN `tunerSelectedNote` emits "A" (closest to A4=440 Hz)

#### Scenario: Viola C3 detection

- GIVEN detected frequency of 130.0 Hz and active instrument is Viola
- WHEN auto-detect is enabled
- THEN `tunerSelectedNote` emits "C" (closest to C3=130.8 Hz)

#### Scenario: Cello A3 detection

- GIVEN detected frequency of 218 Hz and active instrument is Cello
- WHEN auto-detect is enabled
- THEN `tunerSelectedNote` emits "A" (closest to A3=220.0 Hz)

#### Scenario: Reference pitch affects mapping

- GIVEN reference pitch is set to 442 Hz and active instrument is Violin
- WHEN a frequency of 445 Hz is detected
- THEN note maps to A4 relative to 442 Hz AND offset is calculated from 442 Hz

### Requirement: Reference Tone Playback with Active Instrument

The reference tone playback MUST use the active instrument's string frequencies. `ViolinAudioEngine.playStringTone()` SHALL accept an `instrument: Instrument` parameter. Selecting a string note plays the correct frequency through `AudioTrack` using the current reference pitch and the instrument's string definitions.
(Previously: `playStringTone()` only played violin string frequencies; no instrument parameter existed.)

#### Scenario: Violin A4 reference tone (unchanged default)

- GIVEN active instrument is Violin and reference pitch is 440 Hz
- WHEN user taps the A string button
- THEN `playStringTone("A", 440.0, Instrument.VIOLIN)` plays a 440 Hz sine wave

#### Scenario: Viola C3 reference tone

- GIVEN active instrument is Viola and reference pitch is 440 Hz
- WHEN user taps the C string button
- THEN `playStringTone("C", 440.0, Instrument.VIOLA)` plays a 130.8 Hz sine wave

#### Scenario: Cello C2 reference tone with adjusted reference pitch

- GIVEN active instrument is Cello and reference pitch is set to 442 Hz
- WHEN user taps the C string button
- THEN playback frequency is scaled to 442 Hz reference (C2 ≈ 131.4 Hz at A=442)

### Requirement: Auto-Detect Mode with Active Instrument

Auto-detect mode MUST map detected frequencies against the active instrument's string set. When pitch is within 50 cents of any string, `tunerSelectedNote` SHALL update automatically. Manual selection overrides auto-detect.
(Previously: Auto-detect only matched against violin strings G3/D4/A4/E5.)

#### Scenario: Viola auto-detect identifies C3

- GIVEN auto-detect is ON and active instrument is Viola
- WHEN pitch detection identifies 130.0 Hz within 30 cents of C3
- THEN `tunerSelectedNote` updates to "C" automatically

#### Scenario: Cello auto-detect identifies A3

- GIVEN auto-detect is ON and active instrument is Cello
- WHEN pitch detection identifies 219 Hz within 30 cents of A3
- THEN `tunerSelectedNote` updates to "A" automatically

#### Scenario: Manual override still works

- GIVEN auto-detect is OFF and user manually selected "D" on Cello
- WHEN pitch detection identifies A3 (220 Hz)
- THEN `tunerSelectedNote` remains "D" (manual override)
