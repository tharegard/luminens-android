package com.luminens.android.data.repository

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.functions.functions
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.storage.storage
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import java.util.UUID
import javax.inject.Inject

class GenerationRepository @Inject constructor(
    private val client: SupabaseClient,
) {
    private val storage get() = client.storage
    private val functions get() = client.functions

    /**
     * Upload a photo file to Supabase Storage.
     * Returns the storage path: uploads/{userId}/source/{uuid}.jpg
     */
    suspend fun uploadReferencePhoto(
        bytes: ByteArray,
        extension: String = "jpg",
    ): String = withContext(Dispatchers.IO) {
        val userId = client.auth.currentUserOrNull()?.id ?: error("Not authenticated")
        val filename = "${UUID.randomUUID()}.$extension"
        val path = "uploads/$userId/source/$filename"
        storage["photos"].upload(path, bytes) { upsert = false }
        path
    }

    /**
     * Call the generate-photos edge function.
     * Returns list of generated photo URLs.
     */
    suspend fun generatePhotos(
        imagePaths: List<String>,
        style: String,
        setting: String,
        subSetting: String? = null,
        category: String = "adults",
        shotCount: Int = 1,
        aspectRatio: String = "1:1",
        resolution: String = "2K",
        customPrompt: String? = null,
    ): List<String> = withContext(Dispatchers.IO) {
        val body = buildJsonObject {
            putJsonArray("imagePaths") {
                imagePaths.forEach { add(kotlinx.serialization.json.JsonPrimitive(it)) }
            }
            put("style", style)
            put("setting", setting)
            subSetting?.let { put("subSetting", it) }
            put("category", category)
            put("shotCount", shotCount)
            put("aspectRatio", aspectRatio)
            put("resolution", resolution)
            customPrompt?.let { put("customPrompt", it) }
        }
        val response = functions.invoke("generate-photos", body = body)
        val json = Json.parseToJsonElement(response.bodyAsText())
        // Response: { "images": ["url1", "url2", ...] }
        json.jsonObject["images"]
            ?.jsonArray
            ?.map { it.jsonPrimitive.content }
            ?: emptyList()
    }

    /**
     * Call the enhance-prompt edge function.
     * Returns enhanced prompt string.
     */
    suspend fun enhancePrompt(
        style: String,
        setting: String,
        photoCount: Int,
        userPrompt: String?,
        category: String = "adults",
    ): String = withContext(Dispatchers.IO) {
        val body = buildJsonObject {
            put("style", style)
            put("setting", setting)
            put("photoCount", photoCount)
            userPrompt?.let { put("userPrompt", it) }
            put("category", category)
        }
        val response = functions.invoke("enhance-prompt", body = body)
        val json = Json.parseToJsonElement(response.bodyAsText())
        json.jsonObject["enhancedPrompt"]?.jsonPrimitive?.content ?: ""
    }

    /**
     * Save a generated photo URL to the database (photos table).
     */
    suspend fun saveGeneratedPhoto(url: String, storagePath: String? = null): String =
        withContext(Dispatchers.IO) {
            val userId = client.auth.currentUserOrNull()?.id ?: error("Not authenticated")
            val result = client.postgrest["photos"].insert(
                mapOf<String, Any?>(
                    "user_id" to userId,
                    "url" to url,
                    "is_generated" to true,
                    "storage_path" to storagePath,
                )
            ).decodeSingle<Map<String, JsonElement>>()
            result["id"]?.jsonPrimitive?.content ?: ""
        }

    /**
     * Call edit-with-ai edge function and return edited image URL.
     */
    suspend fun editWithAi(
        prompt: String,
        aspectRatio: String,
        imageDataUrl: String? = null,
        imageUrl: String? = null,
        functionName: String = "edit-with-ai",
    ): String = withContext(Dispatchers.IO) {
        val body = buildJsonObject {
            put("prompt", prompt)
            put("aspectRatio", aspectRatio)
            imageDataUrl?.let { put("imageDataUrl", it) }
            imageUrl?.let { put("imageUrl", it) }
        }
        val response = functions.invoke(functionName, body = body)
        val json = Json.parseToJsonElement(response.bodyAsText()).jsonObject
        val backendError = json["error"]?.jsonPrimitive?.contentOrNull
        val refusal = json["refusal"]?.jsonPrimitive?.booleanOrNull ?: false
        if (!backendError.isNullOrBlank()) {
            if (refusal) {
                throw IllegalStateException("REFUSAL: $backendError")
            }
            throw IllegalStateException(backendError)
        }
        json["editedImageUrl"]?.jsonPrimitive?.content
            ?: throw IllegalStateException("No edited image returned")
    }
}
