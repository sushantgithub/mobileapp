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
    val kind: CardKind = CardKind.CREDIT,
    val billGenerationDate: String = "",
    val dueDate: String = "",
    val billingZip: String = "",
    val notes: String = "",
    val createdAtEpochMs: Long,
    val updatedAtEpochMs: Long,
)

@Serializable
enum class CardKind {
    CREDIT,
    DEBIT,
}

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

object CardDates {
    fun forKind(kind: CardKind, billGenerationDate: String, dueDate: String): Pair<String, String> {
        if (kind == CardKind.DEBIT) return "" to ""
        return canonical(billGenerationDate) to canonical(dueDate)
    }

    fun canonical(value: String): String {
        val day = parseDay(value) ?: return ""
        return day.toString()
    }

    fun parseDay(value: String): Int? {
        val trimmed = value.trim()
        if (trimmed.isEmpty()) return null
        if (ISO_DATE.matches(trimmed)) {
            val day = trimmed.substring(8, 10).toIntOrNull() ?: return null
            return day.takeIf { it in 1..31 }
        }
        val digits = trimmed.filter { it.isDigit() }
        if (digits.isEmpty()) return null
        return digits.toIntOrNull()?.takeIf { it in 1..31 }
    }

    fun display(value: String): String {
        val day = parseDay(value) ?: return ""
        return "${ordinal(day)} of every month"
    }

    fun ordinal(day: Int): String {
        val suffix = when {
            day % 100 in 11..13 -> "th"
            day % 10 == 1 -> "st"
            day % 10 == 2 -> "nd"
            day % 10 == 3 -> "rd"
            else -> "th"
        }
        return "$day$suffix"
    }

    private val ISO_DATE = Regex("""\d{4}-\d{2}-\d{2}""")
}

object CardSearch {
    fun byNickname(cards: List<CardRecord>, query: String): List<CardRecord> {
        val needle = query.trim()
        if (needle.isEmpty()) return cards
        return cards.filter { it.nickname.contains(needle, ignoreCase = true) }
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
