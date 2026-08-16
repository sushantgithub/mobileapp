package com.dueday.app.notify

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.dueday.app.MainActivity
import com.dueday.app.R
import com.dueday.app.data.BillReminders
import com.dueday.app.data.BillRepository
import com.dueday.app.data.ReminderKind
import java.time.LocalDate
import java.util.concurrent.TimeUnit

class BillReminderWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        ensureChannel(applicationContext)
        val repo = BillRepository(applicationContext)
        val prefs = applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val today = LocalDate.now()
        val todayKey = today.toString()
        repo.load().filter { it.notify }.forEach { bill ->
            BillReminders.kindsFor(today, bill.dayOfMonth).forEach { kind ->
                val stamp = "${bill.id}:${kind}:$todayKey"
                if (prefs.getBoolean(stamp, false)) return@forEach
                val (title, text) = when (kind) {
                    ReminderKind.FIVE_DAYS_BEFORE ->
                        "Bill in 5 days" to "${bill.title} generates on the ${ordinal(bill.dayOfMonth)}."
                    ReminderKind.ON_BILL_DAY ->
                        "Bill generates today" to "${bill.title} statement day is today."
                }
                val extra = if (bill.amount.isNotBlank()) " Amount: ${bill.amount}." else ""
                show(applicationContext, stamp.hashCode(), title, text + extra)
                prefs.edit().putBoolean(stamp, true).apply()
            }
        }
        pruneOldStamps(prefs, today)
        return Result.success()
    }

    companion object {
        private const val PREFS = "dueday_notify"
        private const val CHANNEL_ID = "bill_reminders"
        private const val WORK_NAME = "dueday-daily-reminders"

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<BillReminderWorker>(1, TimeUnit.DAYS)
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request,
            )
        }

        fun runNow(context: Context) {
            WorkManager.getInstance(context).enqueue(
                androidx.work.OneTimeWorkRequestBuilder<BillReminderWorker>().build(),
            )
        }

        fun ensureChannel(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "Bill reminders",
                    NotificationManager.IMPORTANCE_DEFAULT,
                )
                context.getSystemService(NotificationManager::class.java)
                    .createNotificationChannel(channel)
            }
        }

        private fun show(context: Context, id: Int, title: String, text: String) {
            val launch = PendingIntent.getActivity(
                context,
                0,
                Intent(context, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_stat_notify)
                .setContentTitle(title)
                .setContentText(text)
                .setContentIntent(launch)
                .setAutoCancel(true)
                .build()
            if (NotificationManagerCompat.from(context).areNotificationsEnabled()) {
                NotificationManagerCompat.from(context).notify(id, notification)
            }
        }

        private fun pruneOldStamps(prefs: android.content.SharedPreferences, today: LocalDate) {
            val cutoff = today.minusDays(45).toString()
            val stale = prefs.all.keys.filter { key ->
                key.substringAfterLast(":", missingDelimiterValue = "") < cutoff
            }
            if (stale.isNotEmpty()) {
                prefs.edit().apply {
                    stale.forEach { remove(it) }
                }.apply()
            }
        }

        private fun ordinal(day: Int): String {
            val suffix = when {
                day % 100 in 11..13 -> "th"
                day % 10 == 1 -> "st"
                day % 10 == 2 -> "nd"
                day % 10 == 3 -> "rd"
                else -> "th"
            }
            return "$day$suffix"
        }
    }
}
