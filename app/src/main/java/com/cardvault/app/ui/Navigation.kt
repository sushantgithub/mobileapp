package com.cardvault.app.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.cardvault.app.VaultViewModel
import com.cardvault.app.shareBackup

@Composable
fun CardVaultNav(viewModel: VaultViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val navController = rememberNavController()
    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(state.message) {
        val message = state.message ?: return@LaunchedEffect
        snackbar.showSnackbar(message)
        viewModel.consumeMessage()
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
    ) { uri ->
        if (uri != null) {
            navController.navigate("restore?uri=${android.net.Uri.encode(uri.toString())}")
        }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbar) }) { padding ->
        NavHost(
            navController = navController,
            startDestination = if (!state.initialized) "setup" else if (!state.unlocked) "lock" else "home",
            modifier = Modifier.padding(padding),
        ) {
            composable("setup") {
                SetupScreen(
                    busy = state.busy,
                    onCreate = { pin, confirm -> viewModel.createPin(pin, confirm) },
                    onRestore = { importLauncher.launch("*/*") },
                )
            }
            composable("lock") {
                UnlockScreen(
                    busy = state.busy,
                    failedAttempts = state.failedAttempts,
                    onUnlock = { viewModel.unlock(it) },
                )
            }
            composable("home") {
                HomeScreen(
                    cards = state.cards,
                    onAdd = { navController.navigate("edit") },
                    onOpen = { id -> navController.navigate("detail/$id") },
                    onSettings = { navController.navigate("settings") },
                    onLock = { viewModel.lock() },
                )
            }
            composable("edit") {
                CardEditorScreen(
                    existing = null,
                    onSave = { nickname, name, number, month, year, cvv, zip, notes, kind, billDate, dueDate ->
                        if (viewModel.upsertCard(null, nickname, name, number, month, year, cvv, zip, notes, kind, billDate, dueDate)) {
                            navController.popBackStack()
                        }
                    },
                    onCancel = { navController.popBackStack() },
                )
            }
            composable(
                route = "edit/{id}",
                arguments = listOf(navArgument("id") { type = NavType.StringType }),
            ) { entry ->
                val id = entry.arguments?.getString("id")
                val card = state.cards.firstOrNull { it.id == id }
                CardEditorScreen(
                    existing = card,
                    onSave = { nickname, name, number, month, year, cvv, zip, notes, kind, billDate, dueDate ->
                        if (viewModel.upsertCard(id, nickname, name, number, month, year, cvv, zip, notes, kind, billDate, dueDate)) {
                            navController.popBackStack()
                        }
                    },
                    onCancel = { navController.popBackStack() },
                )
            }
            composable(
                route = "detail/{id}",
                arguments = listOf(navArgument("id") { type = NavType.StringType }),
            ) { entry ->
                val id = entry.arguments?.getString("id").orEmpty()
                val card = state.cards.firstOrNull { it.id == id }
                if (card == null) {
                    navController.popBackStack()
                } else {
                    CardDetailScreen(
                        card = card,
                        onBack = { navController.popBackStack() },
                        onEdit = { navController.navigate("edit/$id") },
                        onDelete = {
                            viewModel.deleteCard(id)
                            navController.popBackStack()
                        },
                        onCopy = { label, value -> viewModel.copyThenClear(context, label, value) },
                    )
                }
            }
            composable("settings") {
                SettingsScreen(
                    onBack = { navController.popBackStack() },
                    onChangePin = { current, next, confirm -> viewModel.changePin(current, next, confirm) },
                    onExport = { password ->
                        val uri = viewModel.exportBackup(context, password)
                        if (uri != null) shareBackup(context, uri)
                    },
                    onImport = { importLauncher.launch("*/*") },
                    onWipe = {
                        viewModel.wipeVault()
                        navController.navigate("setup") {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                    busy = state.busy,
                )
            }
            composable(
                route = "restore?uri={uri}",
                arguments = listOf(
                    navArgument("uri") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    },
                ),
            ) { entry ->
                val uriString = entry.arguments?.getString("uri")
                RestoreScreen(
                    busy = state.busy,
                    onRestore = { password, pin ->
                        if (uriString == null) return@RestoreScreen
                        val uri = android.net.Uri.parse(uriString)
                        val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                        if (bytes == null) return@RestoreScreen
                        viewModel.importBackup(bytes, password, pin)
                    },
                    onCancel = { navController.popBackStack() },
                )
            }
        }
    }

    LaunchedEffect(state.unlocked, state.initialized) {
        val dest = when {
            !state.initialized -> "setup"
            !state.unlocked -> "lock"
            else -> "home"
        }
        val current = navController.currentDestination?.route
        if (current != dest && (dest == "setup" || dest == "lock" || (dest == "home" && current in setOf("setup", "lock", null)))) {
            navController.navigate(dest) {
                popUpTo(0) { inclusive = true }
            }
        }
    }
}
