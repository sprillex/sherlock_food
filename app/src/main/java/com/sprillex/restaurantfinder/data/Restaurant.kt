package com.sprillex.restaurantfinder.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "restaurants",
    indices = [
        Index(value = ["latitude", "longitude"], name = "idx_restaurants_lat_lon"),
        Index(value = ["amenity"], name = "idx_restaurants_amenity"),
        Index(value = ["cuisine"], name = "idx_restaurants_cuisine"),
        Index(value = ["city"], name = "idx_restaurants_city"),
        Index(value = ["brand"], name = "idx_restaurants_brand"),
        Index(value = ["delivery", "takeaway"], name = "idx_restaurants_delivery_takeaway")
    ]
)
data class Restaurant(
    @PrimaryKey
    @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "osm_type") val osm_type: String = "",
    @ColumnInfo(name = "osm_id") val osm_id: Long = 0,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "amenity") val amenity: String,
    @ColumnInfo(name = "cuisine") val cuisine: String? = null,
    @ColumnInfo(name = "street") val street: String? = null,
    @ColumnInfo(name = "housenumber") val housenumber: String? = null,
    @ColumnInfo(name = "postcode") val postcode: String? = null,
    @ColumnInfo(name = "city") val city: String? = null,
    @ColumnInfo(name = "phone") val phone: String? = null,
    @ColumnInfo(name = "website") val website: String? = null,
    @ColumnInfo(name = "opening_hours") val opening_hours: String? = null,
    @ColumnInfo(name = "takeaway") val takeaway: String? = null,
    @ColumnInfo(name = "delivery") val delivery: String? = null,
    @ColumnInfo(name = "outdoor_seating") val outdoor_seating: String? = null,
    @ColumnInfo(name = "drive_through") val drive_through: String? = null,
    @ColumnInfo(name = "wheelchair") val wheelchair: String? = null,
    @ColumnInfo(name = "brand") val brand: String? = null,
    @ColumnInfo(name = "brand_wikidata") val brand_wikidata: String? = null,
    @ColumnInfo(name = "wikidata") val wikidata: String? = null,
    @ColumnInfo(name = "diet_vegetarian") val diet_vegetarian: String? = null,
    @ColumnInfo(name = "diet_vegan") val diet_vegan: String? = null,
    @ColumnInfo(name = "diet_gluten_free") val diet_gluten_free: String? = null,
    @ColumnInfo(name = "latitude") val latitude: Double,
    @ColumnInfo(name = "longitude") val longitude: Double,
    @ColumnInfo(name = "last_updated") val last_updated: Long = 0
)
