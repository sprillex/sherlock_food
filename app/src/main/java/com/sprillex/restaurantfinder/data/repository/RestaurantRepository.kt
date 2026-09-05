package com.sprillex.restaurantfinder.data.repository

import com.sprillex.restaurantfinder.data.Restaurant
import com.sprillex.restaurantfinder.data.RestaurantDetailDao
import com.sprillex.restaurantfinder.data.RestaurantDetailEntity
import com.sprillex.restaurantfinder.data.RestaurantWithDetails
import com.sprillex.restaurantfinder.data.remote.AiDiningClient
import kotlinx.coroutines.flow.Flow

class RestaurantRepository(
    private val detailDao: RestaurantDetailDao,
    private val aiClient: AiDiningClient
) {
    /**
     * UI listens to this Flow. Emits initial cached state immediately,
     * and re-emits automatically once AI data is fetched and stored.
     */
    fun observeRestaurant(restaurantId: String): Flow<RestaurantWithDetails?> {
        return detailDao.getRestaurantWithDetailsFlow(restaurantId)
    }

    /**
     * Call this when opening the restaurant detail screen.
     * Executes network fetch only if cache is absent.
     */
    suspend fun ensureDetailsEnriched(restaurant: Restaurant, apiKeyOverride: String? = null) {
        val cached = detailDao.getCachedDetails(restaurant.id)
        if (cached != null) return

        val aiResult = aiClient.queryDiningProfile(restaurant, overrideApiKey = apiKeyOverride)
        if (aiResult != null) {
            val summary = if (aiResult.found) {
                aiResult.editorialSummary
            } else {
                "No additional web dining notes found for this venue."
            }
            val entity = RestaurantDetailEntity(
                restaurantId = restaurant.id,
                editorialSummary = summary,
                signatureItems = aiResult.signatureItems,
                priceTier = aiResult.priceTier,
                vibeTags = aiResult.vibeTags,
                serviceModel = aiResult.serviceModel,
                parking = aiResult.parking,
                outdoorPatio = aiResult.outdoorPatio,
                goodToKnow = aiResult.goodToKnow,
                fetchedAt = System.currentTimeMillis()
            )
            detailDao.upsertDetails(entity)
        }
    }
}
