package com.cardvault.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.cardvault.app.data.CardDates
import com.cardvault.app.data.CardKind
import com.cardvault.app.data.CardNumberFormatter
import com.cardvault.app.data.CardRecord

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardEditorScreen(
    existing: CardRecord?,
    onSave: (
        nickname: String,
        name: String,
        number: String,
        month: Int,
        year: Int,
        cvv: String,
        zip: String,
        notes: String,
        kind: CardKind,
        billGenerationDate: String,
        dueDate: String,
    ) -> Unit,
    onCancel: () -> Unit,
) {
    var nickname by remember { mutableStateOf(existing?.nickname.orEmpty()) }
    var name by remember { mutableStateOf(existing?.cardholderName.orEmpty()) }
    var number by remember { mutableStateOf(existing?.number.orEmpty()) }
    var month by remember { mutableStateOf(existing?.expiryMonth?.toString().orEmpty()) }
    var year by remember { mutableStateOf(existing?.expiryYear?.toString().orEmpty()) }
    var cvv by remember { mutableStateOf(existing?.cvv.orEmpty()) }
    var zip by remember { mutableStateOf(existing?.billingZip.orEmpty()) }
    var notes by remember { mutableStateOf(existing?.notes.orEmpty()) }
    var kind by remember { mutableStateOf(existing?.kind ?: CardKind.CREDIT) }
    var billGenerationDate by remember { mutableStateOf(existing?.billGenerationDate.orEmpty()) }
    var dueDate by remember { mutableStateOf(existing?.dueDate.orEmpty()) }
    var pickingBillDate by remember { mutableStateOf(false) }
    var pickingDueDate by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (existing == null) "Add card" else "Edit card") },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
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
            Text("Card type")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = kind == CardKind.CREDIT,
                    onClick = { kind = CardKind.CREDIT },
                    label = { Text("Credit card") },
                )
                FilterChip(
                    selected = kind == CardKind.DEBIT,
                    onClick = {
                        kind = CardKind.DEBIT
                        billGenerationDate = ""
                        dueDate = ""
                    },
                    label = { Text("Debit card") },
                )
            }
            OutlinedTextField(nickname, { nickname = it }, label = { Text("Nickname") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(name, { name = it }, label = { Text("Name on card") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(
                value = CardNumberFormatter.pretty(number),
                onValueChange = { number = CardNumberFormatter.digitsOnly(it).take(19) },
                label = { Text("Card number") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    month,
                    { month = it.filter(Char::isDigit).take(2) },
                    label = { Text("MM") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    year,
                    { year = it.filter(Char::isDigit).take(4) },
                    label = { Text("YYYY") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    cvv,
                    { cvv = it.filter(Char::isDigit).take(4) },
                    label = { Text("CVV") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    modifier = Modifier.weight(1f),
                )
            }
            if (kind == CardKind.CREDIT) {
                DateField(
                    label = "Bill generation date",
                    isoDate = billGenerationDate,
                    onPick = { pickingBillDate = true },
                    onClear = { billGenerationDate = "" },
                )
                DateField(
                    label = "Due date",
                    isoDate = dueDate,
                    onPick = { pickingDueDate = true },
                    onClear = { dueDate = "" },
                )
            }
            OutlinedTextField(zip, { zip = it }, label = { Text("Billing ZIP (optional)") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(notes, { notes = it }, label = { Text("Notes (optional)") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = {
                    onSave(
                        nickname,
                        name,
                        number,
                        month.toIntOrNull() ?: 0,
                        year.toIntOrNull() ?: 0,
                        cvv,
                        zip,
                        notes,
                        kind,
                        billGenerationDate,
                        dueDate,
                    )
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Save securely") }
            OutlinedButton(onClick = onCancel, modifier = Modifier.fillMaxWidth()) { Text("Cancel") }
        }
    }

    if (pickingBillDate) {
        VaultDatePickerDialog(
            onDismiss = { pickingBillDate = false },
            onConfirm = { millis ->
                billGenerationDate = CardDates.fromEpochMilliUtc(millis)
                pickingBillDate = false
            },
        )
    }
    if (pickingDueDate) {
        VaultDatePickerDialog(
            onDismiss = { pickingDueDate = false },
            onConfirm = { millis ->
                dueDate = CardDates.fromEpochMilliUtc(millis)
                pickingDueDate = false
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateField(
    label: String,
    isoDate: String,
    onPick: () -> Unit,
    onClear: () -> Unit,
) {
    OutlinedTextField(
        value = CardDates.display(isoDate),
        onValueChange = {},
        readOnly = true,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        trailingIcon = {
            Row {
                if (isoDate.isNotBlank()) {
                    TextButton(onClick = onClear) { Text("Clear") }
                }
                TextButton(onClick = onPick) { Text("Pick") }
            }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VaultDatePickerDialog(
    onDismiss: () -> Unit,
    onConfirm: (Long) -> Unit,
) {
    val state = rememberDatePickerState()
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    val millis = state.selectedDateMillis ?: return@TextButton
                    onConfirm(millis)
                },
            ) { Text("OK") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    ) {
        DatePicker(state = state)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardDetailScreen(
    card: CardRecord,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onCopy: (String, String) -> Unit,
) {
    var revealNumber by remember { mutableStateOf(false) }
    var revealCvv by remember { mutableStateOf(false) }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(card.nickname) },
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
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            CardPreview(card = card, onClick = {})
            Spacer(Modifier.height(8.dp))
            Text("Type: ${if (card.kind == CardKind.CREDIT) "Credit card" else "Debit card"}")
            Text("Name on card: ${card.cardholderName.ifBlank { "—" }}")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    if (revealNumber) CardNumberFormatter.pretty(card.number)
                    else CardNumberFormatter.mask(card.number),
                )
                TextButton(onClick = { revealNumber = !revealNumber }) {
                    Text(if (revealNumber) "Hide" else "Reveal")
                }
                TextButton(onClick = { onCopy("Card number", card.number) }) { Text("Copy") }
            }
            Text("Expiry: %02d/%d".format(card.expiryMonth, card.expiryYear))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("CVV: ${if (revealCvv) card.cvv else "•••"}")
                TextButton(onClick = { revealCvv = !revealCvv }) {
                    Text(if (revealCvv) "Hide" else "Reveal")
                }
                TextButton(onClick = { onCopy("CVV", card.cvv) }) { Text("Copy") }
            }
            if (card.kind == CardKind.CREDIT) {
                Text("Bill generation: ${CardDates.display(card.billGenerationDate).ifBlank { "—" }}")
                Text("Due date: ${CardDates.display(card.dueDate).ifBlank { "—" }}")
            }
            if (card.billingZip.isNotBlank()) Text("ZIP: ${card.billingZip}")
            if (card.notes.isNotBlank()) Text("Notes: ${card.notes}")
            Spacer(Modifier.height(16.dp))
            Button(onClick = onEdit, modifier = Modifier.fillMaxWidth()) { Text("Edit") }
            OutlinedButton(onClick = onDelete, modifier = Modifier.fillMaxWidth()) { Text("Delete card") }
        }
    }
}
