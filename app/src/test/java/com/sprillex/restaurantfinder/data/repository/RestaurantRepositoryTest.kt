package com.sprillex.restaurantfinder.data.repository

import com.sprillex.restaurantfinder.data.Restaurant
import com.sprillex.restaurantfinder.data.RestaurantDetailDao
import com.sprillex.restaurantfinder.data.RestaurantDetailEntity
import com.sprillex.restaurantfinder.data.RestaurantWithDetails
import com.sprillex.restaurantfinder.data.remote.AiDiningClient
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RestaurantRepositoryTest {

    private class FakeDetailDao : RestaurantDetailDao {
        val map = mutableMapOf<String, RestaurantDetailEntity>()

        override fun getRestaurantWithDetailsFlow(restaurantId: String): Flow<RestaurantWithDetails?> {
            val restaurant = Restaurant(id = restaurantId, name = "Test", amenity = "restaurant", latitude = 0.0, longitude = 0.0)
            return flowOf(RestaurantWithDetails(restaurant, map[restaurantId]))
        }

        override suspend fun getCachedDetails(restaurantId: String): RestaurantDetailEntity? {
            return map[restaurantId]
        }

        override suspend fun upsertDetails(details: RestaurantDetailEntity) {
            map[details.restaurantId] = details
        }

        override suspend fun deleteDetails(restaurantId: String) {
            map.remove(restaurantId)
        }
    }

    @Test
    fun testEnsureDetailsEnriched_whenCachedDoesNotReQuery() = runBlocking {
        val fakeDao = FakeDetailDao()
        fakeDao.map["node/1001"] = RestaurantDetailEntity(
            restaurantId = "node/1001",
            editorialSummary = "Pre-cached summary",
            signatureItems = listOf("Burger"),
            priceTier = "$$",
            vibeTags = listOf("cozy"),
            serviceModel = "full_table_service",
            parking = "lot",
            outdoorPatio = "yes",
            goodToKnow = "Tip"
        )

        val client = AiDiningClient(apiKey = "")
        val repository = RestaurantRepository(fakeDao, client)

        val restaurant = Restaurant(
            id = "node/1001",
            name = "Bistro 100",
            amenity = "restaurant",
            latitude = 41.5,
            longitude = -83.5
        )

        repository.ensureDetailsEnriched(restaurant)
        assertEquals("Pre-cached summary", fakeDao.getCachedDetails("node/1001")?.editorialSummary)
    }
}
