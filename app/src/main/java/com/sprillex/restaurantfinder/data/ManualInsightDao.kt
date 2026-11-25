package com.sprillex.restaurantfinder.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ManualInsightDao {
    @Query("SELECT * FROM manual_insights")
    fun getAllManualInsights(): Flow<List<ManualInsight>>

    @Query("SELECT * FROM manual_insights WHERE restaurantId = :restaurantId")
    fun getManualInsightForRestaurant(restaurantId: String): Flow<ManualInsight?>

    @Query("SELECT * FROM manual_insights WHERE restaurantId = :restaurantId")
    suspend fun getManualInsight(restaurantId: String): ManualInsight?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertManualInsight(insight: ManualInsight)

    @Query("DELETE FROM manual_insights WHERE restaurantId = :restaurantId")
    suspend fun deleteManualInsight(restaurantId: String)

    @Delete
    suspend fun removeManualInsight(insight: ManualInsight)
}
