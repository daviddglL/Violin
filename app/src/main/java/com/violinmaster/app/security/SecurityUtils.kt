package com.violinmaster.app.security

import android.content.Context
import android.util.Base64
import dagger.hilt.android.qualifiers.ApplicationContext
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Arrays
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SecurityUtils @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val PREFS_NAME = "secure_user_prefs"
        private const val KEY_HASHED_PASSCODE = "hashed_passcode"
        private const val KEY_PASSCODE_SALT = "passcode_salt"

        /**
         * Generates a secure, random salt for password/passcode hashing.
         */
        fun generateSalt(length: Int = 16): ByteArray {
            val random = SecureRandom()
            val salt = ByteArray(length)
            random.nextBytes(salt)
            return salt
        }

        /**
         * Hashes the passcode with a cryptographic SHA-256 digest and salt to prevent dictionary attacks.
         * Includes a clean practice of clearing the intermediate password array.
         */
        fun hashPasscode(passcodeChars: CharArray, salt: ByteArray): String {
            return try {
                val digest = MessageDigest.getInstance("SHA-256")
                digest.reset()
                digest.update(salt)

                // Convert CharArray to bytes securely without creating intermediate immutable Strings
                val passcodeBytes = passcodeChars.map { it.code.toByte() }.toByteArray()
                val hashedBytes = digest.digest(passcodeBytes)

                // Clear sensitive intermediate byte array from the heap instantly
                Arrays.fill(passcodeBytes, 0.toByte())

                // Convert to secure Hex representation
                hashedBytes.joinToString("") { "%02x".format(it) }
            } catch (e: Exception) {
                ""
            }
        }
    }

    /**
     * Saves a passcode securely in the app context's SharedPreferences using salt + cryptographic digest.
     */
    fun savePasscode(clearcode: String): Boolean {
        if (clearcode.length < 4) return false
        val sharedPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val salt = generateSalt()
        val passcodeChars = clearcode.toCharArray()

        val hashed = hashPasscode(passcodeChars, salt)

        // Clean out raw CharArray
        Arrays.fill(passcodeChars, '0')

        sharedPrefs.edit().apply {
            putString(KEY_HASHED_PASSCODE, hashed)
            putString(KEY_PASSCODE_SALT, Base64.encodeToString(salt, Base64.DEFAULT))
            apply()
        }
        return true
    }

    /**
     * Validates an incoming clearcode input against stored salt + hashed credentials.
     */
    fun verifyPasscode(inputCode: String): Boolean {
        val sharedPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val storedHash = sharedPrefs.getString(KEY_HASHED_PASSCODE, null) ?: return false
        val saltBase64 = sharedPrefs.getString(KEY_PASSCODE_SALT, null) ?: return false

        val salt = Base64.decode(saltBase64, Base64.DEFAULT)
        val passChars = inputCode.toCharArray()
        val computedHash = hashPasscode(passChars, salt)

        // Promptly wipe from memory
        Arrays.fill(passChars, '0')

        return computedHash == storedHash
    }

    /**
     * Checks if a passcode has been set in SharedPreferences.
     */
    fun isPasscodeSet(): Boolean {
        val sharedPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return sharedPrefs.contains(KEY_HASHED_PASSCODE)
    }

    /**
     * Clears any saved master passcode.
     */
    fun clearPasscode() {
        val sharedPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        sharedPrefs.edit().apply {
            remove(KEY_HASHED_PASSCODE)
            remove(KEY_PASSCODE_SALT)
            apply()
        }
    }
}
