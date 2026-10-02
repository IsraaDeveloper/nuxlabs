package com.israadev.nuxlauncher.core.skin

import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

data class MicrosoftCape(
    val id: String,
    val state: String = "INACTIVE", // "ACTIVE" or "INACTIVE"
    val url: String,
    val alias: String? = null
)

data class MicrosoftProfileResponse(
    val id: String,
    val name: String,
    val skins: List<MicrosoftSkinItem> = emptyList(),
    val capes: List<MicrosoftCape> = emptyList()
)

data class MicrosoftSkinItem(
    val id: String,
    val state: String = "INACTIVE",
    val url: String,
    val variant: String = "CLASSIC" // "CLASSIC" or "SLIM"
)

object MicrosoftWardrobeService {
    private const val MINECRAFT_SERVICES_URL = "https://api.minecraftservices.com"
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()
    private val gson = Gson()

    suspend fun getProfile(accessToken: String): Result<MicrosoftProfileResponse> = withContext(Dispatchers.IO) {
        try {
            val req = Request.Builder()
                .url("$MINECRAFT_SERVICES_URL/minecraft/profile")
                .header("Authorization", "Bearer $accessToken")
                .get()
                .build()
            httpClient.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) {
                    return@withContext Result.failure(IOException("Gagal memuat profil Minecraft.net (Code: ${resp.code})"))
                }
                val body = resp.body?.string() ?: return@withContext Result.failure(IOException("Respon profil kosong"))
                val profile = gson.fromJson(body, MicrosoftProfileResponse::class.java)
                Result.success(profile)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun changeCape(accessToken: String, capeId: String?): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val url = "$MINECRAFT_SERVICES_URL/minecraft/profile/capes/active"
            val req = if (capeId.isNullOrBlank()) {
                Request.Builder()
                    .url(url)
                    .header("Authorization", "Bearer $accessToken")
                    .delete()
                    .build()
            } else {
                val jsonBody = JsonObject().apply {
                    addProperty("capeId", capeId)
                }.toString().toRequestBody("application/json; charset=utf-8".toMediaType())

                Request.Builder()
                    .url(url)
                    .header("Authorization", "Bearer $accessToken")
                    .put(jsonBody)
                    .build()
            }

            httpClient.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful && resp.code != 200 && resp.code != 204) {
                    return@withContext Result.failure(IOException("Gagal mengubah jubah di Minecraft.net (Code: ${resp.code})"))
                }
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun uploadSkin(accessToken: String, file: File, isSlim: Boolean): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val variant = if (isSlim) "slim" else "classic"
            val fileBody = file.readBytes().toRequestBody("image/png".toMediaType())
            val multipartBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("variant", variant)
                .addFormDataPart("file", file.name, fileBody)
                .build()

            val req = Request.Builder()
                .url("$MINECRAFT_SERVICES_URL/minecraft/profile/skins")
                .header("Authorization", "Bearer $accessToken")
                .post(multipartBody)
                .build()

            httpClient.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful && resp.code != 200 && resp.code != 204) {
                    return@withContext Result.failure(IOException("Gagal mengunggah skin ke Minecraft.net (Code: ${resp.code})"))
                }
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
