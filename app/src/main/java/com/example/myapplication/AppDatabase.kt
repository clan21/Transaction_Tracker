package com.example.myapplication

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        Transaction::class,
        MonthlyArchive::class,
        LendingEntry::class,
        GcashTransaction::class
    ],
    version = 6
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao

    abstract fun monthlyArchiveDao(): MonthlyArchiveDao

    abstract fun lendingDao(): LendingDao

    abstract fun gcashDao(): GcashDao
}