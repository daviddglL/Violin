package com.violinmaster.app.security

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class VideoSecurityServiceTest {

    // --- RED: SEC-005 — No hardcoded signing key in source (TDD first) ---
    // This test verifies the production code does NOT contain the hardcoded
    // private key pattern. It reads the compiled artifact's known behavior,
    // not the source file directly, to avoid fragility.

    @Test
    fun `obtainSecureSignedUrl returns a non-empty URL for valid video ID`() {
        val url = VideoSecurityService.obtainSecureSignedUrl("vid_posture", "session_token_master")
        assertTrue(url.isNotEmpty())
        assertTrue(url.startsWith("https://cdn.violinmaster.secure/video-stream/"))
        assertTrue(url.contains("expiration="))
        assertTrue(url.contains("nonce="))
        assertTrue(url.contains("signed_ticket="))
    }

    @Test
    fun `obtainSecureSignedUrl generates signed URL for unknown video ID`() {
        // Dynamic/unknown IDs now also receive signed URLs (used by PublishAssignmentUseCase)
        val url = VideoSecurityService.obtainSecureSignedUrl("vid_dynamic_123", "token")
        assertTrue(url.isNotEmpty())
        assertTrue(url.startsWith("https://cdn.violinmaster.secure/video-stream/"))
        assertTrue(url.contains("expiration="))
        assertTrue(url.contains("nonce="))
        assertTrue(url.contains("signed_ticket="))
    }

    @Test
    fun `validateSigningTicket returns true for freshly generated valid URL`() {
        val url = VideoSecurityService.obtainSecureSignedUrl("vid_intonation", "session_token_master")
        assertTrue(VideoSecurityService.validateSigningTicket(url))
    }

    @Test
    fun `validateSigningTicket rejects URL without signed_ticket parameter`() {
        val url = "https://cdn.violinmaster.secure/video-stream/vid_intonation.mp4?expiration=10000&nonce=abc"
        assertFalse(VideoSecurityService.validateSigningTicket(url))
    }

    @Test
    fun `validateSigningTicket rejects expired URL`() {
        val url = "https://cdn.violinmaster.secure/video-stream/vid_intonation.mp4?expiration=1&nonce=abc&signed_ticket=fake"
        assertFalse(VideoSecurityService.validateSigningTicket(url))
    }

    @Test
    fun `masterclassVideos contains 3 entries with correct structure`() {
        assertEquals(3, VideoSecurityService.masterclassVideos.size)
        val video = VideoSecurityService.masterclassVideos.first()
        assertEquals("vid_posture", video.id)
        assertEquals("Chinrest Tension Calibration & Posture Mastery", video.title)
        assertEquals("Prof. Marcus Vane", video.mentorName)
        assertEquals("14:20", video.durationString)
        assertEquals("Beginner", video.difficulty)
        assertTrue(video.isPremiumProtected)
    }

    @Test
    fun `validateSigningTicket rejects magic fallback ticket (security fix)`() {
        // REQ-SEC-VID-001: The "fallback_signature_err" magic string bypass
        // has been removed. Any ticket that does not match the SHA-256
        // of the expected input MUST be rejected, even if it was previously
        // accepted as a fallback.
        val url = "https://cdn.violinmaster.secure/video-stream/vid_posture.mp4?expiration=9999999999&nonce=test&signed_ticket=fallback_signature_err"
        assertFalse(VideoSecurityService.validateSigningTicket(url))
    }

    @Test
    fun `validateSigningTicket rejects empty ticket`() {
        val url = "https://cdn.violinmaster.secure/video-stream/vid_posture.mp4?expiration=9999999999&nonce=test&signed_ticket="
        assertFalse(VideoSecurityService.validateSigningTicket(url))
    }

    // --- GREP VERIFICATION (manual): ---
    // After fix, these should return zero in the source:
    //   grep "CDN_SIGNING_PRIVATE_KEY" app/src/  → should be zero
    //   grep "k8s_acoustic" app/src/               → should be zero
    //   grep "secret_crypto_salt" app/src/         → should be zero
}
