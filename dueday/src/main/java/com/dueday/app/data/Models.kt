package com.dueday.app.data

import kotlinx.serialization.Serializable
import java.time.LocalDate
import java.time.YearMonth

@Serializable
data class Bill(
    val id: String,
    val title: String,
    val amount: String = "",
    val dayOfMonth: Int = 0,
    val billGenerationDay: Int = 0,
    val dueDay: Int = 0,
    val notes: String = "",
    val notify: Boolean = true,
) {
    fun generationDay(): Int = if (billGenerationDay in 1..31) billGenerationDay else 0

    fun paymentDueDay(): Int = when {
        dueDay in 1..31 -> dueDay
        dayOfMonth in 1..31 -> dayOfMonth
        else -> 0
    }
}

@Serializable
data class BillStore(
    val bills: List<Bill> = emptyList(),
)

enum class ReminderKind {
    FIVE_DAYS_BEFORE_BILL,
    ON_BILL_GENERATION,
    FIVE_DAYS_BEFORE_DUE,
    ON_DUE_DAY,
}

data class ReminderEvent(
    val kind: ReminderKind,
    val dayOfMonth: Int,
)

object BillReminders {
    fun kindsFor(today: LocalDate, dayOfMonth: Int): Set<ReminderKind> {
        if (dayOfMonth !in 1..31) return emptySet()
        val kinds = mutableSetOf<ReminderKind>()
        for (offset in 0L..1L) {
            val month = YearMonth.from(today).plusMonths(offset)
            val target = month.atDay(dayOfMonth.coerceAtMost(month.lengthOfMonth()))
            val fiveBefore = target.minusDays(5)
            if (today == target) kinds += ReminderKind.ON_BILL_GENERATION
            if (today == fiveBefore) kinds += ReminderKind.FIVE_DAYS_BEFORE_BILL
        }
        return kinds
    }

    fun eventsFor(today: LocalDate, bill: Bill): List<ReminderEvent> {
        val events = mutableListOf<ReminderEvent>()
        val generation = bill.generationDay()
        if (generation in 1..31) {
            matchDays(today, generation).forEach { onDay ->
                events += ReminderEvent(
                    kind = if (onDay) ReminderKind.ON_BILL_GENERATION else ReminderKind.FIVE_DAYS_BEFORE_BILL,
                    dayOfMonth = generation,
                )
            }
        }
        val due = bill.paymentDueDay()
        if (due in 1..31) {
            matchDays(today, due).forEach { onDay ->
                events += ReminderEvent(
                    kind = if (onDay) ReminderKind.ON_DUE_DAY else ReminderKind.FIVE_DAYS_BEFORE_DUE,
                    dayOfMonth = due,
                )
            }
        }
        return events
    }

    fun nextDate(today: LocalDate, dayOfMonth: Int): LocalDate? {
        if (dayOfMonth !in 1..31) return null
        val thisMonth = YearMonth.from(today)
        val thisDate = thisMonth.atDay(dayOfMonth.coerceAtMost(thisMonth.lengthOfMonth()))
        return if (!thisDate.isBefore(today)) thisDate else {
            val next = thisMonth.plusMonths(1)
            next.atDay(dayOfMonth.coerceAtMost(next.lengthOfMonth()))
        }
    }

    fun nextBillDate(today: LocalDate, dayOfMonth: Int): LocalDate {
        return nextDate(today, dayOfMonth) ?: today
    }

    private fun matchDays(today: LocalDate, dayOfMonth: Int): List<Boolean> {
        val hits = mutableListOf<Boolean>()
        for (offset in 0L..1L) {
            val month = YearMonth.from(today).plusMonths(offset)
            val target = month.atDay(dayOfMonth.coerceAtMost(month.lengthOfMonth()))
            val fiveBefore = target.minusDays(5)
            if (today == target) hits += true
            if (today == fiveBefore) hits += false
        }
        return hits
    }
}
