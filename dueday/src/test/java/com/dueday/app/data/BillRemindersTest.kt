package com.dueday.app.data

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BillRemindersTest {
    @Test
    fun notifiesOnBillDay() {
        val kinds = BillReminders.kindsFor(LocalDate.of(2026, 8, 6), 6)
        assertEquals(setOf(ReminderKind.ON_BILL_DAY), kinds)
    }

    @Test
    fun notifiesFiveDaysBefore() {
        val kinds = BillReminders.kindsFor(LocalDate.of(2026, 8, 1), 6)
        assertEquals(setOf(ReminderKind.FIVE_DAYS_BEFORE), kinds)
    }

    @Test
    fun wrapsIntoPreviousMonth() {
        val kinds = BillReminders.kindsFor(LocalDate.of(2026, 7, 29), 3)
        assertTrue(kinds.contains(ReminderKind.FIVE_DAYS_BEFORE))
    }

    @Test
    fun nextBillStaysThisMonthIfUpcoming() {
        val next = BillReminders.nextBillDate(LocalDate.of(2026, 8, 2), 6)
        assertEquals(LocalDate.of(2026, 8, 6), next)
    }

    @Test
    fun nextBillMovesToNextMonthIfPassed() {
        val next = BillReminders.nextBillDate(LocalDate.of(2026, 8, 7), 6)
        assertEquals(LocalDate.of(2026, 9, 6), next)
    }
}
