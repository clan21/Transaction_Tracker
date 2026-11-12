package com.example.myapplication

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

    @Composable
    fun HistoryScreen(
        transactions: List<Transaction>,
        dao: TransactionDao,
        searchText: String,
        showSearchBar: Boolean,
        onSearchTextChange: (String) -> Unit,
        modifier: Modifier = Modifier
    ) {
        val scope = rememberCoroutineScope()

        val tabs = listOf(
            "Recent",
            "Income",
            "Expenses",
            "Personal Income",
            "Personal Expense",
            "Remit"
        )

        var selectedTab by remember { mutableStateOf(0) }
        var currentPage by remember { mutableStateOf(0) }

        var expandedTransactionId by remember {
            mutableStateOf<Int?>(null)
        }

        var showDeleteDialog by remember {
            mutableStateOf(false)
        }

        var transactionToDelete by remember {
            mutableStateOf<Transaction?>(null)
        }

        var showEditDialog by remember {
            mutableStateOf(false)
        }

        var transactionToEdit by remember {
            mutableStateOf<Transaction?>(null)
        }

        val pageSize = 10

        // =========================
        // GROUP TRANSACTIONS
        // =========================

        val groupedTransactions = when (selectedTab) {

            // Recent
            0 -> transactions
                .sortedByDescending { it.id }
                .take(100)

            // Income
            1 -> transactions.filter {
                it.type == "income"
            }

            // Expenses
            2 -> transactions.filter {
                it.type == "expense"
            }

            // Personal Income
            3 -> transactions.filter {
                it.type == "personalIncome"
            }

            // Personal Expense
            4 -> transactions.filter {
                it.type == "personal"
            }

            // Remit
            else -> transactions.filter {
                it.type == "remit"
            }
        }

        // =========================
        // SEARCH FILTER
        // =========================

        val filteredTransactions = if (searchText.isBlank()) {

            groupedTransactions

        } else {

            val query = searchText.trim()

            groupedTransactions.filter { transaction ->

                transaction.details.contains(
                    query,
                    ignoreCase = true
                ) ||

                        transaction.amount
                            .toString()
                            .contains(
                                query,
                                ignoreCase = true
                            )
            }
        }

        // =========================
        // PAGINATION
        // =========================

        val pageCount = maxOf(
            1,
            (filteredTransactions.size + pageSize - 1) / pageSize
        )

        // Keep current page valid when search results change
        LaunchedEffect(
            selectedTab,
            searchText,
            filteredTransactions.size
        ) {
            currentPage = 0
        }

        val pagesPerGroup = 3

        val pageGroup = currentPage / pagesPerGroup

        val startPage = pageGroup * pagesPerGroup

        val endPage = minOf(
            startPage + pagesPerGroup,
            pageCount
        )

        val pageItems = filteredTransactions
            .drop(currentPage * pageSize)
            .take(pageSize)

        // =========================
        // SCREEN
        // =========================

        Column(
            modifier = modifier.fillMaxSize()
        ) {

            // =========================
            // SEARCH BAR
            // =========================

            if (showSearchBar) {

                OutlinedTextField(
                    value = searchText,

                    onValueChange = {
                        onSearchTextChange(it)
                    },

                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = 12.dp,
                            vertical = 8.dp
                        ),

                    singleLine = true,

                    placeholder = {
                        Text("Search amount or details")
                    },

                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search"
                        )
                    },

                    trailingIcon = {

                        if (searchText.isNotEmpty()) {

                            IconButton(
                                onClick = {
                                    onSearchTextChange("")
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Clear search"
                                )
                            }
                        }
                    }
                )
            }

            // =========================
            // TABS
            // =========================
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
                            currentPage = 0
                            expandedTransactionId = null
                        },

                        selectedContentColor = Color.Black,

                        unselectedContentColor =
                            MaterialTheme.colorScheme.onSurfaceVariant,

                        modifier = Modifier
                            .padding(
                                horizontal = 4.dp,
                                vertical = 6.dp
                            )
                            .background(
                                if (isSelected)
                                    Color(0xFF1c9999)
                                else
                                    Color.Transparent,
                                shape = RoundedCornerShape(8.dp)
                            ),

                        text = {
                            Text(
                                text = title,
                                maxLines = 1
                            )
                        }
                    )
                }
            }

            // =========================
            // TRANSACTION LIST
            // =========================

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .padding(12.dp),

                verticalArrangement =
                    Arrangement.spacedBy(8.dp)
            ) {

                items(pageItems) { transaction ->

                    TransactionRow(
                        transaction = transaction,

                        onEdit = {
                            transactionToEdit = transaction
                            showEditDialog = true
                            expandedTransactionId = null
                        },

                        onDelete = {
                            transactionToDelete = transaction
                            showDeleteDialog = true
                        },

                        isExpanded =
                            expandedTransactionId == transaction.id,

                        onExpand = {
                            expandedTransactionId = transaction.id
                        },

                        onCollapse = {
                            expandedTransactionId = null
                        },

                        onToggle = {
                            scope.launch {
                                dao.update(
                                    transaction.copy(
                                        isChecked = !transaction.isChecked
                                    )
                                )
                            }
                        }
                    )
                }

                // Show message when search has no results
                if (filteredTransactions.isEmpty()) {

                    item {

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),

                            contentAlignment =
                                Alignment.Center
                        ) {

                            Text(
                                text = if (searchText.isBlank())
                                    "No transactions"
                                else
                                    "No matching transactions"
                            )
                        }
                    }
                }
            }

            // =========================
            // PAGINATION
            // =========================

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),

                horizontalArrangement =
                    Arrangement.Center,

                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                // Back skip button
                if (startPage > 0) {

                    OutlinedButton(
                        onClick = {
                            currentPage = startPage - 1
                            expandedTransactionId = null
                        }
                    ) {
                        Text("<<")
                    }

                    Spacer(
                        Modifier.width(4.dp)
                    )
                }

                // Page numbers
                for (page in startPage until endPage) {

                    val isSelected =
                        page == currentPage

                    OutlinedButton(
                        onClick = {
                            currentPage = page
                            expandedTransactionId = null
                        },

                        modifier = Modifier.padding(
                            horizontal = 2.dp
                        ),

                        colors =
                            ButtonDefaults.outlinedButtonColors(
                                containerColor =
                                    if (isSelected)
                                        Color(0xFF1C9999)
                                    else
                                        Color.Transparent
                            )
                    ) {

                        Text(
                            text = "${page + 1}",

                            color =
                                if (isSelected)
                                    Color.White
                                else
                                    Color.Black
                        )
                    }
                }

                // Forward skip button
                if (endPage < pageCount) {

                    Spacer(
                        Modifier.width(4.dp)
                    )

                    OutlinedButton(
                        onClick = {
                            currentPage = endPage
                            expandedTransactionId = null
                        }
                    ) {
                        Text(">>")
                    }
                }
            }
        }

        // =========================
        // DELETE CONFIRMATION
        // =========================

        if (showDeleteDialog) {

            AlertDialog(
                onDismissRequest = {
                    showDeleteDialog = false
                    transactionToDelete = null
                },

                title = {
                    Text("Delete Transaction?")
                },

                text = {
                    Text(
                        "Are you sure you want to delete this transaction?"
                    )
                },

                confirmButton = {

                    TextButton(
                        onClick = {

                            transactionToDelete?.let { transaction ->

                                scope.launch {
                                    dao.delete(transaction)
                                }
                            }

                            showDeleteDialog = false
                            transactionToDelete = null
                            expandedTransactionId = null
                        }
                    ) {
                        Text(
                            "DELETE",
                            color = Color(0xFFF44336)
                        )
                    }
                },

                dismissButton = {

                    TextButton(
                        onClick = {
                            showDeleteDialog = false
                            transactionToDelete = null
                        }
                    ) {
                        Text("CANCEL")
                    }
                }
            )
        }
        if (showEditDialog) {

            transactionToEdit?.let { transaction ->

                EditTransactionDialog(
                    transaction = transaction,

                    onDismiss = {
                        showEditDialog = false
                        transactionToEdit = null
                    },

                    onSave = { amount, details, date, paymentMethod, expenseCategory ->

                        scope.launch {

                            dao.update(
                                transaction.copy(
                                    amount = amount,
                                    details = details,
                                    date = date,
                                    paymentMethod = paymentMethod,
                                    expenseCategory = expenseCategory
                                )
                            )

                            showEditDialog = false
                            transactionToEdit = null
                            expandedTransactionId = null
                        }
                    }
                )
            }
        }
    }