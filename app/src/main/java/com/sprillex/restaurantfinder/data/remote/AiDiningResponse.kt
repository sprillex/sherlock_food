package com.sprillex.restaurantfinder.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AiDiningResponse(
    @SerialName("found") val found: Boolean,
    @SerialName("editorial_summary") val editorialSummary: String? = null,
    @SerialName("signature_items") val signatureItems: List<String>? = null,
    @SerialName("price_tier") val priceTier: String? = null,
    @SerialName("vibe_tags") val vibeTags: List<String>? = null,
    @SerialName("service_model") val serviceModel: String? = null,
    @SerialName("parking") val parking: String? = null,
    @SerialName("outdoor_patio") val outdoorPatio: String? = null,
    @SerialName("good_to_know") val goodToKnow: String? = null
)
