package com.dueday.app.data

import kotlinx.serialization.Serializable
import java.time.LocalDate
import java.time.YearMonth

@Serializable
data class Bill(
    val id: String,
    val title: String,
    val amount: String = "",
    val dayOfMonth: Int,
    val notes: String = "",
    val notify: Boolean = true,
)

@Serializable
data class BillStore(
    val bills: List<Bill> = emptyList(),
)

enum class ReminderKind {
    FIVE_DAYS_BEFORE,
    ON_BILL_DAY,
}

object BillReminders {
    fun kindsFor(today: LocalDate, dayOfMonth: Int): Set<ReminderKind> {
        require(dayOfMonth in 1..31)
        val kinds = mutableSetOf<ReminderKind>()
        for (offset in 0L..1L) {
            val month = YearMonth.from(today).plusMonths(offset)
            val billDate = month.atDay(dayOfMonth.coerceAtMost(month.lengthOfMonth()))
            val fiveBefore = billDate.minusDays(5)
            if (today == billDate) kinds += ReminderKind.ON_BILL_DAY
            if (today == fiveBefore) kinds += ReminderKind.FIVE_DAYS_BEFORE
        }
        return kinds
    }

    fun nextBillDate(today: LocalDate, dayOfMonth: Int): LocalDate {
        val thisMonth = YearMonth.from(today)
        val thisBill = thisMonth.atDay(dayOfMonth.coerceAtMost(thisMonth.lengthOfMonth()))
        return if (!thisBill.isBefore(today)) thisBill else {
            val next = thisMonth.plusMonths(1)
            next.atDay(dayOfMonth.coerceAtMost(next.lengthOfMonth()))
        }
    }
}
