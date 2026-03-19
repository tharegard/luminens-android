package com.luminens.android.data.remote

import com.luminens.android.data.model.Album
import com.luminens.android.data.model.CartItem
import com.luminens.android.data.model.Photo
import com.luminens.android.data.model.PrintOrder
import com.luminens.android.data.model.Profile
import com.luminens.android.data.model.ShippingAddress
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.functions.functions
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.storage.storage
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import javax.inject.Inject
import kotlin.time.Duration.Companion.hours

@Serializable
private data class AlbumPhotoJoin(
    @SerialName("album_id") val albumId: String? = null,
    @SerialName("photo_id") val photoId: String,
    @SerialName("sort_order") val sortOrder: Int? = null,
)

class SupabaseDataSource @Inject constructor(
    private val client: SupabaseClient,
) {
    private val db get() = client.postgrest
    private val storage get() = client.storage
    private val userId get() = client.auth.currentSessionOrNull()?.user?.id

    // ── Profile ─────────────────────────────────────────────────────────────

    suspend fun getProfile(): Profile? = withContext(Dispatchers.IO) {
        val uid = userId ?: return@withContext null
        db["profiles"]
            .select { filter { eq("id", uid) } }
            .decodeSingleOrNull<Profile>()
    }

    suspend fun updateProfile(updates: Map<String, Any>) = withContext(Dispatchers.IO) {
        val uid = userId ?: return@withContext
        db["profiles"].update(updates) { filter { eq("id", uid) } }
    }

    // ── Photos ───────────────────────────────────────────────────────────────

    suspend fun getPhotos(): List<Photo> = withContext(Dispatchers.IO) {
        val uid = userId ?: return@withContext emptyList()
        val photos = db["photos"]
            .select {
                filter { eq("user_id", uid) }
                order("created_at", Order.DESCENDING)
            }
            .decodeList<Photo>()
        val visiblePhotos = photos.filter { photo ->
            val source = photo.positionData?.source
            val hiddenFromGallery = photo.positionData?.hiddenFromGallery == true
            !hiddenFromGallery && (photo.isGenerated || source == "gallery_upload")
        }
        resolveSignedUrls(visiblePhotos)
    }

    suspend fun deletePhoto(id: String, storagePath: String?) = withContext(Dispatchers.IO) {
        // Remove from junction table first (in case there are rows without CASCADE)
        runCatching { db["album_photos"].delete { filter { eq("photo_id", id) } } }
        if (storagePath != null) {
            runCatching { storage["photos"].delete(storagePath) }
        }
        db["photos"].delete { filter { eq("id", id) } }
    }

    suspend fun updatePhotoPublic(id: String, isPublic: Boolean) = withContext(Dispatchers.IO) {
        db["photos"].update(mapOf("is_public" to isPublic)) {
            filter { eq("id", id) }
        }
    }

    /** Insert a new photo record after upload. Returns the inserted photo. */
    suspend fun insertPhoto(photo: Map<String, Any>): Photo = withContext(Dispatchers.IO) {
        db["photos"].insert(photo).decodeSingle<Photo>()
    }

    // ── Albums ────────────────────────────────────────────────────────────────

    suspend fun getAlbums(): List<Album> = withContext(Dispatchers.IO) {
        val uid = userId ?: return@withContext emptyList()
        val albums = db["albums"]
            .select {
                filter { eq("user_id", uid) }
                order("created_at", Order.DESCENDING)
            }
            .decodeList<Album>()
        resolveAlbumCoverUrls(albums)
    }

    suspend fun getAlbumPhotos(albumId: String): List<Photo> = withContext(Dispatchers.IO) {
        // Schema was migrated to many-to-many: album_photos junction table
        val joins = db["album_photos"]
            .select {
                filter { eq("album_id", albumId) }
                order("sort_order", Order.ASCENDING)
            }
            .decodeList<AlbumPhotoJoin>()
        val photoIds = joins.map { it.photoId }
        if (photoIds.isEmpty()) return@withContext emptyList()
        val photos = db["photos"]
            .select { filter { isIn("id", photoIds) } }
            .decodeList<Photo>()
        val resolved = resolveSignedUrls(photos)
        val byId = resolved.associateBy { it.id }
        joins.mapNotNull { join -> byId[join.photoId] }
    }

    suspend fun createAlbum(name: String): Album = withContext(Dispatchers.IO) {
        val uid = userId ?: error("Not authenticated")
        db["albums"]
            .insert(mapOf("name" to name, "user_id" to uid))
            .decodeSingle<Album>()
    }

    suspend fun getAvailablePhotosForAlbum(albumId: String): List<Photo> = withContext(Dispatchers.IO) {
        val uid = userId ?: return@withContext emptyList()
        val allUserPhotos = db["photos"]
            .select {
                filter { eq("user_id", uid) }
                order("created_at", Order.DESCENDING)
            }
            .decodeList<Photo>()

        val albumPhotoIds = db["album_photos"]
            .select { filter { eq("album_id", albumId) } }
            .decodeList<AlbumPhotoJoin>()
            .map { it.photoId }
            .toSet()

        resolveSignedUrls(allUserPhotos.filterNot { it.id in albumPhotoIds })
    }

    suspend fun addPhotosToAlbum(albumId: String, photoIds: List<String>) = withContext(Dispatchers.IO) {
        if (photoIds.isEmpty()) return@withContext

        val currentCount = db["album_photos"]
            .select { filter { eq("album_id", albumId) } }
            .decodeList<AlbumPhotoJoin>()
            .size

        val rows = photoIds.mapIndexed { index, photoId ->
            mapOf(
                "album_id" to albumId,
                "photo_id" to photoId,
                "sort_order" to (currentCount + index),
            )
        }
        db["album_photos"].insert(rows)
    }

    suspend fun removePhotoFromAlbum(albumId: String, photoId: String) = withContext(Dispatchers.IO) {
        db["album_photos"].delete {
            filter {
                eq("album_id", albumId)
                eq("photo_id", photoId)
            }
        }
    }

    suspend fun reorderAlbumPhotos(albumId: String, orderedPhotoIds: List<String>) = withContext(Dispatchers.IO) {
        if (orderedPhotoIds.isEmpty()) return@withContext
        orderedPhotoIds.forEachIndexed { index, photoId ->
            db["album_photos"].update(mapOf("sort_order" to index)) {
                filter {
                    eq("album_id", albumId)
                    eq("photo_id", photoId)
                }
            }
        }
    }

    suspend fun deleteAlbum(id: String) = withContext(Dispatchers.IO) {
        db["albums"].delete { filter { eq("id", id) } }
    }

    suspend fun updateAlbumPublic(id: String, isPublic: Boolean) = withContext(Dispatchers.IO) {
        db["albums"].update(mapOf("is_public" to isPublic)) { filter { eq("id", id) } }
    }

    suspend fun updateAlbumName(id: String, name: String) = withContext(Dispatchers.IO) {
        db["albums"].update(mapOf("name" to name)) { filter { eq("id", id) } }
    }

    // ── Print orders ───────────────────────────────────────────────────────────

    suspend fun getPrintOrders(): List<PrintOrder> = withContext(Dispatchers.IO) {
        val uid = userId ?: return@withContext emptyList()
        db["print_orders"]
            .select {
                filter { eq("user_id", uid) }
                order("created_at", Order.DESCENDING)
            }
            .decodeList<PrintOrder>()
    }

    suspend fun getPrintOrderByStripeSessionId(sessionId: String): PrintOrder? = withContext(Dispatchers.IO) {
        val uid = userId ?: return@withContext null
        db["print_orders"]
            .select {
                filter {
                    eq("user_id", uid)
                    eq("stripe_session_id", sessionId)
                }
                order("created_at", Order.DESCENDING)
            }
            .decodeList<PrintOrder>()
            .firstOrNull()
    }

    suspend fun createCheckoutSession(
        items: List<CartItem>,
        shippingAddress: ShippingAddress,
        shipmentMethodUid: String?,
        totalAmountEur: Double,
    ): String = withContext(Dispatchers.IO) {
        val body = buildJsonObject {
            put("items", buildJsonArray {
                items.forEach { item ->
                    add(buildJsonObject {
                        put("productUid", item.productUid)
                        put("quantity", item.quantity)
                        put("storagePath", item.storagePath)
                        put("photoUrl", item.photoUrl)
                    })
                }
            })
            putJsonObject("shippingAddress") {
                put("firstName", shippingAddress.firstName)
                put("lastName", shippingAddress.lastName)
                put("addressLine1", shippingAddress.addressLine1)
                put("city", shippingAddress.city)
                put("postCode", shippingAddress.postCode)
                put("country", shippingAddress.country)
                put("email", shippingAddress.email)
            }
            shipmentMethodUid?.takeIf { it.isNotBlank() }?.let { put("shipmentMethodUid", it) }
            put("totalAmountEur", totalAmountEur)
        }

        val response = client.functions.invoke("create-checkout-session", body = body)
        val payload = Json.parseToJsonElement(response.bodyAsText()).jsonObject
        payload["url"]?.jsonPrimitive?.content
            ?: throw IllegalStateException(payload["error"]?.jsonPrimitive?.content ?: "Checkout non disponibile")
    }

    // ── Storage: Signed URL ──────────────────────────────────────────────────

    suspend fun getSignedUrl(storagePath: String): String? = withContext(Dispatchers.IO) {
        runCatching {
            storage["photos"].createSignedUrl(storagePath, expiresIn = 1.hours)
        }.getOrNull()
    }

    /** Upload file bytes to Storage, returns the storage path. */
    suspend fun uploadPhoto(
        bytes: ByteArray,
        storagePath: String,
        contentType: String = "image/jpeg",
    ): String = withContext(Dispatchers.IO) {
        storage["photos"].upload(storagePath, bytes) {
            upsert = true
        }
        storagePath
    }

    /** Replace an existing photo file in Storage and update its storage_path, then verify persisted row. */
    suspend fun updatePhotoFile(photoId: String, file: java.io.File): Photo = withContext(Dispatchers.IO) {
        val bytes = file.readBytes()
        val currentPath = db["photos"].select { filter { eq("id", photoId) } }
            .decodeSingleOrNull<Photo>()?.storagePath
        val path = currentPath ?: "edited/${photoId}_${System.currentTimeMillis()}.jpg"
        storage["photos"].upload(path, bytes) { upsert = true }
        db["photos"].update(mapOf("storage_path" to path, "url" to storage["photos"].publicUrl(path))) {
            filter { eq("id", photoId) }
        }

        val updated = db["photos"].select { filter { eq("id", photoId) } }
            .decodeSingleOrNull<Photo>() ?: error("Impossibile verificare la foto salvata")

        val hasPath = !updated.storagePath.isNullOrBlank()
        val hasUrl = updated.url.isNotBlank()
        if (!hasPath || !hasUrl) {
            error("Salvataggio incompleto: metadati foto non aggiornati")
        }
        updated
    }

    // ── Public access (no auth required) ────────────────────────────────────

    suspend fun getPublicPhoto(photoId: String): Photo? = withContext(Dispatchers.IO) {
        db["photos"]
            .select { filter { and { eq("id", photoId); eq("is_public", true) } } }
            .decodeSingleOrNull<Photo>()
    }

    suspend fun getPublicAlbum(albumId: String): Album? = withContext(Dispatchers.IO) {
        db["albums"]
            .select { filter { and { eq("id", albumId); eq("is_public", true) } } }
            .decodeSingleOrNull<Album>()
    }

    suspend fun getPublicAlbumPhotos(albumId: String): List<Photo> = withContext(Dispatchers.IO) {
        // Also uses junction table for public albums
        val joins = db["album_photos"]
            .select { filter { eq("album_id", albumId) } }
            .decodeList<AlbumPhotoJoin>()
        val photoIds = joins.map { it.photoId }
        if (photoIds.isEmpty()) return@withContext emptyList()
        val photos = db["photos"]
            .select { filter { isIn("id", photoIds) } }
            .decodeList<Photo>()
        resolveSignedUrls(photos)
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    /**
     * For each photo that has a storagePath, replace the stored URL with a
     * fresh 1-hour signed URL so private-bucket photos load in Coil.
     * Requests are dispatched in parallel to minimise latency.
     */
    private suspend fun resolveSignedUrls(photos: List<Photo>): List<Photo> =
        coroutineScope {
            photos.map { photo ->
                async {
                    val signedUrl = photo.storagePath?.let { path ->
                        runCatching {
                            storage["photos"].createSignedUrl(path, expiresIn = 1.hours)
                        }.getOrNull()
                    }
                    if (signedUrl != null) photo.copy(url = signedUrl) else photo
                }
            }.awaitAll()
        }

    /**
     * Some albums have no explicit cover_url. In that case, use the first
     * photo in album_photos (ordered by sort_order) as a visual fallback.
     */
    private suspend fun resolveAlbumCoverUrls(albums: List<Album>): List<Album> =
        coroutineScope {
            albums.map { album ->
                async {
                    val explicitCover = album.coverUrl?.takeIf { it.isNotBlank() && it.startsWith("http") }
                    if (explicitCover != null) return@async album

                    val firstPhotoId = runCatching {
                        db["album_photos"]
                            .select {
                                filter { eq("album_id", album.id) }
                                order("sort_order", Order.ASCENDING)
                            }
                            .decodeList<AlbumPhotoJoin>()
                            .firstOrNull()
                            ?.photoId
                    }.getOrNull() ?: return@async album

                    val firstPhoto = runCatching {
                        db["photos"]
                            .select { filter { eq("id", firstPhotoId) } }
                            .decodeList<Photo>()
                            .firstOrNull()
                    }.getOrNull() ?: return@async album

                    val resolved = resolveSignedUrls(listOf(firstPhoto)).firstOrNull()
                    val coverUrl = resolved?.url ?: firstPhoto.url
                    if (coverUrl.isBlank()) album else album.copy(coverUrl = coverUrl)
                }
            }.awaitAll()
        }
}
