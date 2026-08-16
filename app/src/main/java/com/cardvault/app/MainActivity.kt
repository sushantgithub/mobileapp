package com.cardvault.app

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.cardvault.app.data.VaultRepository
import com.cardvault.app.ui.CardVaultNav
import com.cardvault.app.ui.theme.CardVaultTheme

class MainActivity : ComponentActivity() {
    private val viewModel: VaultViewModel by viewModels {
        VaultViewModelFactory(VaultRepository(applicationContext))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
        lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStop(owner: LifecycleOwner) {
                viewModel.scheduleAutoLock()
            }

            override fun onStart(owner: LifecycleOwner) {
                viewModel.cancelAutoLock()
            }
        })
        setContent {
            CardVaultTheme {
                CardVaultNav(viewModel)
            }
        }
    }
}
