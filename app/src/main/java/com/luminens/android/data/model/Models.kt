package com.luminens.android.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Photo(
    val id: String,
    @SerialName("user_id") val userId: String? = null,
    @SerialName("album_id") val albumId: String? = null,
    val url: String = "",
    @SerialName("storage_path") val storagePath: String? = null,
    @SerialName("is_generated") val isGenerated: Boolean = false,
    val liked: Boolean = false,
    @SerialName("is_public") val isPublic: Boolean = false,
    @SerialName("original_url") val originalUrl: String? = null,
    @SerialName("position_data") val positionData: PositionData? = null,
    @SerialName("created_at") val createdAt: String? = null,
)

@Serializable
data class PositionData(
    val x: Double? = null,
    val y: Double? = null,
    @SerialName("baselineSettings") val baselineSettings: Map<String, Double>? = null,
    val source: String? = null,
    @SerialName("hidden_from_gallery") val hiddenFromGallery: Boolean? = null,
)

@Serializable
data class Album(
    val id: String,
    @SerialName("user_id") val userId: String? = null,
    val name: String,
    @SerialName("cover_url") val coverUrl: String? = null,
    @SerialName("is_public") val isPublic: Boolean = false,
    @SerialName("created_at") val createdAt: String? = null,
    // Loaded separately
    val photos: List<Photo> = emptyList(),
    val photoCount: Int = 0,
)

@Serializable
data class Profile(
    val id: String,
    @SerialName("display_name") val displayName: String? = null,
    @SerialName("avatar_url") val avatarUrl: String? = null,
    val plan: String = "free",
    @SerialName("credits_remaining") val creditsRemaining: Int = 5,
    @SerialName("credits_monthly_limit") val creditsMonthlyLimit: Int = 10,
    @SerialName("plan_started_at") val planStartedAt: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
) {
    val isPremium get() = plan != "free"
    val planLabel get() = when (plan) {
        "starter" -> "Starter"
        "pro" -> "Pro"
        "editor" -> "Editor Pass"
        else -> "Free"
    }
    val maxShots get() = when (plan) {
        "free" -> 4
        "starter" -> 6
        "pro", "editor" -> 8
        else -> 4
    }
}

@Serializable
data class PrintOrder(
    val id: String? = null,
    @SerialName("user_id") val userId: String? = null,
    val status: String = "pending_payment",
    @SerialName("stripe_session_id") val stripeSessionId: String? = null,
    @SerialName("gelato_order_id") val gelatoOrderId: String? = null,
    @SerialName("total_amount_eur") val totalAmountEur: Double? = null,
    @SerialName("created_at") val createdAt: String? = null,
)

@Serializable
data class CartItem(
    val id: String,
    val type: String = "single", // "single" | "album"
    val photoUrl: String,
    val storagePath: String,
    val productId: String,
    val productName: String,
    val productUid: String,
    val quantity: Int = 1,
    val priceEur: Double = 0.0,
    val fitMode: String = "fill", // "fit" | "fill"
    val positionX: Float = 0.5f,
    val positionY: Float = 0.5f,
    val orientation: String = "vertical",
    val albumPhotos: List<String> = emptyList(),
    val albumName: String? = null,
)

@Serializable
data class ShippingAddress(
    val firstName: String = "",
    val lastName: String = "",
    val addressLine1: String = "",
    val city: String = "",
    val postCode: String = "",
    val country: String = "IT",
    val email: String = "",
)

@Serializable
data class ShipmentMethod(
    val uid: String,
    val name: String,
    val price: Double,
    val estimatedDeliveryDays: Int,
)
