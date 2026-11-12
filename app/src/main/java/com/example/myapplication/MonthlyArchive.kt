package com.example.myapplication

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "monthly_archives")
data class MonthlyArchive(

    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val monthYear: String,

    val totalTransactions: Int,

    val totalCollections: Double,

    val totalIncome: Double,

    val totalExpenses: Double,

    val totalPersonalIncome: Double,

    val totalPersonalExpenses: Double,

    val totalRemittances: Double,

    val cashOnHand: Double,

    // ---- added in DB version 5 ----

    val totalBorrowed: Double = 0.0,

    val totalReturned: Double = 0.0,

    val gcashCashIn: Double = 0.0,

    val gcashCashOut: Double = 0.0,

    val gcashCharges: Double = 0.0
)