# Video Playback Specification

## Purpose

Real ExoPlayer/Media3 video playback replacing the simulated SecureMediaPlaybackConsole (fake soundwave bars, fake decrypt logs). Passcode wall on MasterclassTab becomes optional for free-tier access.

## Requirements

### Requirement: Real Media Playback via ExoPlayer/Media3

The system MUST render actual MP4 video content using ExoPlayer/Media3 in both masterclass and assignment playback contexts. The simulated SecureMediaPlaybackConsole (soundwave animation, fake decrypt logs, SecurityLogItem) SHALL be replaced. Playback controls (play/pause/seek) MUST be present.

#### Scenario: Masterclass video plays real content

- GIVEN a valid video URL exists for a masterclass video
- WHEN user selects the video
- THEN real MP4 content plays with standard transport controls
- AND no simulated soundwave bars or "live_decrypting" logs appear

#### Scenario: Assignment video plays uploaded content

- GIVEN a student has received an assignment with a real video URL
- WHEN student taps play on the assignment video
- THEN the uploaded video content plays via ExoPlayer

#### Scenario: Invalid or missing video URL

- GIVEN the video URL is null, empty, or unreachable
- WHEN playback is attempted
- THEN an error state is displayed (not simulated content)
- AND the app does not crash

### Requirement: Optional Passcode Wall

The mandatory passcode authentication on MasterclassTab SHALL become optional. Users without a configured passcode MAY access masterclass content directly. Users with a configured passcode MUST still authenticate via PIN before accessing protected content.

#### Scenario: No passcode configured — direct access

- GIVEN no passcode has been set in the system
- WHEN user navigates to MasterclassTab
- THEN the masterclass content hub renders immediately without lock screen

#### Scenario: Passcode configured — authentication required

- GIVEN a passcode is already configured
- WHEN user navigates to MasterclassTab
- THEN the passcode lock screen appears
- AND only correct PIN grants access to content hub

#### Scenario: Existing passcode users unaffected

- GIVEN user has previously set a passcode
- WHEN they access masterclass after the update
- THEN existing passcode behavior is preserved (lock → authenticate → access)
