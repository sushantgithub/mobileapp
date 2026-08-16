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

class CardSearchTest {
    private val hdfc = sample("1", "HDFC Millennia")
    private val amazon = sample("2", "Amazon Pay ICICI")
    private val sbi = sample("3", "SBI SimplyCLICK")

    @Test
    fun emptyQueryReturnsAll() {
        assertEquals(listOf(hdfc, amazon, sbi), CardSearch.byNickname(listOf(hdfc, amazon, sbi), "  "))
    }

    @Test
    fun matchesNicknameIgnoringCase() {
        assertEquals(listOf(amazon), CardSearch.byNickname(listOf(hdfc, amazon, sbi), "amazon"))
    }

    @Test
    fun matchesPartialNickname() {
        assertEquals(listOf(hdfc), CardSearch.byNickname(listOf(hdfc, amazon, sbi), "Mill"))
    }

    @Test
    fun noMatchReturnsEmpty() {
        assertTrue(CardSearch.byNickname(listOf(hdfc, amazon), "axis").isEmpty())
    }

    private fun sample(id: String, nickname: String) = CardRecord(
        id = id,
        nickname = nickname,
        cardholderName = "Test",
        number = "4111111111111111",
        expiryMonth = 12,
        expiryYear = 2030,
        cvv = "123",
        createdAtEpochMs = 0,
        updatedAtEpochMs = 0,
    )
}
