package com.sprillex.restaurantfinder.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface VisitLogDao {
    @Query("SELECT * FROM visit_logs ORDER BY visitedAt DESC")
    fun getAllVisitLogs(): Flow<List<VisitLog>>

    @Query("SELECT * FROM visit_logs WHERE restaurantId = :restaurantId ORDER BY visitedAt DESC")
    fun getVisitLogsForRestaurant(restaurantId: String): Flow<List<VisitLog>>

    @Query("SELECT * FROM visit_logs WHERE restaurantId = :restaurantId ORDER BY visitedAt DESC")
    suspend fun getVisitLogsListForRestaurant(restaurantId: String): List<VisitLog>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertVisitLog(visitLog: VisitLog): Long

    @Delete
    suspend fun deleteVisitLog(visitLog: VisitLog)

    @Query("DELETE FROM visit_logs WHERE id = :visitId")
    suspend fun deleteVisitLogById(visitId: Long)
}
