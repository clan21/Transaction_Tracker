package com.example.myapplication

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@Composable
fun LendingScreen(
    lending: List<LendingEntry>,
    dao: LendingDao,
    searchText: String,
    showSearchBar: Boolean,
    onSearchTextChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()

    var showNewDialog by remember { mutableStateOf(false) }
    var selectedId by remember { mutableStateOf<Int?>(null) }
    var expandedId by remember { mutableStateOf<Int?>(null) }
    var toEdit by remember { mutableStateOf<LendingEntry?>(null) }
    var toDelete by remember { mutableStateOf<LendingEntry?>(null) }

    // Borrowed = everything lent out. Returns do NOT reduce it.
    val borrowedTotal = lending.sumOf { it.amount }
    val returnedTotal = lending.sumOf { it.returnedAmount }

    val query = searchText.trim()
    val filtered = if (query.isBlank()) lending else lending.filter {
        it.details.contains(query, ignoreCase = true) ||
                it.amount.toString().contains(query, ignoreCase = true)
    }

    // Always read the latest copy of the selected entry
    val selectedEntry = lending.firstOrNull { it.id == selectedId }

    Column(modifier = modifier.fillMaxSize()) {

        if (showSearchBar) {
            OutlinedTextField(
                value = searchText,
                onValueChange = onSearchTextChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                singleLine = true,
                placeholder = { Text("Search amount or details") },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = "Search")
                },
                trailingIcon = {
                    if (searchText.isNotEmpty()) {
                        IconButton(onClick = { onSearchTextChange("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear search")
                        }
                    }
                }
            )
        }

        // Cards
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            MoneyCard("Borrowed", borrowedTotal, Modifier.weight(1f))
            Spacer(Modifier.width(12.dp))
            MoneyCard("Returned", returnedTotal, Modifier.weight(1f))
        }

        // List
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(filtered, key = { it.id }) { entry ->
                LendingRow(
                    entry = entry,
                    isExpanded = expandedId == entry.id,
                    onClick = { selectedId = entry.id },
                    onExpand = { expandedId = entry.id },
                    onCollapse = { expandedId = null },
                    onEdit = {
                        toEdit = entry
                        expandedId = null
                    },
                    onDelete = { toDelete = entry }
                )
            }

            if (filtered.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            if (query.isBlank()) "No borrowed entries"
                            else "No matching entries"
                        )
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            contentAlignment = Alignment.Center
        ) {
            Button(
                onClick = { showNewDialog = true },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFF44336)
                )
            ) {
                Text("New Borrowed")
            }
        }
    }

    if (showNewDialog) {
        BorrowedDialog(
            entry = null,
            onDismiss = { showNewDialog = false },
            onConfirm = { amount, details, date, paymentMethod ->
                scope.launch {
                    dao.insert(
                        LendingEntry(
                            details = details,
                            date = date,
                            amount = amount,
                            paymentMethod = paymentMethod
                        )
                    )
                }
                showNewDialog = false
            }
        )
    }

    selectedEntry?.let { entry ->
        ReturnDialog(
            entry = entry,
            onDismiss = { selectedId = null },
            onPay = { paid, method ->
                scope.launch {
                    dao.update(
                        entry.copy(
                            returnedAmount = entry.returnedAmount + paid,
                            returnedGcash = entry.returnedGcash +
                                    if (method == "GCash") paid else 0.0
                        )
                    )
                }
            }
        )
    }

    toEdit?.let { entry ->
        BorrowedDialog(
            entry = entry,
            onDismiss = { toEdit = null },
            onConfirm = { amount, details, date, paymentMethod ->
                scope.launch {
                    dao.update(
                        entry.copy(
                            amount = amount,
                            details = details,
                            date = date,
                            paymentMethod = paymentMethod
                        )
                    )
                }
                toEdit = null
            }
        )
    }

    toDelete?.let { entry ->
        AlertDialog(
            onDismissRequest = { toDelete = null },
            title = { Text("Delete Entry?") },
            text = {
                Text("Are you sure you want to delete this entry? Its returned payments will be removed too.")
            },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch { dao.delete(entry) }
                    toDelete = null
                    expandedId = null
                }) { Text("DELETE", color = Color(0xFFF44336)) }
            },
            dismissButton = {
                TextButton(onClick = { toDelete = null }) { Text("CANCEL") }
            }
        )
    }
}

@Composable
private fun LendingRow(
    entry: LendingEntry,
    isExpanded: Boolean,
    onClick: () -> Unit,
    onExpand: () -> Unit,
    onCollapse: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val statusText = when (entry.status) {
        "Returned" -> "Returned"
        "Incomplete" -> "Incomplete • Balance ${peso(entry.balance)}"
        else -> "Borrowed"
    }

    val statusColor = when (entry.status) {
        "Returned" -> Color(0xFF4CAF50)
        "Incomplete" -> Color(0xFFFF9800)
        else -> Color(0xFFF44336)
    }

    ExpandableActionRow(
        isExpanded = isExpanded,
        onExpand = onExpand,
        onCollapse = onCollapse,
        onEdit = onEdit,
        onDelete = onDelete,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(entry.details)
                Text(
                    "${entry.date} • ${entry.paymentMethod}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Text(peso(entry.amount))
        }

        Text(
            text = statusText,
            color = statusColor,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
private fun PaymentMethodRow(
    paymentMethod: String,
    onChange: (String) -> Unit
) {
    Text(
        text = "Payment Method",
        style = MaterialTheme.typography.bodyLarge
    )

    Row(verticalAlignment = Alignment.CenterVertically) {
        RadioButton(
            selected = paymentMethod == "Cash",
            onClick = { onChange("Cash") }
        )
        Text("Cash")

        Spacer(Modifier.width(16.dp))

        RadioButton(
            selected = paymentMethod == "GCash",
            onClick = { onChange("GCash") }
        )
        Text("GCash")
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BorrowedDialog(
    entry: LendingEntry? = null,
    onDismiss: () -> Unit,
    onConfirm: (
        amount: Double,
        details: String,
        date: String,
        paymentMethod: String
    ) -> Unit
) {
    var amountText by remember { mutableStateOf(entry?.amount?.toString() ?: "") }
    var details by remember { mutableStateOf(entry?.details ?: "") }
    var selectedDate by remember { mutableStateOf(entry?.date ?: todayString()) }
    var paymentMethod by remember { mutableStateOf(entry?.paymentMethod ?: "Cash") }

    val parsedAmount = amountText.toDoubleOrNull()
    // An edited amount can't drop below what has already been returned
    val belowReturned = entry != null &&
            parsedAmount != null &&
            parsedAmount < entry.returnedAmount - 0.005

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (entry == null) "New Borrowed" else "Edit Borrowed") },
        text = {
            Column {
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { newValue ->
                        if (newValue.matches(Regex("^\\d*\\.?\\d*$"))) {
                            amountText = newValue
                        }
                    },
                    label = { Text("Amount") },
                    isError = belowReturned,
                    supportingText = {
                        if (belowReturned && entry != null) {
                            Text("Already returned ${peso(entry.returnedAmount)}")
                        }
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal
                    )
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = details,
                    onValueChange = { details = it },
                    label = { Text("Details") }
                )
                Spacer(Modifier.height(8.dp))
                DateField(
                    date = selectedDate,
                    onDateChange = { selectedDate = it }
                )
                Spacer(Modifier.height(8.dp))
                PaymentMethodRow(
                    paymentMethod = paymentMethod,
                    onChange = { paymentMethod = it }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val amount = amountText.toDoubleOrNull()
                if (amount != null && amount > 0.0 && !belowReturned) {
                    onConfirm(amount, details, selectedDate, paymentMethod)
                }
            }) { Text(if (entry == null) "Add" else "Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun ReturnDialog(
    entry: LendingEntry,
    onDismiss: () -> Unit,
    onPay: (amount: Double, paymentMethod: String) -> Unit
) {
    var payText by remember(entry.id) { mutableStateOf("") }
    var payMethod by remember(entry.id) { mutableStateOf("Cash") }

    val payAmount = payText.toDoubleOrNull()
    val tooMuch = payAmount != null && payAmount > entry.balance + 0.005

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Borrowed Details") },
        text = {
            Column {
                Text("Details: ${entry.details}")
                Text("Date: ${entry.date}")
                Text("Amount: ${peso(entry.amount)}")
                Text("Lent via: ${entry.paymentMethod}")

                if (entry.returnedAmount > 0.0) {
                    Text("Returned: ${peso(entry.returnedAmount)}")
                    Text("Balance: ${peso(entry.balance)}")
                }

                Spacer(Modifier.height(4.dp))
                Text(
                    "Status: ${entry.status}",
                    style = MaterialTheme.typography.titleSmall
                )

                if (entry.status != "Returned") {
                    Spacer(Modifier.height(12.dp))

                    OutlinedTextField(
                        value = payText,
                        onValueChange = { newValue ->
                            if (newValue.matches(Regex("^\\d*\\.?\\d*$"))) {
                                payText = newValue
                            }
                        },
                        label = { Text("Enter returned amount") },
                        singleLine = true,
                        isError = tooMuch,
                        supportingText = {
                            if (tooMuch) {
                                Text("Cannot be more than ${peso(entry.balance)}")
                            }
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Decimal
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(8.dp))

                    PaymentMethodRow(
                        paymentMethod = payMethod,
                        onChange = { payMethod = it }
                    )

                    Spacer(Modifier.height(8.dp))

                    Button(
                        onClick = {
                            if (payAmount != null && payAmount > 0.0 && !tooMuch) {
                                onPay(payAmount, payMethod)
                                payText = ""
                            }
                        },
                        enabled = payAmount != null && payAmount > 0.0 && !tooMuch,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Pay")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("CLOSE") }
        }
    )
}