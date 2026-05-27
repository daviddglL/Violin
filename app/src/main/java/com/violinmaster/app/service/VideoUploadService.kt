package com.violinmaster.app.service

import android.net.Uri
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Uploads video files to Firebase Storage and obtains download URLs.
 *
 * REQ-VID-005: Uploads to videos/{teacherUsername}/{assignmentId}/{timestamp}.mp4.
 * REQ-VID-006: Emits progress via [onProgress] callback (0.0 to 1.0).
 *
 * Error handling:
 * - Network failure: throws IOException
 * - Auth failure: throws SecurityException with Firebase error detail
 * - Quota exceeded: throws StorageException
 *
 * @param storage Firebase Storage instance (injected from FirebaseModule).
 */
@Singleton
class VideoUploadService @Inject constructor(
    private val storage: FirebaseStorage
) {

    companion object {
        private const val VIDEOS_PATH = "videos"
    }

    /**
     * Uploads a video file to Firebase Storage and returns the download URL.
     *
     * REQ-VID-005: Path = videos/{teacherUsername}/{assignmentId}/{timestamp}.mp4.
     * REQ-VID-006: Progress callback 0.0 → 1.0.
     *
     * @param videoFile Local compressed video file to upload.
     * @param teacherUsername Teacher's username for path scoping.
     * @param assignmentId Assignment ID for path scoping.
     * @param onProgress Progress callback with values from 0.0 to 1.0.
     * @return The download URL string of the uploaded video.
     * @throws IOException On network failure.
     * @throws SecurityException On authentication failure.
     * @throws StorageException On quota exceeded or other Storage errors.
     */
    suspend fun uploadVideo(
        videoFile: File,
        teacherUsername: String,
        assignmentId: String,
        onProgress: (Float) -> Unit
    ): String = withContext(Dispatchers.IO) {
        if (!videoFile.exists() || !videoFile.canRead()) {
            throw IOException("Video file does not exist or is not readable: ${videoFile.absolutePath}")
        }

        val timestamp = System.currentTimeMillis()
        val path = "$VIDEOS_PATH/$teacherUsername/$assignmentId/$timestamp.mp4"
        val storageRef = storage.reference.child(path)
        val fileUri = Uri.fromFile(videoFile)

        // ── Upload with progress tracking ──────────────────────────
        val uploadTask = storageRef.putFile(fileUri)

        uploadTask.addOnProgressListener { taskSnapshot ->
            val progress = taskSnapshot.bytesTransferred.toFloat() /
                taskSnapshot.totalByteCount.toFloat()
            onProgress(progress.coerceIn(0f, 1f))
        }

        // ── Await upload completion ────────────────────────────────
        suspendCancellableCoroutine<Unit> { continuation ->
            uploadTask
                .addOnSuccessListener {
                    continuation.resume(Unit)
                }
                .addOnFailureListener { exception ->
                    val mapped = when (exception) {
                        is StorageException -> {
                            when (exception.errorCode) {
                                StorageException.ERROR_NOT_AUTHENTICATED ->
                                    SecurityException("Firebase Storage: not authenticated")
                                StorageException.ERROR_QUOTA_EXCEEDED ->
                                    StorageException::class.java
                                        .getDeclaredConstructor(
                                            Int::class.javaPrimitiveType,
                                            Throwable::class.java
                                        )
                                        .let {
                                            throw exception // rethrow original
                                        }
                                else -> exception
                            }
                        }
                        is IOException -> exception
                        else -> IOException("Upload failed: ${exception.message}", exception)
                    }
                    continuation.resumeWithException(mapped)
                }
                .addOnCanceledListener {
                    continuation.resumeWithException(
                        IOException("Upload was cancelled")
                    )
                }
        }

        // ── Obtain download URL ───────────────────────────────────
        val downloadUrl = suspendCancellableCoroutine<String> { continuation ->
            storageRef.downloadUrl
                .addOnSuccessListener { uri ->
                    continuation.resume(uri.toString())
                }
                .addOnFailureListener { exception ->
                    continuation.resumeWithException(
                        IOException("Failed to obtain download URL: ${exception.message}", exception)
                    )
                }
        }

        onProgress(1.0f)
        downloadUrl
    }
}
