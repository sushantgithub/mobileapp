package com.dueday.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.dueday.app.BillViewModel
import com.dueday.app.data.Bill
import com.dueday.app.data.BillReminders
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun DueDayApp(viewModel: BillViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val nav = rememberNavController()
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(state.message) {
        val message = state.message ?: return@LaunchedEffect
        snackbar.showSnackbar(message)
        viewModel.consumeMessage()
    }
    Scaffold(snackbarHost = { SnackbarHost(snackbar) }) { padding ->
        NavHost(
            navController = nav,
            startDestination = "home",
            modifier = Modifier.padding(padding),
        ) {
            composable("home") {
                HomeScreen(
                    bills = state.bills,
                    onAdd = { nav.navigate("edit") },
                    onOpen = { nav.navigate("edit/${it}") },
                )
            }
            composable("edit") {
                EditorScreen(
                    existing = null,
                    onSave = { title, amount, billDay, dueDay, notes, notify ->
                        if (viewModel.upsert(null, title, amount, billDay, dueDay, notes, notify)) nav.popBackStack()
                    },
                    onCancel = { nav.popBackStack() },
                    onDelete = null,
                )
            }
            composable(
                "edit/{id}",
                arguments = listOf(navArgument("id") { type = NavType.StringType }),
            ) { entry ->
                val id = entry.arguments?.getString("id")
                val bill = state.bills.firstOrNull { it.id == id }
                EditorScreen(
                    existing = bill,
                    onSave = { title, amount, billDay, dueDay, notes, notify ->
                        if (viewModel.upsert(id, title, amount, billDay, dueDay, notes, notify)) nav.popBackStack()
                    },
                    onCancel = { nav.popBackStack() },
                    onDelete = {
                        if (id != null) viewModel.delete(id)
                        nav.popBackStack()
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeScreen(
    bills: List<Bill>,
    onAdd: () -> Unit,
    onOpen: (String) -> Unit,
) {
    Scaffold(
        topBar = { TopAppBar(title = { Text("DueDay") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = onAdd) {
                Icon(Icons.Outlined.Add, contentDescription = "Add bill")
            }
        },
    ) { padding ->
        if (bills.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("Add rent, cards, or any monthly bill.\nSet bill generation day and due day.\nYou’ll get reminders 5 days before and on each day.")
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(bills, key = { it.id }) { bill ->
                    val nextBill = BillReminders.nextDate(LocalDate.now(), bill.generationDay())
                    val nextDue = BillReminders.nextDate(LocalDate.now(), bill.paymentDueDay())
                    Card(Modifier.fillMaxWidth().clickable { onOpen(bill.id) }) {
                        Column(Modifier.padding(16.dp)) {
                            Text(bill.title, style = MaterialTheme.typography.titleLarge)
                            if (bill.generationDay() in 1..31) {
                                Text("Bill generation: ${ordinal(bill.generationDay())} of every month")
                                if (nextBill != null) {
                                    Text("Next bill: ${nextBill.format(DateTimeFormatter.ofPattern("d MMM yyyy"))}")
                                }
                            }
                            if (bill.paymentDueDay() in 1..31) {
                                Text("Due: ${ordinal(bill.paymentDueDay())} of every month")
                                if (nextDue != null) {
                                    Text("Next due: ${nextDue.format(DateTimeFormatter.ofPattern("d MMM yyyy"))}")
                                }
                            }
                            if (bill.amount.isNotBlank()) Text("Amount: ${bill.amount}")
                            if (!bill.notify) Text("Reminders off")
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditorScreen(
    existing: Bill?,
    onSave: (String, String, Int, Int, String, Boolean) -> Unit,
    onCancel: () -> Unit,
    onDelete: (() -> Unit)?,
) {
    var title by remember { mutableStateOf(existing?.title.orEmpty()) }
    var amount by remember { mutableStateOf(existing?.amount.orEmpty()) }
    var billDay by remember { mutableIntStateOf(existing?.generationDay() ?: 0) }
    var dueDay by remember { mutableIntStateOf(existing?.paymentDueDay() ?: 0) }
    var notes by remember { mutableStateOf(existing?.notes.orEmpty()) }
    var notify by remember { mutableStateOf(existing?.notify ?: true) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (existing == null) "Add bill" else "Edit bill") },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedTextField(title, { title = it }, label = { Text("Name (e.g. HDFC Millennia)") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(
                amount,
                { amount = it },
                label = { Text("Amount (optional)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
            )
            DayOfMonthField("Bill generation day", billDay) { billDay = it }
            DayOfMonthField("Due day", dueDay) { dueDay = it }
            OutlinedTextField(notes, { notes = it }, label = { Text("Notes (optional)") }, modifier = Modifier.fillMaxWidth())
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Remind 5 days before and on each day", modifier = Modifier.weight(1f))
                Switch(checked = notify, onCheckedChange = { notify = it })
            }
            Spacer(Modifier.height(8.dp))
            Button(onClick = { onSave(title, amount, billDay, dueDay, notes, notify) }, modifier = Modifier.fillMaxWidth()) {
                Text("Save")
            }
            OutlinedButton(onClick = onCancel, modifier = Modifier.fillMaxWidth()) { Text("Cancel") }
            if (onDelete != null) {
                OutlinedButton(onClick = onDelete, modifier = Modifier.fillMaxWidth()) { Text("Delete") }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DayOfMonthField(
    label: String,
    day: Int,
    onDayChange: (Int) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded, { expanded = it }) {
        OutlinedTextField(
            value = if (day in 1..31) "${ordinal(day)} of every month" else "Not set",
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor(),
        )
        ExposedDropdownMenu(expanded, { expanded = false }) {
            DropdownMenuItem(
                text = { Text("Not set") },
                onClick = {
                    onDayChange(0)
                    expanded = false
                },
            )
            (1..31).forEach { value ->
                DropdownMenuItem(
                    text = { Text("${ordinal(value)} of every month") },
                    onClick = {
                        onDayChange(value)
                        expanded = false
                    },
                )
            }
        }
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
