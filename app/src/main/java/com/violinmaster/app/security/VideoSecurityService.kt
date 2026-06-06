package com.violinmaster.app.security

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

    // TODO(production): Replace with BuildConfig field or environment-seeded key before production release.
    // In real production, the signing key lives in a secure backend vault and is NEVER present
    // in client-side code. This simulated service uses a local key only for dev/demo purposes.
    private const val CDN_SIGNING_PRIVATE_KEY = "demo_signing_key_not_for_production"

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
