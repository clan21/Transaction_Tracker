package com.example.myapplication

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * Money YOU lent to someone.
 * amount         = original amount lent (or the carried-over balance after a wrap up)
 * paymentMethod  = how it was lent out: "Cash" or "GCash"
 * returnedAmount = total paid back so far (cash + GCash)
 * returnedGcash  = the part of returnedAmount that came back through GCash
 * carriedOver    = true if this is an unpaid balance carried in from a previous month
 *                  (its original lending was already deducted last month)
 */
@Entity(tableName = "lending")
data class LendingEntry(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val details: String,
    val date: String,
    val amount: Double,
    val returnedAmount: Double = 0.0,
    val paymentMethod: String = "Cash",
    val returnedGcash: Double = 0.0,
    val carriedOver: Boolean = false
) {
    val balance: Double
        get() = (amount - returnedAmount).coerceAtLeast(0.0)

    val status: String
        get() = when {
            returnedAmount >= amount - 0.005 -> "Returned"
            returnedAmount > 0.0 -> "Incomplete"
            else -> "Borrowed"
        }
}

@Dao
interface LendingDao {

    @Insert
    suspend fun insert(entry: LendingEntry)

    @Update
    suspend fun update(entry: LendingEntry)

    @Delete
    suspend fun delete(entry: LendingEntry)

    @Query("SELECT * FROM lending ORDER BY id DESC")
    fun getAll(): Flow<List<LendingEntry>>

    // ---- used by Wrap Up ----

    @Query("DELETE FROM lending WHERE returnedAmount >= amount - 0.005")
    suspend fun deleteFullyReturned()

    // Unpaid balances carry over to the next month as fresh entries
    @Query(
        "UPDATE lending SET amount = amount - returnedAmount, " +
                "returnedAmount = 0, returnedGcash = 0, carriedOver = 1 " +
                "WHERE returnedAmount < amount - 0.005"
    )
    suspend fun rollOver()
}