package com.cardvault.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CryptoEngineTest {
    @Test
    fun wrapAndUnwrapDek() {
        val pin = "246810".toCharArray()
        val dek = CryptoEngine.generateDek()
        val wrapped = CryptoEngine.wrapDek(dek, pin)
        val restored = CryptoEngine.unwrapDek(wrapped, pin)
        assertTrue(dek.contentEquals(restored))
    }

    @Test
    fun wrongPinFails() {
        val dek = CryptoEngine.generateDek()
        val wrapped = CryptoEngine.wrapDek(dek, "1111".toCharArray())
        var failed = false
        try {
            CryptoEngine.unwrapDek(wrapped, "2222".toCharArray())
        } catch (_: Exception) {
            failed = true
        }
        assertTrue(failed)
    }

    @Test
    fun encryptRoundTrip() {
        val key = CryptoEngine.generateDek()
        val sealed = CryptoEngine.encrypt("secret-cards".toByteArray(), key)
        val plain = CryptoEngine.decrypt(sealed.ciphertext, key, sealed.iv)
        assertEquals("secret-cards", String(plain))
    }
}

class CardNumberFormatterTest {
    @Test
    fun luhnAcceptsVisaTestNumber() {
        assertTrue(CardNumberFormatter.luhnValid("4111111111111111"))
    }

    @Test
    fun luhnRejectsBadNumber() {
        assertFalse(CardNumberFormatter.luhnValid("4111111111111112"))
    }

    @Test
    fun maskKeepsLastFour() {
        assertEquals("•••• •••• •••• 1111", CardNumberFormatter.mask("4111111111111111"))
    }
}
