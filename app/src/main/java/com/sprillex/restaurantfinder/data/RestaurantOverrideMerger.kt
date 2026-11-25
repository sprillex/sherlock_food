package com.sprillex.restaurantfinder.data

/**
 * Pure helper that overlays non-null [RestaurantOverride] fields onto a base [Restaurant]
 * without altering immutable identity or coordinate columns (`id`, `osm_type`, `osm_id`,
 * `latitude`, `longitude`).
 */
fun mergeRestaurantWithOverride(
    base: Restaurant,
    override: RestaurantOverride?
): Restaurant {
    if (override == null) return base
    return base.copy(
        name = override.name?.takeIf { it.isNotBlank() } ?: base.name,
        amenity = override.amenity?.takeIf { it.isNotBlank() } ?: base.amenity,
        cuisine = override.cuisine ?: base.cuisine,
        street = override.street ?: base.street,
        housenumber = override.housenumber ?: base.housenumber,
        postcode = override.postcode ?: base.postcode,
        city = override.city ?: base.city,
        phone = override.phone ?: base.phone,
        website = override.website ?: base.website,
        opening_hours = override.openingHours ?: base.opening_hours,
        takeaway = override.takeaway ?: base.takeaway,
        delivery = override.delivery ?: base.delivery,
        outdoor_seating = override.outdoorSeating ?: base.outdoor_seating,
        drive_through = override.driveThrough ?: base.drive_through,
        wheelchair = override.wheelchair ?: base.wheelchair,
        diet_vegetarian = override.dietVegetarian ?: base.diet_vegetarian,
        diet_vegan = override.dietVegan ?: base.diet_vegan,
        diet_gluten_free = override.dietGlutenFree ?: base.diet_gluten_free
    )
}

fun Restaurant.applyOverride(override: RestaurantOverride?): Restaurant {
    return mergeRestaurantWithOverride(this, override)
}
