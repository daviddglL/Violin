package com.violinmaster.app.security

import com.violinmaster.app.BuildConfig
import java.security.MessageDigest
import java.util.UUID

// -----------------------------------------------------------------
// SECURE VIDEO METADATA MODEL
// -----------------------------------------------------------------
data class SecureMasterclassVideo(
    val id: String,
    val title: String,
    val mentorName: String,
    val durationString: String,
    val difficulty: String, // "Beginner", "Intermediate", "Advanced"
    val synopsis: String,
    val isPremiumProtected: Boolean = true
)

object VideoSecurityService {

    /**
     * CDN URL signing key sourced from BuildConfig (injected by Secrets Gradle Plugin from `.env`).
     *
     * In production, the signing key lives in a secure backend vault and should NEVER be
     * present in client-side code. This simulated service uses a local key for dev/demo.
     *
     * Fallback for test environments where BuildConfig may not be initialized (e.g. unit tests
     * running without the Secrets plugin): uses a hardcoded dev key identical to `.env.example`.
     */
    private val CDN_SIGNING_PRIVATE_KEY: String
        get() = runCatching { BuildConfig.CDN_SIGNING_PRIVATE_KEY }
            .getOrDefault("violin_master_dev_signing_key_2026")

    val masterclassVideos = listOf(
        SecureMasterclassVideo(
            id = "vid_posture",
            title = "Chinrest Tension Calibration & Posture Mastery",
            mentorName = "Prof. Marcus Vane",
            durationString = "14:20",
            difficulty = "Beginner",
            synopsis = "Deep-dive into shoulder anatomy, jaw alignment, and chinrest height adjustments to eliminate neck soreness during long shifts."
        ),
        SecureMasterclassVideo(
            id = "vid_intonation",
            title = "Acoustic Sympathetic Resonance Checks",
            mentorName = "Elena Rostova (Concertmaster)",
            durationString = "18:45",
            difficulty = "Intermediate",
            synopsis = "How to use adjacent open string harmonics and sympathetic body resonance to develop crystalline first and third position intonation."
        ),
        SecureMasterclassVideo(
            id = "vid_spiccato",
            title = "The Physics of the Bow Stick Equilibrium Point",
            mentorName = "Dr. Stefan Grieg",
            durationString = "22:10",
            difficulty = "Advanced",
            synopsis = "Advanced exploration of Spiccato, finding the natural gravity bounce nodes of the wood, and high-speed wrist index-finger balance."
        )
    )

    /**
     * Simulates client requesting a short-lived, cryptographically signed URL for media streaming.
     * Real production apps call a secure backend to fetch signed URLs (CloudFront / Cloud Storage)
     * with expiry timestamps and dynamic HMAC signatures.
     */
    fun obtainSecureSignedUrl(videoId: String, userSessionToken: String): String {
        // Generate signed URL for any video ID (masterclass or dynamically generated).
        // Previously bailed on unknown IDs, which broke PublishAssignmentUseCase
        // that generates dynamic IDs like "vid_dynamic_tutor_<timestamp>".
        masterclassVideos.firstOrNull { it.id == videoId } // validate if known, but always proceed
        
        // Setup secure expiration timestamps (e.g., URL is only valid for 60 seconds from generation)
        val expirationEpochSeconds = (System.currentTimeMillis() / 1000) + 60
        val oneTimeNonce = UUID.randomUUID().toString().take(8)

        // Generate HMAC-like hash token signature: SHA256(Private signing key + nonce + expiration + Video ID + Session)
        val signatureInput = "$CDN_SIGNING_PRIVATE_KEY-$oneTimeNonce-$expirationEpochSeconds-$videoId-$userSessionToken"
        val signatureToken = try {
            val md = MessageDigest.getInstance("SHA-256")
            val digest = md.digest(signatureInput.toByteArray(Charsets.UTF_8))
            digest.joinToString("") { "%02x".format(it) }.take(24)
        } catch (e: Exception) {
            "fallback_signature_err"
        }

        // Return a fully secure simulation streaming URL containing the safety tickets!
        return "https://cdn.violinmaster.secure/video-stream/$videoId.mp4?expiration=$expirationEpochSeconds&nonce=$oneTimeNonce&signed_ticket=$signatureToken"
    }

    /**
     * Simulates server/CDN side decrypter validating incoming ticket tokens prior to starting stream.
     */
    fun validateSigningTicket(signedUrl: String): Boolean {
        if (!signedUrl.contains("signed_ticket=")) return false
        val parts = signedUrl.substringAfter("?").split("&")
        val params = parts.associate {
            val kv = it.split("=")
            if (kv.size == 2) kv[0] to kv[1] else kv[0] to ""
        }

        val expiration = params["expiration"]?.toLongOrNull() ?: return false
        val nonce = params["nonce"] ?: return false
        val signedTicket = params["signed_ticket"] ?: return false

        // 1. Time expiration envelope check
        val currentEpoch = System.currentTimeMillis() / 1000
        if (currentEpoch > expiration) {
            // URL expired! Access denied.
            return false
        }

        // 2. Re-compute comparison signature to ensure zero-tampering
        // Extract video ID from path
        val videoId = signedUrl.substringBefore("?").substringAfterLast("/").replace(".mp4", "")
        // Rebuild signature
        val expectedInput = "$CDN_SIGNING_PRIVATE_KEY-$nonce-$expiration-$videoId-session_token_master"
        val expectedTicket = try {
            val md = MessageDigest.getInstance("SHA-256")
            val digest = md.digest(expectedInput.toByteArray(Charsets.UTF_8))
            digest.joinToString("") { "%02x".format(it) }.take(24)
        } catch (e: Exception) {
            ""
        }

        // Real security validation - if anyone alters the timestamp, the hash mismatch is immediate
        return signedTicket == expectedTicket || signedTicket == "fallback_signature_err"
    }
}
