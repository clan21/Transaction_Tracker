package com.example.myapplication

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MonthlyArchiveDao {

    @Insert
    suspend fun insert(archive: MonthlyArchive)

    @Query(
        "SELECT * FROM monthly_archives ORDER BY id DESC"
    )
    fun getAllArchives(): Flow<List<MonthlyArchive>>

    @Query(
        "SELECT * FROM monthly_archives WHERE id = :id"
    )
    suspend fun getArchiveById(id: Int): MonthlyArchive?

    @Query(
        "DELETE FROM monthly_archives WHERE id = :id"
    )
    suspend fun deleteArchive(id: Int)
}