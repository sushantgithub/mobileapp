package com.cardvault.app.data

import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

object CryptoEngine {
    const val KDF_ITERATIONS = 210_000
    private const val KEY_LENGTH_BITS = 256
    private const val GCM_IV_BYTES = 12
    private const val GCM_TAG_BITS = 128
    private const val SALT_BYTES = 16
    private const val DEK_BYTES = 32

    private val random = SecureRandom()

    fun generateDek(): ByteArray = ByteArray(DEK_BYTES).also { random.nextBytes(it) }

    fun generateSalt(): ByteArray = ByteArray(SALT_BYTES).also { random.nextBytes(it) }

    fun deriveKey(secret: CharArray, salt: ByteArray, iterations: Int = KDF_ITERATIONS): ByteArray {
        val spec = PBEKeySpec(secret, salt, iterations, KEY_LENGTH_BITS)
        return try {
            SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                .generateSecret(spec)
                .encoded
        } finally {
            spec.clearPassword()
        }
    }

    fun wrapDek(dek: ByteArray, pin: CharArray, iterations: Int = KDF_ITERATIONS): WrappedKey {
        val salt = generateSalt()
        val kek = deriveKey(pin, salt, iterations)
        return try {
            val sealed = encrypt(dek, kek)
            WrappedKey(
                saltB64 = b64(salt),
                ivB64 = b64(sealed.iv),
                ciphertextB64 = b64(sealed.ciphertext),
                iterations = iterations,
            )
        } finally {
            kek.fill(0)
        }
    }

    fun unwrapDek(wrapped: WrappedKey, pin: CharArray): ByteArray {
        val salt = fromB64(wrapped.saltB64)
        val kek = deriveKey(pin, salt, wrapped.iterations)
        return try {
            decrypt(fromB64(wrapped.ciphertextB64), kek, fromB64(wrapped.ivB64))
        } finally {
            kek.fill(0)
        }
    }

    fun encrypt(plaintext: ByteArray, key: ByteArray): SealedBytes {
        val iv = ByteArray(GCM_IV_BYTES).also { random.nextBytes(it) }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(GCM_TAG_BITS, iv))
        return SealedBytes(iv = iv, ciphertext = cipher.doFinal(plaintext))
    }

    fun decrypt(ciphertext: ByteArray, key: ByteArray, iv: ByteArray): ByteArray {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(GCM_TAG_BITS, iv))
        return cipher.doFinal(ciphertext)
    }

    fun b64(bytes: ByteArray): String = Base64.getEncoder().encodeToString(bytes)

    fun fromB64(value: String): ByteArray = Base64.getDecoder().decode(value)

    data class SealedBytes(val iv: ByteArray, val ciphertext: ByteArray)
}
