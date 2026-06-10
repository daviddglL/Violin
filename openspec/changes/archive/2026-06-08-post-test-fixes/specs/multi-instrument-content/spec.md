# Multi-Instrument Content Specification

## Purpose

Fingering maps and quiz question banks for viola, cello, and double bass instruments. Currently only violin fingering and quiz content exists; non-violin instruments show a "fingering_violin_only" placeholder.

## Requirements

### Requirement: Multi-Instrument Fingering Maps

VirtualFingerboard MUST render fingering position maps for all four instrument types: violin (G/D/A/E), viola (C/G/D/A), cello (C/G/D/A), and bass (E/A/D/G). Each map SHALL include correct note names, frequencies, and position descriptions. The "fingering_violin_only" placeholder SHALL be replaced with real content for each instrument.

#### Scenario: Viola fingering map

- GIVEN active instrument is Viola
- WHEN VirtualFingerboard renders
- THEN string tabs display C3(130.8Hz), G3(196.0Hz), D4(293.7Hz), A4(440.0Hz)
- AND each string shows 6 fingering positions with correct note names and frequencies

#### Scenario: Cello fingering map

- GIVEN active instrument is Cello
- WHEN VirtualFingerboard renders
- THEN string tabs display C2(65.4Hz), G2(98.0Hz), D3(146.8Hz), A3(220.0Hz)
- AND each string shows correct fingering positions

#### Scenario: Bass fingering map

- GIVEN active instrument is Bass
- WHEN VirtualFingerboard renders
- THEN string tabs display E1(41.2Hz), A1(55.0Hz), D2(73.4Hz), G2(98.0Hz)

#### Scenario: Instrument switch updates map

- GIVEN VirtualFingerboard is showing Viola map
- WHEN user switches instrument to Cello in Settings and returns
- THEN the fingerboard immediately shows Cello string tabs and positions

### Requirement: Instrument-Specific Quiz Banks

TheoryQuizTab MUST load instrument-specific quiz question banks. The quiz SHALL display questions relevant to the selected instrument. Switching instruments mid-session SHALL reset the quiz with the new instrument's question bank.

#### Scenario: Viola quiz questions

- GIVEN selected instrument is Viola
- WHEN TheoryQuizTab opens
- THEN questions reference viola-specific tuning, fingerings, and technique

#### Scenario: Instrument change resets quiz

- GIVEN quiz is in progress with 3 questions answered for Violin
- WHEN user switches to Cello
- THEN quiz resets to question 1 with cello-specific questions

#### Scenario: Bass quiz bank

- GIVEN selected instrument is Bass (DOUBLE_BASS)
- WHEN TheoryQuizTab opens
- THEN quiz questions reference bass-specific tuning (E/A/D/G) and technique
