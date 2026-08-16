package com.cardvault.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onChangePin: (CharArray, CharArray, CharArray) -> Unit,
    onExport: (CharArray) -> Unit,
    onImport: () -> Unit,
    onWipe: () -> Unit,
    busy: Boolean,
) {
    var currentPin by remember { mutableStateOf("") }
    var newPin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var backupPassword by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Change PIN", style = MaterialTheme.typography.titleLarge)
            PinField("Current PIN", currentPin) { currentPin = it.filter(Char::isDigit).take(8) }
            PinField("New PIN", newPin) { newPin = it.filter(Char::isDigit).take(8) }
            PinField("Confirm new PIN", confirmPin) { confirmPin = it.filter(Char::isDigit).take(8) }
            Button(
                onClick = {
                    onChangePin(currentPin.toCharArray(), newPin.toCharArray(), confirmPin.toCharArray())
                    currentPin = ""; newPin = ""; confirmPin = ""
                },
                enabled = !busy,
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Update PIN") }

            Spacer(Modifier.height(16.dp))
            Text("Move to a new phone", style = MaterialTheme.typography.titleLarge)
            Text(
                "Export creates an encrypted .cvault file. Copy it to Drive, USB, or another app, then restore it on the new phone with the backup password.",
            )
            OutlinedTextField(
                value = backupPassword,
                onValueChange = { backupPassword = it },
                label = { Text("Backup password (min 8 characters)") },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth(),
            )
            Button(
                onClick = {
                    onExport(backupPassword.toCharArray())
                    backupPassword = ""
                },
                enabled = !busy && backupPassword.length >= 8,
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Export encrypted backup") }
            OutlinedButton(onClick = onImport, enabled = !busy, modifier = Modifier.fillMaxWidth()) {
                Text("Import backup")
            }

            Spacer(Modifier.height(16.dp))
            Text("Danger zone", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.error)
            OutlinedButton(onClick = onWipe, modifier = Modifier.fillMaxWidth()) {
                Text("Erase all local data")
            }
        }
    }
}

@Composable
fun RestoreScreen(
    busy: Boolean,
    onRestore: (CharArray, CharArray) -> Unit,
    onCancel: () -> Unit,
) {
    var password by remember { mutableStateOf("") }
    var pin by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text("Restore vault", style = MaterialTheme.typography.headlineLarge)
        Spacer(Modifier.height(8.dp))
        Text("Enter the backup password, then choose a PIN for this phone.")
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Backup password") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(12.dp))
        PinField("New device PIN", pin) { pin = it.filter(Char::isDigit).take(8) }
        Spacer(Modifier.height(12.dp))
        PinField("Confirm PIN", confirm) { confirm = it.filter(Char::isDigit).take(8) }
        Spacer(Modifier.height(20.dp))
        Button(
            onClick = {
                if (pin == confirm) onRestore(password.toCharArray(), pin.toCharArray())
            },
            enabled = !busy && password.length >= 8 && pin.length in 4..8 && pin == confirm,
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Decrypt and restore") }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = onCancel, modifier = Modifier.fillMaxWidth()) { Text("Cancel") }
    }
}
