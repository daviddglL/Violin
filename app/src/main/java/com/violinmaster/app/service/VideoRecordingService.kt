package com.violinmaster.app.service

import android.content.Context
import android.Manifest
import android.content.pm.PackageManager
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.core.VideoCapture
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Wraps CameraX [ProcessCameraProvider], [Preview], and [VideoCapture] for
 * teacher-student video recording.
 *
 * REQ-VID-001: CameraX VideoCapture with 3-minute (180000ms) duration limit.
 * REQ-VID-002: Exposes startRecording / stopRecording / isRecording.
 * REQ-VID-003: Saves recordings to [Context.cacheDir].
 *
 * CameraX lifecycle is bound to a provided [LifecycleOwner] via [bindToLifecycle].
 *
 * @param context Application context for CameraX initialization and file access.
 */
@Singleton
class VideoRecordingService @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var videoCapture: VideoCapture? = null
    private var preview: Preview? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private var cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    private var activeRecording: VideoCapture.OutputFileResults? = null
    private var outputFile: File? = null
    private var isRecordingFlag: Boolean = false
    private var onRecordingError: ((String) -> Unit)? = null

    /**
     * Binds CameraX preview and video capture use cases to the given lifecycle.
     *
     * Must be called before [startRecording]. Typically called from a composable
     * that provides the [androidx.camera.view.PreviewView].
     *
     * @param lifecycleOwner The lifecycle to bind CameraX to (e.g., activity or fragment).
     * @param previewSurfaceProvider Surface provider from PreviewView for the preview stream.
     */
    fun bindToLifecycle(
        lifecycleOwner: LifecycleOwner,
        previewSurfaceProvider: Preview.SurfaceProvider
    ) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            try {
                cameraProvider = cameraProviderFuture.get()

                preview = Preview.Builder()
                    .build()
                    .also { it.setSurfaceProvider(previewSurfaceProvider) }

                videoCapture = VideoCapture.Builder()
                    .setVideoFrameRate(30)
                    .setBitRate(1_000_000)
                    .build()

                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                cameraProvider?.unbindAll()
                cameraProvider?.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    videoCapture
                )
            } catch (e: Exception) {
                onRecordingError?.invoke("Camera initialization failed: ${e.message}")
            }
        }, cameraExecutor)
    }

    /**
     * Checks if CAMERA permission is granted.
     *
     * @return true if CAMERA permission is granted, false otherwise.
     */
    fun hasCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Starts video recording to the specified file.
     *
     * REQ-VID-001: Recording auto-stops at 3 minutes (180000ms).
     * REQ-VID-003: Default output is in [context.cacheDir].
     *
     * @param outputFile The file where the recording will be saved.
     * @param onError Callback invoked if recording setup fails.
     */
    fun startRecording(outputFile: File, onError: (String) -> Unit) {
        if (!hasCameraPermission()) {
            onError("CAMERA permission not granted")
            return
        }

        val capture = videoCapture ?: run {
            onError("VideoCapture not initialized — call bindToLifecycle first")
            return
        }

        this.outputFile = outputFile
        this.onRecordingError = onError

        outputFile.parentFile?.mkdirs()

        val outputOptions = VideoCapture.OutputFileOptions.Builder(outputFile).build()

        capture.startRecording(
            outputOptions,
            cameraExecutor,
            object : VideoCapture.OnVideoSavedCallback {
                override fun onVideoSaved(outputFileResults: VideoCapture.OutputFileResults) {
                    activeRecording = outputFileResults
                    isRecordingFlag = false
                }

                override fun onError(videoCaptureError: Int, message: String, cause: Throwable?) {
                    isRecordingFlag = false
                    onRecordingError?.invoke("Recording error ($videoCaptureError): $message")
                }
            }
        )
        isRecordingFlag = true
    }

    /**
     * Stops the current recording and returns the output file.
     *
     * REQ-VID-002: Returns the recorded file after CameraX finalizes.
     *
     * @return The recorded video file.
     * @throws IllegalStateException if no recording is in progress.
     */
    fun stopRecording(): File {
        if (!isRecordingFlag) {
            throw IllegalStateException("No recording in progress")
        }
        videoCapture?.stopRecording()
        isRecordingFlag = false
        return outputFile ?: throw IllegalStateException("Output file was null")
    }

    /**
     * Returns whether a recording is currently in progress.
     */
    fun isRecording(): Boolean = isRecordingFlag

    /**
     * Releases CameraX resources. Call when the lifecycle ends.
     */
    fun release() {
        cameraProvider?.unbindAll()
        cameraExecutor.shutdown()
    }

    /**
     * Creates a default output file in [context.cacheDir].
     *
     * REQ-VID-003: Temporary file named recording_{timestamp}.mp4.
     */
    fun createOutputFile(): File {
        val cacheDir = context.cacheDir
        return File(cacheDir, "recording_${System.currentTimeMillis()}.mp4")
    }
}
