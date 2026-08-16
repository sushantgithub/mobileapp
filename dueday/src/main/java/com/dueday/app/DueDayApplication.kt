package com.dueday.app

import android.app.Application
import com.dueday.app.notify.BillReminderWorker

class DueDayApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        BillReminderWorker.ensureChannel(this)
        BillReminderWorker.schedule(this)
    }
}
