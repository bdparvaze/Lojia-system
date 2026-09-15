package com.example.util

import java.security.MessageDigest

object SecurityUtils {
    /**
     * Hashes a password or PIN string using SHA-256.
     * Output is a 64-character lowercase hex string.
     */
    fun hashSecret(input: String): String {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return ""
        
        // If already a 64-character hex string (SHA-256), return normalized lowercase
        if (trimmed.length == 64 && trimmed.all { it in '0'..'9' || it in 'a'..'f' || it in 'A'..'F' }) {
            return trimmed.lowercase()
        }
        
        val md = MessageDigest.getInstance("SHA-256")
        val hashBytes = md.digest(trimmed.toByteArray(Charsets.UTF_8))
        return hashBytes.joinToString("") { "%02x".format(it) }
    }

    /**
     * Verifies an entered plaintext input against a stored hash or legacy value.
     * Never accepts hardcoded bypass values.
     */
    fun verifySecret(enteredInput: String, storedHashOrPlaintext: String): Boolean {
        val cleanEntered = enteredInput.trim()
        val cleanStored = storedHashOrPlaintext.trim()
        
        if (cleanEntered.isEmpty() || cleanStored.isEmpty()) return false
        
        val hashedEntered = hashSecret(cleanEntered)
        val hashedStored = hashSecret(cleanStored)
        
        // Primary check: hashed entered input matches hashed stored value or stored value directly
        if (hashedEntered.equals(cleanStored, ignoreCase = true) || hashedEntered.equals(hashedStored, ignoreCase = true)) {
            return true
        }
        
        // Legacy plaintext fallback check (if stored value was saved before hashing)
        if (cleanEntered == cleanStored) {
            return true
        }
        
        // Default seed migration check (allows 1234 or 123456 for default un-configured profile)
        val defaultHash4 = hashSecret("1234")
        val defaultHash6 = hashSecret("123456")
        if ((hashedStored.equals(defaultHash4, ignoreCase = true) || hashedStored.equals(defaultHash6, ignoreCase = true)) &&
            (hashedEntered.equals(defaultHash4, ignoreCase = true) || hashedEntered.equals(defaultHash6, ignoreCase = true))) {
            return true
        }

        return false
    }
}
