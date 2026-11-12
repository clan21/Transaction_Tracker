package com.example.myapplication

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditTransactionDialog(
    transaction: Transaction,
    onDismiss: () -> Unit,
    onSave: (
        amount: Double,
        details: String,
        date: String,
        paymentMethod: String,
        expenseCategory: String?
    ) -> Unit
) {

    var amountText by remember {
        mutableStateOf(transaction.amount.toString())
    }

    var details by remember {
        mutableStateOf(transaction.details)
    }

    var selectedDate by remember {
        mutableStateOf(transaction.date)
    }

    var paymentMethod by remember {
        mutableStateOf(transaction.paymentMethod)
    }

    var expenseCategory by remember {
        mutableStateOf(
            transaction.expenseCategory ?: "Others"
        )
    }

    var paymentExpanded by remember {
        mutableStateOf(false)
    }

    var categoryExpanded by remember {
        mutableStateOf(false)
    }

    var showDatePicker by remember {
        mutableStateOf(false)
    }

    AlertDialog(

        onDismissRequest = onDismiss,

        // =========================
        // TITLE
        // =========================

        title = {
            Text("Edit Transaction")
        },

        // =========================
        // CONTENT
        // =========================

        text = {

            Column {

                // =========================
                // AMOUNT
                // =========================

                OutlinedTextField(
                    value = amountText,

                    onValueChange = { newValue ->

                        if (
                            newValue.matches(
                                Regex("^\\d*\\.?\\d*$")
                            )
                        ) {
                            amountText = newValue
                        }
                    },

                    label = {
                        Text("Amount")
                    },

                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal
                    ),

                    singleLine = true,

                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(
                    Modifier.height(8.dp)
                )

                // =========================
                // DETAILS
                // =========================

                OutlinedTextField(
                    value = details,

                    onValueChange = {
                        details = it
                    },

                    label = {
                        Text("Details")
                    },

                    singleLine = false,

                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(
                    Modifier.height(8.dp)
                )

                // =========================
                // DATE
                // =========================

                OutlinedTextField(
                    value = selectedDate,

                    onValueChange = {},

                    label = {
                        Text("Date")
                    },

                    readOnly = true,

                    trailingIcon = {

                        IconButton(
                            onClick = {
                                showDatePicker = true
                            }
                        ) {
                            Text("📅")
                        }
                    },

                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(
                    Modifier.height(8.dp)
                )

                // =========================
                // PAYMENT METHOD
                // =========================

                ExposedDropdownMenuBox(
                    expanded = paymentExpanded,

                    onExpandedChange = {
                        paymentExpanded = !paymentExpanded
                    }
                ) {

                    OutlinedTextField(
                        value = paymentMethod,

                        onValueChange = {},

                        readOnly = true,

                        label = {
                            Text("Payment Method")
                        },

                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(
                                expanded = paymentExpanded
                            )
                        },

                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )

                    ExposedDropdownMenu(
                        expanded = paymentExpanded,

                        onDismissRequest = {
                            paymentExpanded = false
                        }
                    ) {

                        DropdownMenuItem(
                            text = {
                                Text("Cash")
                            },

                            onClick = {
                                paymentMethod = "Cash"
                                paymentExpanded = false
                            }
                        )

                        DropdownMenuItem(
                            text = {
                                Text("GCash")
                            },

                            onClick = {
                                paymentMethod = "GCash"
                                paymentExpanded = false
                            }
                        )
                    }
                }

                // =========================
                // EXPENSE CATEGORY
                // =========================

                if (
                    transaction.type == "expense" ||
                    transaction.type == "personal"
                ) {

                    Spacer(
                        Modifier.height(8.dp)
                    )

                    ExposedDropdownMenuBox(
                        expanded = categoryExpanded,

                        onExpandedChange = {
                            categoryExpanded = !categoryExpanded
                        }
                    ) {

                        OutlinedTextField(
                            value = expenseCategory,

                            onValueChange = {},

                            readOnly = true,

                            label = {
                                Text("Expense Category")
                            },

                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(
                                    expanded = categoryExpanded
                                )
                            },

                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                        )

                        ExposedDropdownMenu(
                            expanded = categoryExpanded,

                            onDismissRequest = {
                                categoryExpanded = false
                            }
                        ) {

                            DropdownMenuItem(
                                text = {
                                    Text("Fuel")
                                },

                                onClick = {
                                    expenseCategory = "Fuel"
                                    categoryExpanded = false
                                }
                            )

                            DropdownMenuItem(
                                text = {
                                    Text("Salary")
                                },

                                onClick = {
                                    expenseCategory = "Salary"
                                    categoryExpanded = false
                                }
                            )

                            DropdownMenuItem(
                                text = {
                                    Text("Others")
                                },

                                onClick = {
                                    expenseCategory = "Others"
                                    categoryExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        },

        // =========================
        // SAVE BUTTON
        // =========================

        confirmButton = {

            TextButton(
                onClick = {

                    val amount =
                        amountText.toDoubleOrNull()

                    if (amount != null) {

                        onSave(
                            amount,
                            details,
                            selectedDate,
                            paymentMethod,

                            if (
                                transaction.type == "expense" ||
                                transaction.type == "personal"
                            ) {
                                expenseCategory
                            } else {
                                null
                            }
                        )
                    }
                }
            ) {
                Text("SAVE")
            }
        },

        // =========================
        // CANCEL BUTTON
        // =========================

        dismissButton = {

            TextButton(
                onClick = onDismiss
            ) {
                Text("CANCEL")
            }
        }
    )

    // =========================
    // DATE PICKER
    // =========================

    if (showDatePicker) {

        val datePickerState =
            rememberDatePickerState()

        DatePickerDialog(

            onDismissRequest = {
                showDatePicker = false
            },

            confirmButton = {

                TextButton(
                    onClick = {

                        datePickerState
                            .selectedDateMillis
                            ?.let { millis ->

                                val formatter =
                                    SimpleDateFormat(
                                        "MM/dd/yyyy",
                                        Locale.getDefault()
                                    )

                                selectedDate =
                                    formatter.format(
                                        Date(millis)
                                    )
                            }

                        showDatePicker = false
                    }
                ) {
                    Text("OK")
                }
            },

            dismissButton = {

                TextButton(
                    onClick = {
                        showDatePicker = false
                    }
                ) {
                    Text("Cancel")
                }
            }

        ) {

            DatePicker(
                state = datePickerState
            )
        }
    }
}