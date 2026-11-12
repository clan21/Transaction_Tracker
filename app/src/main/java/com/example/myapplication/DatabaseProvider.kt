package com.example.myapplication

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object DatabaseProvider {
    @Volatile private var INSTANCE: AppDatabase? = null

    fun getDatabase(context: Context): AppDatabase {
        return INSTANCE ?: synchronized(this) {
            val instance = Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "kazibufast_db"
            )
                .addMigrations(MIGRATION_1_2)
                .addMigrations(MIGRATION_2_3)
                .addMigrations(MIGRATION_3_4)
                .addMigrations(MIGRATION_4_5)
                .addMigrations(MIGRATION_5_6)
                .build()

            INSTANCE = instance
            instance
        }
    }
}


// =========================
// MIGRATION 2 → 3
// =========================

val MIGRATION_2_3 = object : Migration(2, 3) {

    override fun migrate(
        database: SupportSQLiteDatabase
    ) {

        database.execSQL(
            "ALTER TABLE transactions ADD COLUMN paymentMethod TEXT NOT NULL DEFAULT 'Cash'"
        )

        database.execSQL(
            "ALTER TABLE transactions ADD COLUMN expenseCategory TEXT"
        )
    }
}

val MIGRATION_3_4 = object : Migration(3, 4) {

    override fun migrate(
        database: SupportSQLiteDatabase
    ) {

        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS monthly_archives (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                monthYear TEXT NOT NULL,
                totalTransactions INTEGER NOT NULL,
                totalCollections REAL NOT NULL,
                totalIncome REAL NOT NULL,
                totalExpenses REAL NOT NULL,
                totalPersonalIncome REAL NOT NULL,
                totalPersonalExpenses REAL NOT NULL,
                totalRemittances REAL NOT NULL,
                cashOnHand REAL NOT NULL
            )
            """.trimIndent()
        )
    }
}

// =========================
// MIGRATION 4 → 5  (Lending + GCash)
// =========================

val MIGRATION_4_5 = object : Migration(4, 5) {

    override fun migrate(
        database: SupportSQLiteDatabase
    ) {

        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS lending (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                details TEXT NOT NULL,
                date TEXT NOT NULL,
                amount REAL NOT NULL,
                returnedAmount REAL NOT NULL
            )
            """.trimIndent()
        )

        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS gcash_transactions (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                amount REAL NOT NULL,
                charge REAL NOT NULL,
                details TEXT NOT NULL,
                date TEXT NOT NULL,
                type TEXT NOT NULL
            )
            """.trimIndent()
        )

        // New columns on the saved monthly records
        listOf(
            "totalBorrowed",
            "totalReturned",
            "gcashCashIn",
            "gcashCashOut",
            "gcashCharges"
        ).forEach { column ->
            database.execSQL(
                "ALTER TABLE monthly_archives ADD COLUMN $column REAL NOT NULL DEFAULT 0"
            )
        }
    }
}

// =========================
// MIGRATION 5 → 6  (payment method on lending)
// =========================

val MIGRATION_5_6 = object : Migration(5, 6) {

    override fun migrate(
        database: SupportSQLiteDatabase
    ) {
        // Existing entries are treated as lent in Cash
        database.execSQL(
            "ALTER TABLE lending ADD COLUMN paymentMethod TEXT NOT NULL DEFAULT 'Cash'"
        )
        database.execSQL(
            "ALTER TABLE lending ADD COLUMN returnedGcash REAL NOT NULL DEFAULT 0"
        )
        database.execSQL(
            "ALTER TABLE lending ADD COLUMN carriedOver INTEGER NOT NULL DEFAULT 0"
        )
    }
}