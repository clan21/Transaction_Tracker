package com.example.myapplication

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

const val GCASH_CASH_IN = "cashIn"
const val GCASH_CASH_OUT = "cashOut"

@Entity(tableName = "gcash_transactions")
data class GcashTransaction(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val amount: Double,
    val charge: Double,
    val details: String,
    val date: String,
    val type: String // GCASH_CASH_IN or GCASH_CASH_OUT
)

@Dao
interface GcashDao {

    @Insert
    suspend fun insert(transaction: GcashTransaction)

    @Update
    suspend fun update(transaction: GcashTransaction)

    @Delete
    suspend fun delete(transaction: GcashTransaction)

    @Query("SELECT * FROM gcash_transactions ORDER BY id DESC")
    fun getAll(): Flow<List<GcashTransaction>>

    @Query("DELETE FROM gcash_transactions")
    suspend fun deleteAll()
}