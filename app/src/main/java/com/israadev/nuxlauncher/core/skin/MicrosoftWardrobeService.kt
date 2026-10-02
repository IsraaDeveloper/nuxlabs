package com.israadev.nuxlauncher.core.skin

import android.content.Context
import android.util.Base64
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.israadev.nuxlauncher.core.account.AccountManager
import com.israadev.nuxlauncher.core.account.microsoft.MicrosoftAuthService
import com.israadev.nuxlauncher.core.models.UserAccount
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
    var state: String = "INACTIVE", // "ACTIVE" or "INACTIVE"
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
    private const val MOJANG_SESSION_URL = "https://sessionserver.mojang.com"

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()

    /**
     * Nama tampilan ramah untuk tiap alias jubah resmi Minecraft (seperti pada ZalithLauncher)
     */
    fun getCapeDisplayName(alias: String?): String {
        return when (alias) {
            "Migrator" -> "Migrator Cape"
            "Vanilla" -> "Vanilla Cape"
            "Cherry Blossom" -> "Cherry Blossom Cape"
            "15th Anniversary" -> "15th Anniversary Cape"
            "Purple Heart" -> "Purple Heart Cape (Twitch)"
            "Follower's" -> "Follower's Cape (TikTok)"
            "MCC 15th Year" -> "MCC 15th Year Cape"
            "Minecon2011" -> "Minecon 2011 Cape"
            "Minecon2012" -> "Minecon 2012 Cape"
            "Minecon2013" -> "Minecon 2013 Cape"
            "Minecon2015" -> "Minecon 2015 Cape"
            "Minecon2016" -> "Minecon 2016 Cape"
            "Minecraft Experience" -> "Minecraft Experience Cape"
            "Mojang Office" -> "Mojang Office Cape"
            "Home" -> "Home Cape"
            "Menace" -> "Menace Cape"
            "Yearn" -> "Yearn Cape"
            "Common" -> "Common Cape"
            null, "" -> "Jubah Resmi Minecraft"
            else -> "$alias Cape"
        }
    }

    /**
     * Mengambil tekstur jubah aktif langsung dari Session Server resmi Mojang tanpa perlu token OAuth
     */
    suspend fun getSessionActiveCape(uuid: String): String? = withContext(Dispatchers.IO) {
        try {
            val cleanUuid = uuid.replace("-", "")
            val req = Request.Builder()
                .url("$MOJANG_SESSION_URL/session/minecraft/profile/$cleanUuid")
                .get()
                .build()

            httpClient.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return@withContext null
                val body = resp.body?.string() ?: return@withContext null
                val json = JsonParser.parseString(body).asJsonObject
                val props = json.getAsJsonArray("properties") ?: return@withContext null
                for (p in props) {
                    val pObj = p.asJsonObject
                    if (pObj.get("name")?.asString == "textures") {
                        val base64Val = pObj.get("value")?.asString ?: continue
                        val decodedBytes = Base64.decode(base64Val, Base64.DEFAULT)
                        val decodedJson = JsonParser.parseString(String(decodedBytes)).asJsonObject
                        val textures = decodedJson.getAsJsonObject("textures") ?: continue
                        val cape = textures.getAsJsonObject("CAPE")
                        val capeUrl = cape?.get("url")?.asString
                        if (!capeUrl.isNullOrBlank()) {
                            return@withContext if (capeUrl.startsWith("http://")) capeUrl.replace("http://", "https://") else capeUrl
                        }
                    }
                }
            }
        } catch (_: Exception) {}
        null
    }

    /**
     * Mengambil profil lengkap pemain dari api.minecraftservices.com.
     * Jika token kedaluwarsa (HTTP 401), secara otomatis memperbarui token via refresh_token,
     * menyimpan akun yang diperbarui ke AccountManager, lalu mencoba kembali.
     * Juga menggabungkan informasi dari Session Server jika diperlukan agar cape aktif selalu terdeteksi.
     */
    suspend fun getProfile(
        context: Context,
        account: UserAccount
    ): Result<Pair<UserAccount, MicrosoftProfileResponse>> = withContext(Dispatchers.IO) {
        var currentAcc = account

        // 1. Coba fetch dengan accessToken saat ini
        var firstResult = fetchProfileWithToken(currentAcc.safeAccessToken)

        // 2. Jika 401 Unauthorized (token kedaluwarsa) dan ada refresh_token, lakukan auto-refresh
        if (firstResult.isFailure && currentAcc.safeRefreshToken.isNotBlank() && currentAcc.safeRefreshToken != "0") {
            val refreshResult = MicrosoftAuthService.refreshMicrosoftAccount(currentAcc)
            if (refreshResult.isSuccess) {
                currentAcc = refreshResult.getOrThrow()
                AccountManager.updateAccount(context, currentAcc)
                firstResult = fetchProfileWithToken(currentAcc.safeAccessToken)
            }
        }

        // 3. Ambil juga tekstur jubah aktif dari Mojang Session Server sebagai jaminan/fallback
        val sessionCapeUrl = getSessionActiveCape(currentAcc.uuid)

        if (firstResult.isSuccess) {
            val baseProf = firstResult.getOrThrow()
            val capesList = baseProf.capes.toMutableList()

            // Sinkronkan status ACTIVE dengan session cape
            if (sessionCapeUrl != null) {
                var foundMatch = false
                for (cape in capesList) {
                    val safeCapeUrl = if (cape.url.startsWith("http://")) cape.url.replace("http://", "https://") else cape.url
                    if (safeCapeUrl == sessionCapeUrl || cape.url == sessionCapeUrl) {
                        cape.state = "ACTIVE"
                        foundMatch = true
                    }
                }
                // Jika user punya cape aktif di session tapi belum tercantum di daftar capes, tambahkan
                if (!foundMatch) {
                    capesList.add(
                        MicrosoftCape(
                            id = "mojang_active_cape",
                            state = "ACTIVE",
                            url = sessionCapeUrl,
                            alias = "Jubah Aktif Minecraft"
                        )
                    )
                }
            }

            val finalProf = baseProf.copy(capes = capesList)
            return@withContext Result.success(Pair(currentAcc, finalProf))
        }

        // 4. Jika api.minecraftservices.com gagal (misal koneksi / limit), namun session server punya cape aktif
        if (sessionCapeUrl != null) {
            val fallbackProf = MicrosoftProfileResponse(
                id = currentAcc.uuid,
                name = currentAcc.username,
                capes = listOf(
                    MicrosoftCape(
                        id = "mojang_active_cape",
                        state = "ACTIVE",
                        url = sessionCapeUrl,
                        alias = "Jubah Aktif Minecraft"
                    )
                )
            )
            return@withContext Result.success(Pair(currentAcc, fallbackProf))
        }

        // Jika semua gagal, teruskan exception
        Result.failure(firstResult.exceptionOrNull() ?: IOException("Gagal memuat jubah Minecraft."))
    }

    private fun fetchProfileWithToken(accessToken: String): Result<MicrosoftProfileResponse> {
        return try {
            val req = Request.Builder()
                .url("$MINECRAFT_SERVICES_URL/minecraft/profile")
                .header("Authorization", "Bearer $accessToken")
                .get()
                .build()

            httpClient.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) {
                    return Result.failure(IOException("HTTP ${resp.code}: ${resp.message}"))
                }
                val body = resp.body?.string() ?: return Result.failure(IOException("Respon profil kosong"))
                val profile = gson.fromJson(body, MicrosoftProfileResponse::class.java)
                Result.success(profile)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Mengubah jubah aktif di Minecraft.net dengan auto-refresh token jika kedaluwarsa
     */
    suspend fun changeCape(
        context: Context,
        account: UserAccount,
        capeId: String?
    ): Result<UserAccount> = withContext(Dispatchers.IO) {
        var currentAcc = account

        var res = doChangeCapeCall(currentAcc.safeAccessToken, capeId)
        if (res.isFailure && currentAcc.safeRefreshToken.isNotBlank() && currentAcc.safeRefreshToken != "0") {
            val refreshResult = MicrosoftAuthService.refreshMicrosoftAccount(currentAcc)
            if (refreshResult.isSuccess) {
                currentAcc = refreshResult.getOrThrow()
                AccountManager.updateAccount(context, currentAcc)
                res = doChangeCapeCall(currentAcc.safeAccessToken, capeId)
            }
        }

        if (res.isSuccess) {
            Result.success(currentAcc)
        } else {
            Result.failure(res.exceptionOrNull() ?: IOException("Gagal mengubah jubah."))
        }
    }

    private fun doChangeCapeCall(accessToken: String, capeId: String?): Result<Unit> {
        return try {
            val url = "$MINECRAFT_SERVICES_URL/minecraft/profile/capes/active"
            val req = if (capeId.isNullOrBlank() || capeId == "mojang_active_cape") {
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
                    return Result.failure(IOException("HTTP ${resp.code}: ${resp.message}"))
                }
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Mengunggah skin baru ke Minecraft.net dengan auto-refresh token jika kedaluwarsa
     */
    suspend fun uploadSkin(
        context: Context,
        account: UserAccount,
        file: File,
        isSlim: Boolean
    ): Result<UserAccount> = withContext(Dispatchers.IO) {
        var currentAcc = account

        var res = doUploadSkinCall(currentAcc.safeAccessToken, file, isSlim)
        if (res.isFailure && currentAcc.safeRefreshToken.isNotBlank() && currentAcc.safeRefreshToken != "0") {
            val refreshResult = MicrosoftAuthService.refreshMicrosoftAccount(currentAcc)
            if (refreshResult.isSuccess) {
                currentAcc = refreshResult.getOrThrow()
                AccountManager.updateAccount(context, currentAcc)
                res = doUploadSkinCall(currentAcc.safeAccessToken, file, isSlim)
            }
        }

        if (res.isSuccess) {
            Result.success(currentAcc)
        } else {
            Result.failure(res.exceptionOrNull() ?: IOException("Gagal mengunggah skin."))
        }
    }

    private fun doUploadSkinCall(accessToken: String, file: File, isSlim: Boolean): Result<Unit> {
        return try {
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
                    return Result.failure(IOException("HTTP ${resp.code}: ${resp.message}"))
                }
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
