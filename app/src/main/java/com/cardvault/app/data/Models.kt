package com.cardvault.app.data

import kotlinx.serialization.Serializable

@Serializable
data class CardRecord(
    val id: String,
    val nickname: String,
    val cardholderName: String,
    val number: String,
    val expiryMonth: Int,
    val expiryYear: Int,
    val cvv: String,
    val brand: CardBrand = CardBrand.detect(number),
    val billingZip: String = "",
    val notes: String = "",
    val createdAtEpochMs: Long,
    val updatedAtEpochMs: Long,
)

@Serializable
enum class CardBrand {
    VISA,
    MASTERCARD,
    AMEX,
    DISCOVER,
    RUPAY,
    OTHER,
    ;

    companion object {
        fun detect(number: String): CardBrand {
            val digits = number.filter { it.isDigit() }
            return when {
                digits.startsWith("4") -> VISA
                digits.length >= 2 && digits.take(2).toIntOrNull() in 51..55 -> MASTERCARD
                digits.length >= 4 && digits.take(4).toIntOrNull() in 2221..2720 -> MASTERCARD
                digits.startsWith("34") || digits.startsWith("37") -> AMEX
                digits.startsWith("6011") || digits.startsWith("65") -> DISCOVER
                digits.startsWith("60") || digits.startsWith("65") || digits.startsWith("81") || digits.startsWith("82") -> RUPAY
                else -> OTHER
            }
        }
    }
}

@Serializable
data class VaultPayload(
    val version: Int = 1,
    val cards: List<CardRecord> = emptyList(),
)

@Serializable
data class WrappedKey(
    val saltB64: String,
    val ivB64: String,
    val ciphertextB64: String,
    val iterations: Int,
)

@Serializable
data class EncryptedBackup(
    val magic: String = MAGIC,
    val version: Int = 1,
    val wrappedDek: WrappedKey,
    val vaultIvB64: String,
    val vaultCiphertextB64: String,
) {
    companion object {
        const val MAGIC = "CARDVAULT"
    }
}

object CardNumberFormatter {
    fun digitsOnly(value: String): String = value.filter { it.isDigit() }

    fun mask(number: String): String {
        val digits = digitsOnly(number)
        if (digits.length < 4) return "••••"
        return "•••• •••• •••• ${digits.takeLast(4)}"
    }

    fun pretty(number: String): String {
        val digits = digitsOnly(number)
        return digits.chunked(4).joinToString(" ")
    }

    fun luhnValid(number: String): Boolean {
        val digits = digitsOnly(number)
        if (digits.length !in 12..19) return false
        var sum = 0
        var alternate = false
        for (i in digits.indices.reversed()) {
            var n = digits[i] - '0'
            if (alternate) {
                n *= 2
                if (n > 9) n -= 9
            }
            sum += n
            alternate = !alternate
        }
        return sum % 10 == 0
    }
}
