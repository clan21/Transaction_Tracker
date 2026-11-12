package com.example.myapplication

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val amount: Double,
    val details: String,
    val date: String,
    val type: String, // "income", "expense", or "personal"
    val isChecked: Boolean = false,
    val paymentMethod: String = "Cash",
    val expenseCategory: String? = null
)