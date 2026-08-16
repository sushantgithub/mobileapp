package com.cardvault.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.cardvault.app.ui.theme.Gold

@Composable
fun SetupScreen(
    busy: Boolean,
    onCreate: (CharArray, CharArray) -> Unit,
    onRestore: () -> Unit,
) {
    var pin by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(Icons.Outlined.Lock, contentDescription = null, tint = Gold, modifier = Modifier.size(56.dp))
        Spacer(Modifier.height(16.dp))
        Text("Card Vault", style = MaterialTheme.typography.headlineLarge)
        Text(
            "Create a PIN to encrypt cards on this phone. Nothing is uploaded.",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(top = 8.dp, bottom = 24.dp),
        )
        PinField("PIN (4–8 digits)", pin) { pin = it.filter(Char::isDigit).take(8) }
        Spacer(Modifier.height(12.dp))
        PinField("Confirm PIN", confirm) { confirm = it.filter(Char::isDigit).take(8) }
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = { onCreate(pin.toCharArray(), confirm.toCharArray()) },
            enabled = !busy && pin.length in 4..8,
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (busy) CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
            else Text("Create encrypted vault")
        }
        Spacer(Modifier.height(12.dp))
        OutlinedButton(onClick = onRestore, enabled = !busy, modifier = Modifier.fillMaxWidth()) {
            Text("Restore from backup")
        }
    }
}

@Composable
fun UnlockScreen(
    busy: Boolean,
    failedAttempts: Int,
    onUnlock: (CharArray) -> Unit,
) {
    var pin by remember { mutableStateOf("") }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(Icons.Outlined.Lock, contentDescription = null, tint = Gold, modifier = Modifier.size(56.dp))
        Spacer(Modifier.height(16.dp))
        Text("Unlock vault", style = MaterialTheme.typography.headlineLarge)
        Text("Enter your PIN to decrypt cards stored on this device.")
        Spacer(Modifier.height(24.dp))
        PinDots(pin.length)
        Spacer(Modifier.height(16.dp))
        PinField("PIN", pin) {
            pin = it.filter(Char::isDigit).take(8)
        }
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = {
                onUnlock(pin.toCharArray())
                pin = ""
            },
            enabled = !busy && pin.length in 4..8,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Unlock")
        }
        if (failedAttempts > 0) {
            Text(
                "Failed attempts: $failedAttempts",
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 12.dp),
            )
        }
    }
}

@Composable
fun PinField(label: String, value: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
fun PinDots(count: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        repeat(8) { index ->
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .background(
                        color = if (index < count) Gold else MaterialTheme.colorScheme.surface,
                        shape = CircleShape,
                    ),
            )
        }
    }
}
