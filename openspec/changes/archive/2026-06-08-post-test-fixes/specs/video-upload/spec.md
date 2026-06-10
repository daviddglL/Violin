# Video Upload Specification

## Purpose

End-to-end teacher/student recording pipeline: record → face-blur (minors) → H.264 compress → Firebase upload → real download URL. Replaces the checkbox-only attachment pattern in AssignmentCreationForm and fake URL generation in PublishAssignmentUseCase.

## Requirements

### Requirement: Teacher Recording and Upload Pipeline

The assignment creation flow MUST allow teachers to record video directly from AssignmentCreationForm. The pipeline SHALL follow: CameraX recording → H.264 compression via MediaCodec → Firebase Storage upload → real download URL stored in Assignment. The current checkbox-only pattern (attachVideo flag) and fake URL generation via VideoSecurityService.obtainSecureSignedUrl() SHALL be replaced.

#### Scenario: Teacher records and publishes video

- GIVEN teacher is on AssignmentCreationForm
- WHEN teacher triggers video recording
- THEN CameraX recording UI opens
- AND on stop: compression → upload → Done state
- AND the real Firebase download URL is stored in the Assignment entity

#### Scenario: Teacher cancels recording

- GIVEN recording is in progress
- WHEN teacher cancels
- THEN recording stops, temp files are deleted, state returns to Idle

#### Scenario: Upload fails

- GIVEN network is unavailable during upload phase
- WHEN upload is attempted
- THEN an Error state with descriptive message is emitted
- AND the app does not crash

### Requirement: Student Face Blur for Minor Protection

Student recording pipeline MUST apply face blur BEFORE compression for users marked as minors. Non-minor users SHALL skip the blur phase entirely. If blur processing fails, the system SHALL fall back to the original unblurred video and display a warning.

#### Scenario: Minor student — blur applied

- GIVEN authenticated user is marked as minor
- WHEN recording is stopped
- THEN face blur phase runs before compression
- AND the uploaded video has faces blurred

#### Scenario: Non-minor student — blur skipped

- GIVEN authenticated user is not a minor
- WHEN recording is stopped
- THEN the blur phase is skipped entirely
- AND compression begins immediately on raw video

#### Scenario: Blur fails — fallback with warning

- GIVEN face blur processing throws an exception
- WHEN blur is attempted for a minor
- THEN the original video is used for compression
- AND a blurWarning message is emitted to the UI

### Requirement: VideoUploadViewModel DI Wiring

VideoUploadViewModel SHALL be injectable via Hilt with all service dependencies (VideoRecordingService, FaceBlurProcessor, VideoCompressionService, VideoUploadService). It MUST be passed as a non-null parameter to TeacherDashboardTab and StudentAssignmentsTab, replacing the current nullable default.

#### Scenario: Hilt provides VideoUploadViewModel

- GIVEN the Hilt component graph is initialized
- WHEN TeacherDashboardTab requests VideoUploadViewModel
- THEN a fully-injected instance with all services is provided

#### Scenario: Recording button replaces checkbox

- GIVEN teacher is viewing AssignmentCreationForm
- WHEN the form renders
- THEN a recording trigger button is present instead of the checkbox-only attachVideo toggle
