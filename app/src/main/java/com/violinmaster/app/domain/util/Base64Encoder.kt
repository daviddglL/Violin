package com.violinmaster.app.domain.util

import java.util.Base64

/**
 * Pure utility wrapping java.util.Base64 (NOT android.util.Base64).
 *
 * No Android imports — functions are pure and testable without Robolectric.
 *
 * REQ-ARCH-005-S1: Replace all android.util.Base64 usage with this domain utility.
 */
object Base64Encoder {

  /**
   * Encodes a byte array to a Base64 string using the basic encoder.
   */
  fun encodeToString(bytes: ByteArray): String = Base64.getEncoder().encodeToString(bytes)

  /**
   * Decodes a Base64 string back to a byte array using the basic decoder.
   */
  fun decode(str: String): ByteArray = Base64.getDecoder().decode(str)
}
