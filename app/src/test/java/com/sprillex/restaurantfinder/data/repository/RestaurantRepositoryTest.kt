package com.sprillex.restaurantfinder.data.repository

import com.sprillex.restaurantfinder.data.Restaurant
import com.sprillex.restaurantfinder.data.RestaurantDetailDao
import com.sprillex.restaurantfinder.data.RestaurantDetailEntity
import com.sprillex.restaurantfinder.data.RestaurantWithDetails
import com.sprillex.restaurantfinder.data.remote.AiDiningClient
import com.sprillex.restaurantfinder.data.remote.AiDiningResponse
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
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

    private class StubAiDiningClient(
        private val responseToReturn: AiDiningResponse?
    ) : AiDiningClient(apiKey = "test-key") {
        var callCount = 0

        override suspend fun queryDiningProfile(
            restaurant: Restaurant,
            overrideApiKey: String?
        ): AiDiningResponse? {
            callCount++
            return responseToReturn
        }
    }

    private fun sampleRestaurant() = Restaurant(
        id = "node/1001",
        name = "Bistro 100",
        amenity = "restaurant",
        latitude = 41.5,
        longitude = -83.5
    )

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

        val client = StubAiDiningClient(
            AiDiningResponse(found = true, editorialSummary = "Fresh AI summary")
        )
        val repository = RestaurantRepository(fakeDao, client)

        repository.ensureDetailsEnriched(sampleRestaurant())
        assertEquals(0, client.callCount)
        assertEquals("Pre-cached summary", fakeDao.getCachedDetails("node/1001")?.editorialSummary)
    }

    @Test
    fun testEnsureDetailsEnriched_forceRefreshBypassesFoundFalsePlaceholder() = runBlocking {
        val fakeDao = FakeDetailDao()
        fakeDao.map["node/1001"] = RestaurantDetailEntity(
            restaurantId = "node/1001",
            editorialSummary = "No additional web dining notes found for this venue.",
            signatureItems = null,
            priceTier = null,
            vibeTags = null,
            serviceModel = null,
            parking = null,
            outdoorPatio = null,
            goodToKnow = null
        )

        val client = StubAiDiningClient(
            AiDiningResponse(
                found = true,
                editorialSummary = "Updated neighborhood bistro summary",
                signatureItems = listOf("Perch Tacos"),
                priceTier = "$$",
                vibeTags = listOf("casual"),
                serviceModel = "full_table_service",
                parking = "dedicated_lot",
                outdoorPatio = "yes",
                goodToKnow = "Patio open in summer"
            )
        )
        val repository = RestaurantRepository(fakeDao, client)

        repository.ensureDetailsEnriched(sampleRestaurant(), forceRefresh = true)

        assertEquals(1, client.callCount)
        val updated = fakeDao.getCachedDetails("node/1001")
        assertEquals("Updated neighborhood bistro summary", updated?.editorialSummary)
        assertEquals(listOf("Perch Tacos"), updated?.signatureItems)
        assertEquals("$$", updated?.priceTier)
    }

    @Test
    fun testEnsureDetailsEnriched_forceRefreshKeepsExistingCacheWhenClientReturnsNull() = runBlocking {
        val fakeDao = FakeDetailDao()
        fakeDao.map["node/1001"] = RestaurantDetailEntity(
            restaurantId = "node/1001",
            editorialSummary = "Existing cached note",
            signatureItems = null,
            priceTier = null,
            vibeTags = null,
            serviceModel = null,
            parking = null,
            outdoorPatio = null,
            goodToKnow = null
        )

        val client = StubAiDiningClient(null)
        val repository = RestaurantRepository(fakeDao, client)

        repository.ensureDetailsEnriched(sampleRestaurant(), forceRefresh = true)

        assertEquals(1, client.callCount)
        assertEquals("Existing cached note", fakeDao.getCachedDetails("node/1001")?.editorialSummary)
    }
}
