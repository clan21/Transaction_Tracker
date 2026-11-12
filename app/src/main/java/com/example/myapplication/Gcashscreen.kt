package com.example.myapplication

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
fun GcashScreen(
    gcash: List<GcashTransaction>,
    dao: GcashDao,
    searchText: String,
    showSearchBar: Boolean,
    onSearchTextChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()

    val tabs = listOf("Recent", "Cash-in", "Cash-out")
    var selectedTab by remember { mutableStateOf(0) }

    var showNewDialog by remember { mutableStateOf(false) }
    var expandedId by remember { mutableStateOf<Int?>(null) }
    var toEdit by remember { mutableStateOf<GcashTransaction?>(null) }
    var toDelete by remember { mutableStateOf<GcashTransaction?>(null) }

    val grouped = when (selectedTab) {
        0 -> gcash.sortedByDescending { it.id }.take(100)
        1 -> gcash.filter { it.type == GCASH_CASH_IN }
        else -> gcash.filter { it.type == GCASH_CASH_OUT }
    }

    val query = searchText.trim()
    val filtered = if (query.isBlank()) grouped else grouped.filter {
        it.details.contains(query, ignoreCase = true) ||
                it.amount.toString().contains(query, ignoreCase = true)
    }

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

        ScrollableTabRow(
            selectedTabIndex = selectedTab,
            edgePadding = 8.dp,
            containerColor = MaterialTheme.colorScheme.surface,
            divider = {}
        ) {
            tabs.forEachIndexed { index, title ->
                val isSelected = selectedTab == index

                Tab(
                    selected = isSelected,
                    onClick = {
                        selectedTab = index
                        expandedId = null
                    },
                    selectedContentColor = Color.Black,
                    unselectedContentColor =
                        MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .padding(horizontal = 4.dp, vertical = 6.dp)
                        .background(
                            if (isSelected) Color(0xFF1c9999)
                            else Color.Transparent,
                            shape = RoundedCornerShape(8.dp)
                        ),
                    text = { Text(text = title, maxLines = 1) }
                )
            }
        }

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(filtered, key = { it.id }) { item ->
                GcashRow(
                    item = item,
                    isExpanded = expandedId == item.id,
                    onExpand = { expandedId = item.id },
                    onCollapse = { expandedId = null },
                    onEdit = {
                        toEdit = item
                        expandedId = null
                    },
                    onDelete = { toDelete = item }
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
                            if (query.isBlank()) "No GCash transactions"
                            else "No matching transactions"
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
            Button(onClick = { showNewDialog = true }) {
                Text("New Transaction")
            }
        }
    }

    if (showNewDialog) {
        GcashDialog(
            initial = null,
            onDismiss = { showNewDialog = false },
            onConfirm = { amount, charge, details, date, type ->
                scope.launch {
                    dao.insert(
                        GcashTransaction(
                            amount = amount,
                            charge = charge,
                            details = details,
                            date = date,
                            type = type
                        )
                    )
                }
                showNewDialog = false
            }
        )
    }

    toEdit?.let { item ->
        GcashDialog(
            initial = item,
            onDismiss = { toEdit = null },
            onConfirm = { amount, charge, details, date, type ->
                scope.launch {
                    dao.update(
                        item.copy(
                            amount = amount,
                            charge = charge,
                            details = details,
                            date = date,
                            type = type
                        )
                    )
                }
                toEdit = null
            }
        )
    }

    toDelete?.let { item ->
        AlertDialog(
            onDismissRequest = { toDelete = null },
            title = { Text("Delete Transaction?") },
            text = { Text("Are you sure you want to delete this transaction?") },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch { dao.delete(item) }
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
private fun GcashRow(
    item: GcashTransaction,
    isExpanded: Boolean,
    onExpand: () -> Unit,
    onCollapse: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val isCashIn = item.type == GCASH_CASH_IN

    ExpandableActionRow(
        isExpanded = isExpanded,
        onExpand = onExpand,
        onCollapse = onCollapse,
        onEdit = onEdit,
        onDelete = onDelete
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(item.details)
                Text(
                    "${item.date} • ${if (isCashIn) "Cash-in" else "Cash-out"}",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    peso(item.amount),
                    color = if (isCashIn) Color(0xFF4CAF50) else Color(0xFFF44336)
                )
                Text(
                    "Charge ${peso(item.charge)}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GcashDialog(
    initial: GcashTransaction? = null,
    onDismiss: () -> Unit,
    onConfirm: (
        amount: Double,
        charge: Double,
        details: String,
        date: String,
        type: String
    ) -> Unit
) {
    var amountText by remember { mutableStateOf(initial?.amount?.toString() ?: "") }
    var chargeText by remember { mutableStateOf(initial?.charge?.toString() ?: "") }
    var details by remember { mutableStateOf(initial?.details ?: "") }
    var selectedDate by remember { mutableStateOf(initial?.date ?: todayString()) }
    var type by remember { mutableStateOf(initial?.type ?: GCASH_CASH_IN) }

    val decimalRegex = Regex("^\\d*\\.?\\d*$")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (initial == null) "New GCash Transaction" else "Edit GCash Transaction")
        },
        text = {
            Column {
                OutlinedTextField(
                    value = amountText,
                    onValueChange = {
                        if (it.matches(decimalRegex)) amountText = it
                    },
                    label = { Text("Amount") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal
                    )
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = chargeText,
                    onValueChange = {
                        if (it.matches(decimalRegex)) chargeText = it
                    },
                    label = { Text("Charge") },
                    singleLine = true,
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

                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = type == GCASH_CASH_IN,
                        onClick = { type = GCASH_CASH_IN }
                    )
                    Text("Cash-in")

                    Spacer(Modifier.width(16.dp))

                    RadioButton(
                        selected = type == GCASH_CASH_OUT,
                        onClick = { type = GCASH_CASH_OUT }
                    )
                    Text("Cash-out")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val amount = amountText.toDoubleOrNull()
                val charge = chargeText.toDoubleOrNull() ?: 0.0
                if (amount != null && amount > 0.0) {
                    onConfirm(amount, charge, details, selectedDate, type)
                }
            }) { Text(if (initial == null) "Confirm" else "Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}