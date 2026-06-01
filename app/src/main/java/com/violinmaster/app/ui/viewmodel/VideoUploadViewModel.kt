package com.violinmaster.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.violinmaster.app.data.firebase.Message
import com.violinmaster.app.di.AuthManager
import com.violinmaster.app.service.FaceBlurProcessor
import com.violinmaster.app.service.VideoCompressionService
import com.violinmaster.app.service.VideoRecordingService
import com.violinmaster.app.service.VideoUploadService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

/**
 * ViewModel orchestrating the video recording → compression → upload pipeline.
 *
 * REQ-VID-001 through REQ-VID-008.
 *
 * State machine: IDLE → RECORDING → COMPRESSING → UPLOADING → DONE
 * Any state can transition to ERROR. User can cancel back to IDLE.
 *
 * Threading: CameraX on Main; compression on Dispatchers.Default; upload on Dispatchers.IO.
 *
 * @param recordingService Wraps CameraX for recording.
 * @param faceBlurProcessor ML Kit-based face blur for minor students.
 * @param compressionService MediaCodec-based H.264 compression.
 * @param uploadService Firebase Storage upload + download URL retrieval.
 * @param authManager Provides current user identity and minor status.
 */
@HiltViewModel
class VideoUploadViewModel @Inject constructor(
    private val recordingService: VideoRecordingService,
    private val faceBlurProcessor: FaceBlurProcessor,
    private val compressionService: VideoCompressionService,
    private val uploadService: VideoUploadService,
    internal val authManager: AuthManager
) : ViewModel() {

    /**
     * Sealed class representing the complete video upload pipeline state.
     */
    sealed class UploadState {
        /** No recording in progress. Initial and reset state. */
        data object Idle : UploadState()

        /** Camera is recording. [elapsedSeconds] ticks up each second. */
        data class Recording(val elapsedSeconds: Int = 0) : UploadState()

        /**
         * Face blur is being applied (minor students only).
         * [progress] is a human-readable description like "Processing frame 45/1080".
         * REQ-BLR-005, REQ-BLR-006.
         */
        data class Blurring(val progress: String = "") : UploadState()

        /** Video is being compressed. [progress] ranges 0.0–1.0. */
        data class Compressing(val progress: Float = 0f) : UploadState()

        /** Compressed video is uploading to Firebase Storage. [progress] 0.0–1.0. */
        data class Uploading(val progress: Float = 0f) : UploadState()

        /** Upload complete. [videoUrl] is the Firebase Storage download URL. */
        data class Done(val videoUrl: String) : UploadState()

        /** Pipeline error. [message] describes what went wrong. */
        data class Error(val message: String) : UploadState()
    }

    private val _uploadState = MutableStateFlow<UploadState>(UploadState.Idle)
    val uploadState: StateFlow<UploadState> = _uploadState.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    /** Warning message when blur was skipped or failed but video was still sent. */
    private val _blurWarning = MutableStateFlow<String?>(null)
    val blurWarning: StateFlow<String?> = _blurWarning.asStateFlow()

    /** Observable transition history for testing state machine correctness. */
    private val _transitionHistory = mutableListOf<UploadState>()
    val transitionHistory: List<UploadState> get() = _transitionHistory.toList()

    private var currentRecordingFile: File? = null
    private var currentBlurredFile: File? = null
    private var currentCompressedFile: File? = null

    // ═══════════════════════════════════════════════════════════════════
    // Public API
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Starts video recording. Transitions state to [UploadState.Recording].
     *
     * Creates a temp file in cacheDir for the recording.
     */
    fun startRecording() {
        if (!recordingService.hasCameraPermission()) {
            _uploadState.value = UploadState.Error("Camera permission required")
            _transitionHistory.add(_uploadState.value)
            return
        }

        val outputFile = recordingService.createOutputFile()
        currentRecordingFile = outputFile

        recordingService.startRecording(outputFile) { errorMsg ->
            _uploadState.value = UploadState.Error(errorMsg)
            _transitionHistory.add(_uploadState.value)
        }

        _uploadState.value = UploadState.Recording(elapsedSeconds = 0)
        _transitionHistory.add(_uploadState.value)
    }

    /**
     * Stops recording and triggers the blur → compression → upload pipeline.
     *
     * Pipeline: stop CameraX → [blur if minor] → compress → upload → Done(url) / Error(msg).
     * REQ-BLR-002: Blur BEFORE compression.
     * REQ-BLR-006: Conditional blur — only for minor users.
     */
    fun stopRecording() {
        if (!recordingService.isRecording()) return

        val rawFile = try {
            recordingService.stopRecording()
        } catch (e: Exception) {
            _uploadState.value = UploadState.Error("Failed to stop recording: ${e.message}")
            _transitionHistory.add(_uploadState.value)
            return
        }

        currentRecordingFile = rawFile

        viewModelScope.launch {
            try {
                // ── Determine if blur is needed ────────────────────────
                val isMinor = authManager.isCurrentUserMinor

                // File that will be used for compression (blurred or original)
                var fileToCompress: File = rawFile

                if (isMinor) {
                    // ── Face Blur Phase (minor students only) ──────────
                    // REQ-BLR-002: Blur BEFORE compression
                    // REQ-BLR-006: Conditional pipeline gate
                    _uploadState.value = UploadState.Blurring(progress = "Protecting identity...")
                    _transitionHistory.add(_uploadState.value)

                    val blurredFile = File(rawFile.parent, "blurred_${System.currentTimeMillis()}.mp4")

                    try {
                        fileToCompress = faceBlurProcessor.processVideo(
                            inputFile = rawFile,
                            outputFile = blurredFile,
                            isMinor = true
                        ) { progressMsg ->
                            _uploadState.value = UploadState.Blurring(progress = progressMsg)
                            _transitionHistory.add(_uploadState.value)
                        }

                        // Check if blur was actually applied (different file = processed)
                        if (fileToCompress != rawFile && fileToCompress == blurredFile) {
                            currentBlurredFile = fileToCompress
                        }
                    } catch (e: Exception) {
                        // REQ-BLR-007: Blur failed — fall back to original
                        _blurWarning.value = "Face blur could not be applied. Video sent without blur."
                        fileToCompress = rawFile
                        _uploadState.value = UploadState.Blurring(
                            progress = "Face blur could not be applied. Video sent without blur."
                        )
                        _transitionHistory.add(_uploadState.value)
                    }
                } else {
                    // Non-minor: log skip per REQ-BLR-006
                    android.util.Log.d(
                        "VideoUploadViewModel",
                        "Face blur skipped — user is not a minor"
                    )
                }

                // ── Compression phase ──────────────────────────────────
                _uploadState.value = UploadState.Compressing(progress = 0f)
                _transitionHistory.add(_uploadState.value)

                val compressedFile = File(rawFile.parent, "compressed_${System.currentTimeMillis()}.mp4")

                val result = compressionService.compressVideo(
                    inputFile = fileToCompress,
                    outputFile = compressedFile
                ) { progress ->
                    _uploadState.value = UploadState.Compressing(progress = progress)
                    _transitionHistory.add(_uploadState.value)
                }

                currentCompressedFile = result

                // ── Upload phase ───────────────────────────────────────
                _uploadState.value = UploadState.Uploading(progress = 0f)
                _transitionHistory.add(_uploadState.value)

                val currentUser = authManager.currentUser.value
                val teacherUsername = currentUser?.username ?: "unknown"
                val assignmentId = "default" // Will be overridden when wired to chat

                val downloadUrl = uploadService.uploadVideo(
                    videoFile = result,
                    teacherUsername = teacherUsername,
                    assignmentId = assignmentId
                ) { progress ->
                    _uploadState.value = UploadState.Uploading(progress = progress)
                    _transitionHistory.add(_uploadState.value)
                }

                // ── Cleanup temp files ─────────────────────────────────
                cleanupTempFiles()

                // ── Done ───────────────────────────────────────────────
                _uploadState.value = UploadState.Done(videoUrl = downloadUrl)
                _transitionHistory.add(_uploadState.value)

            } catch (e: Exception) {
                // Determine which phase failed based on current state
                val currentState = _uploadState.value
                val phase = when (currentState) {
                    is UploadState.Blurring -> "blur"
                    is UploadState.Compressing -> "compression"
                    is UploadState.Uploading -> "upload"
                    else -> "processing"
                }
                _uploadState.value = UploadState.Error(
                    "Video $phase failed: ${e.message}"
                )
                _transitionHistory.add(_uploadState.value)
            }
        }
    }

    /**
     * Cancels recording or in-progress pipeline.
     * Discards any temporary files and returns to IDLE.
     */
    fun cancelRecording() {
        if (recordingService.isRecording()) {
            try { recordingService.stopRecording() } catch (_: Exception) {}
        }
        cleanupTempFiles()
        _uploadState.value = UploadState.Idle
        _transitionHistory.add(_uploadState.value)
        _error.value = null
    }

    /**
     * Resets state back to IDLE after Done or Error.
     * Clears transition history for the next pipeline run.
     */
    fun resetState() {
        _uploadState.value = UploadState.Idle
        _transitionHistory.clear()
        _error.value = null
        _blurWarning.value = null
        currentRecordingFile = null
        currentBlurredFile = null
        currentCompressedFile = null
    }

    /**
     * Returns whether a recording is currently active.
     */
    fun isRecording(): Boolean = recordingService.isRecording()

    // ═══════════════════════════════════════════════════════════════════
    // Internal
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Deletes temporary recording and compression files.
     *
     * REQ-VID-008: Clean up temp files after successful upload.
     * On failure, files are preserved for retry.
     */
    private fun cleanupTempFiles() {
        currentRecordingFile?.takeIf { it.exists() }?.delete()
        currentBlurredFile?.takeIf { it.exists() && it != currentRecordingFile }?.delete()
        currentCompressedFile?.takeIf { it.exists() && it != currentRecordingFile }?.delete()
        currentRecordingFile = null
        currentBlurredFile = null
        currentCompressedFile = null
    }

    override fun onCleared() {
        super.onCleared()
        cleanupTempFiles()
    }
}
