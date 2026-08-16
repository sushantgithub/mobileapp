package com.dueday.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dueday.app.notify.BillReminderWorker
import com.dueday.app.ui.DueDayApp
import com.dueday.app.ui.theme.DueDayTheme

class MainActivity : ComponentActivity() {
    private val permission = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { /* user can deny; reminders simply will not show */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestNotifications()
        BillReminderWorker.schedule(this)
        BillReminderWorker.runNow(this)
        setContent {
            DueDayTheme {
                DueDayApp(viewModel = viewModel(factory = BillViewModel.factory(applicationContext)))
            }
        }
    }

    private fun requestNotifications() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) permission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
