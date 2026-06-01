package com.violinmaster.app.domain.util

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class Base64EncoderTest {

  @Test
  fun `encode produces non-empty string for non-empty input`() {
    val input = "Hello, World!".toByteArray()
    val encoded = Base64Encoder.encodeToString(input)
    assertNotNull(encoded)
    assertTrue(encoded.isNotEmpty())
  }

  @Test
  fun `encode-decode round-trip preserves original data`() {
    val original = "Violin Master test data 12345!@#$%".toByteArray()
    val encoded = Base64Encoder.encodeToString(original)
    val decoded = Base64Encoder.decode(encoded)
    assertArrayEquals("Round-trip should preserve data", original, decoded)
  }

  @Test
  fun `encode empty byte array produces valid base64`() {
    val encoded = Base64Encoder.encodeToString(ByteArray(0))
    assertNotNull(encoded)
    assertEquals("", encoded)
  }

  @Test
  fun `decode empty string returns empty byte array`() {
    val decoded = Base64Encoder.decode("")
    assertArrayEquals(ByteArray(0), decoded)
  }

  @Test
  fun `encode binary data round-trip works`() {
    val binary = ByteArray(256) { it.toByte() }
    val encoded = Base64Encoder.encodeToString(binary)
    val decoded = Base64Encoder.decode(encoded)
    assertArrayEquals(binary, decoded)
  }

  @Test
  fun `encode produces strings with only base64 characters`() {
    val input = "test".toByteArray()
    val encoded = Base64Encoder.encodeToString(input)
    val base64Regex = Regex("^[A-Za-z0-9+/=]+$")
    assertTrue("Encoded string should contain only base64 chars: $encoded", base64Regex.matches(encoded))
  }
}
