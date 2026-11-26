package com.sprillex.restaurantfinder.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * User-created custom restaurant persisted in [UserDatabase] (`user_data.db`).
 * IDs are prefixed with `"custom/"` to guarantee zero collision with OSM `"node/"` / `"way/"` IDs.
 */
@Entity(tableName = "custom_restaurants")
data class CustomRestaurant(
    @PrimaryKey val id: String = generateCustomId(),
    val name: String,
    val amenity: String = "restaurant",
    val cuisine: String? = null,
    val street: String? = null,
    val housenumber: String? = null,
    val postcode: String? = null,
    val city: String? = null,
    val phone: String? = null,
    val website: String? = null,
    val openingHours: String? = null,
    val takeaway: String? = null,
    val delivery: String? = null,
    val outdoorSeating: String? = null,
    val driveThrough: String? = null,
    val wheelchair: String? = null,
    val dietVegetarian: String? = null,
    val dietVegan: String? = null,
    val dietGlutenFree: String? = null,
    val latitude: Double,
    val longitude: Double,
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toRestaurant(): Restaurant = Restaurant(
        id = id,
        osm_type = CUSTOM_OSM_TYPE,
        osm_id = 0L,
        name = name,
        amenity = amenity,
        cuisine = cuisine,
        street = street,
        housenumber = housenumber,
        postcode = postcode,
        city = city,
        phone = phone,
        website = website,
        opening_hours = openingHours,
        takeaway = takeaway,
        delivery = delivery,
        outdoor_seating = outdoorSeating,
        drive_through = driveThrough,
        wheelchair = wheelchair,
        diet_vegetarian = dietVegetarian,
        diet_vegan = dietVegan,
        diet_gluten_free = dietGlutenFree,
        latitude = latitude,
        longitude = longitude,
        last_updated = createdAt
    )

    companion object {
        const val CUSTOM_ID_PREFIX = "custom/"
        const val CUSTOM_OSM_TYPE = "custom"

        fun generateCustomId(): String = "$CUSTOM_ID_PREFIX${UUID.randomUUID()}"

        fun isCustomId(restaurantId: String): Boolean = restaurantId.startsWith(CUSTOM_ID_PREFIX)
    }
}

/**
 * Pure helper that combines base OSM [Restaurant]s with user-created [CustomRestaurant]s.
 * Custom venues take precedence if an ID ever collides.
 */
fun mergeVenueLists(
    osmRestaurants: List<Restaurant>,
    customRestaurants: List<CustomRestaurant>
): List<Restaurant> {
    if (customRestaurants.isEmpty()) return osmRestaurants
    val customAsRestaurants = customRestaurants.map { it.toRestaurant() }
    val customIds = customAsRestaurants.map { it.id }.toSet()
    return customAsRestaurants + osmRestaurants.filterNot { it.id in customIds }
}
