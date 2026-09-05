package com.sprillex.restaurantfinder.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "restaurant_details",
    foreignKeys = [
        ForeignKey(
            entity = Restaurant::class,
            parentColumns = ["id"],
            childColumns = ["restaurant_id"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class RestaurantDetailEntity(
    @PrimaryKey
    @ColumnInfo(name = "restaurant_id") val restaurantId: String,
    @ColumnInfo(name = "editorial_summary") val editorialSummary: String?,
    @ColumnInfo(name = "signature_items") val signatureItems: List<String>?,
    @ColumnInfo(name = "price_tier") val priceTier: String?,
    @ColumnInfo(name = "vibe_tags") val vibeTags: List<String>?,
    @ColumnInfo(name = "service_model") val serviceModel: String?,
    @ColumnInfo(name = "parking") val parking: String?,
    @ColumnInfo(name = "outdoor_patio") val outdoorPatio: String?,
    @ColumnInfo(name = "good_to_know") val goodToKnow: String?,
    @ColumnInfo(name = "fetched_at") val fetchedAt: Long = System.currentTimeMillis()
)
