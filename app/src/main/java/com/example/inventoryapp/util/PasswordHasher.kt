package com.example.inventoryapp.util

import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * Password hashing with PBKDF2-HMAC-SHA256.
 *
 * Implemented on top of `HmacSHA256` (available on every Android version) instead of
 * `SecretKeyFactory("PBKDF2WithHmacSHA256")`, which only exists on API 26+ while minSdk is 24.
 *
 * Stored format: `pbkdf2_sha256$<iterations>$<saltHex>$<hashHex>`. The same format can be
 * verified by a server later (step 2), e.g. with Python's hashlib.pbkdf2_hmac or Node's crypto.pbkdf2.
 */
object PasswordHasher {
    private const val ALGORITHM = "pbkdf2_sha256"
    const val DEFAULT_ITERATIONS = 120_000
    private const val SALT_BYTES = 16
    private const val HASH_BYTES = 32

    private val random = SecureRandom()

    fun hash(password: String, iterations: Int = DEFAULT_ITERATIONS): String {
        val salt = ByteArray(SALT_BYTES).also { random.nextBytes(it) }
        val derived = pbkdf2(password.toByteArray(Charsets.UTF_8), salt, iterations, HASH_BYTES)
        return "$ALGORITHM\$$iterations\$${salt.toHex()}\$${derived.toHex()}"
    }

    fun verify(password: String, stored: String): Boolean {
        val parts = stored.split('$')
        if (parts.size != 4 || parts[0] != ALGORITHM) return false
        val iterations = parts[1].toIntOrNull() ?: return false
        val salt = parts[2].hexToBytesOrNull() ?: return false
        val expected = parts[3].hexToBytesOrNull() ?: return false
        val actual = pbkdf2(password.toByteArray(Charsets.UTF_8), salt, iterations, expected.size)
        // Constant-time comparison
        return MessageDigest.isEqual(actual, expected)
    }

    /** RFC 8018 PBKDF2 with HMAC-SHA256 as the PRF. */
    internal fun pbkdf2(password: ByteArray, salt: ByteArray, iterations: Int, keyLength: Int): ByteArray {
        require(iterations > 0 && keyLength > 0)
        val mac = Mac.getInstance("HmacSHA256")
        // HMAC accepts any key length; an empty password needs a non-empty key spec.
        mac.init(SecretKeySpec(if (password.isEmpty()) ByteArray(1) else password, "HmacSHA256").let {
            if (password.isEmpty()) EmptyKey else it
        })
        val hLen = mac.macLength
        val blocks = (keyLength + hLen - 1) / hLen
        val output = ByteArray(keyLength)
        var offset = 0
        for (i in 1..blocks) {
            mac.update(salt)
            mac.update(byteArrayOf((i ushr 24).toByte(), (i ushr 16).toByte(), (i ushr 8).toByte(), i.toByte()))
            var u = mac.doFinal()
            val t = u.copyOf()
            repeat(iterations - 1) {
                u = mac.doFinal(u)
                for (k in t.indices) t[k] = (t[k].toInt() xor u[k].toInt()).toByte()
            }
            val len = minOf(hLen, keyLength - offset)
            System.arraycopy(t, 0, output, offset, len)
            offset += len
        }
        return output
    }

    /** SecretKeySpec rejects empty keys, but HMAC with an empty key is valid. */
    private object EmptyKey : javax.crypto.SecretKey {
        private fun readResolve(): Any = EmptyKey
        override fun getAlgorithm() = "HmacSHA256"
        override fun getFormat() = "RAW"
        override fun getEncoded() = ByteArray(0)
    }

    private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }

    private fun String.hexToBytesOrNull(): ByteArray? {
        if (length % 2 != 0) return null
        return try {
            ByteArray(length / 2) { i -> substring(i * 2, i * 2 + 2).toInt(16).toByte() }
        } catch (e: NumberFormatException) {
            null
        }
    }
}
