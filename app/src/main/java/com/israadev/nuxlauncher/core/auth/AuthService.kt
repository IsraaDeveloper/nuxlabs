package com.israadev.nuxlauncher.core.auth

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit
import com.israadev.nuxlauncher.core.account.AccountManager
import com.israadev.nuxlauncher.core.network.NuxConfig

data class AuthUser(
    val uid: String = "",
    val email: String = "",
    val username: String = "",
    val photoURL: String = "",
    val platform: String = "android",
    val isActivated: Boolean = false,
    val tier: String = "unactivated",
    val activatedAt: Long? = null,
    val expiresAt: String? = null,
    val redeemedKey: String? = null,
    val idToken: String = "",
    val refreshToken: String = ""
)

data class LoginResult(
    val success: Boolean,
    val isActivated: Boolean,
    val user: AuthUser,
    val message: String? = null
)

data class RegisterResult(
    val success: Boolean,
    val isActivated: Boolean,
    val user: AuthUser,
    val message: String? = null
)

data class ActivateResult(
    val success: Boolean,
    val isActivated: Boolean,
    val tier: String = "lifetime",
    val expiresAt: String = "lifetime",
    val message: String = ""
)

data class SubscriptionInfo(
    val isActivated: Boolean = false,
    val tier: String = "unactivated",
    val activatedAt: Long? = null,
    val expiresAt: String? = null,
    val redeemedKey: String? = null
)

object AuthService {
    private val gson = Gson()
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .writeTimeout(25, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    // Endpoint server Keystore NUX Launcher (diinjeksi dari local.properties via NuxConfig)
    private val CANDIDATE_BASES: List<String>
        get() = NuxConfig.CANDIDATE_BASES

    suspend fun postApi(endpoint: String, jsonBody: String, bearerToken: String? = null): Result<String> = withContext(Dispatchers.IO) {
        if (CANDIDATE_BASES.isEmpty()) {
            return@withContext Result.failure(Exception("Server autentikasi belum dikonfigurasi. Pastikan 'nux.server.url' diisi di local.properties."))
        }
        var lastError: Exception? = null

        for (base in CANDIDATE_BASES) {
            try {
                val url = "$base$endpoint"
                val body = jsonBody.toRequestBody(jsonMediaType)
                val reqBuilder = Request.Builder()
                    .url(url)
                    .post(body)
                if (!bearerToken.isNullOrBlank()) {
                    reqBuilder.addHeader("Authorization", "Bearer $bearerToken")
                }
                val request = reqBuilder.build()

                val response = client.newCall(request).execute()
                val responseBody = response.body.string()

                // Cek pesan error dari backend JSON terlebih dahulu (baik HTTP 200 maupun status kode lainnya)
                try {
                    val errorJson = gson.fromJson(responseBody, JsonObject::class.java)
                    if (errorJson != null) {
                        if (errorJson.has("remainingSeconds")) {
                            val remSec = errorJson.get("remainingSeconds").asInt
                            if (remSec > 0) {
                                OtpCooldownManager.markOtpSent(null, remSec)
                            }
                        }
                        if (errorJson.has("error")) {
                            val msg = errorJson.get("error").asString
                            return@withContext Result.failure(Exception(msg))
                        }
                        if (errorJson.has("success") && !errorJson.get("success").asBoolean) {
                            val msg = if (errorJson.has("message")) errorJson.get("message").asString else "Permintaan ditolak oleh server."
                            return@withContext Result.failure(Exception(msg))
                        }
                    }
                } catch (_: Exception) {}

                if (response.isSuccessful) {
                    return@withContext Result.success(responseBody)
                }

                // Tangani kode error gateway Cloudflare (502, 503, 504, 520, 521, 522, 524, 530)
                if (response.code in listOf(502, 503, 504, 520, 521, 522, 524, 530)) {
                    return@withContext Result.failure(
                        Exception("Server sedang sibuk atau mengalami kendala gateway (kode: ${response.code}). Harap coba beberapa saat lagi.")
                    )
                }

                lastError = Exception("Server merespon kode: ${response.code}")
            } catch (e: Exception) {
                lastError = Exception(
                    when (e) {
                        is java.net.SocketTimeoutException -> "Koneksi ke server timeout. Periksa koneksi internet kamu dan coba lagi."
                        is java.net.UnknownHostException -> "Tidak dapat menemukan server autentikasi. Pastikan HP terhubung ke internet."
                        else -> e.message ?: "Gagal terhubung ke server autentikasi"
                    }
                )
            }
        }

        Result.failure(lastError ?: Exception("Gagal menghubungi server autentikasi"))
    }

    /**
     * 1. Kirim Kode OTP Verifikasi (Tipe: "register" atau "reset_password")
     */
    suspend fun sendOtp(email: String, username: String = "", type: String = "register"): Result<String> {
        val payload = JsonObject().apply {
            addProperty("email", email.trim().lowercase())
            addProperty("username", username.trim())
            addProperty("type", type)
        }
        val result = postApi("/api/auth/send-otp", payload.toString())
        return result.mapCatching { json ->
            val obj = gson.fromJson(json, JsonObject::class.java)
            obj.get("message")?.asString ?: "Kode verifikasi 6-digit telah dikirim ke email kamu."
        }
    }

    /**
     * 2. Registrasi Akun Android Baru (Langsung tanpa OTP)
     */
    suspend fun register(
        email: String,
        username: String,
        password: String
    ): Result<RegisterResult> {
        val payload = JsonObject().apply {
            addProperty("email", email.trim().lowercase())
            addProperty("username", username.trim())
            addProperty("password", password)
        }

        val result = postApi("/api/auth/android/register", payload.toString())
        return result.mapCatching { json ->
            val obj = gson.fromJson(json, JsonObject::class.java)
            val userObj = obj.getAsJsonObject("user")
            val user = AuthUser(
                uid = userObj?.get("uid")?.asString ?: "",
                email = userObj?.get("email")?.asString ?: email,
                username = userObj?.get("username")?.asString ?: username,
                photoURL = userObj?.get("photoURL")?.asString ?: "",
                platform = "android",
                isActivated = obj.get("isActivated")?.asBoolean ?: false
            )
            RegisterResult(
                success = obj.get("success")?.asBoolean ?: true,
                isActivated = user.isActivated,
                user = user,
                message = obj.get("message")?.asString
            )
        }
    }

    /**
     * 3. Login Akun Android (Mengecek platform & status aktivasi)
     */
    suspend fun login(email: String, password: String): Result<LoginResult> {
        val payload = JsonObject().apply {
            addProperty("email", email.trim().lowercase())
            addProperty("password", password)
        }

        val result = postApi("/api/auth/android/login", payload.toString())
        return result.mapCatching { json ->
            val obj = gson.fromJson(json, JsonObject::class.java)
            val isActivated = obj.get("isActivated")?.asBoolean ?: false
            val userObj = obj.getAsJsonObject("user")
            val idToken = obj.get("idToken")?.asString ?: ""
            val refreshToken = obj.get("refreshToken")?.asString ?: ""
            val user = AuthUser(
                uid = userObj?.get("uid")?.asString ?: "",
                email = userObj?.get("email")?.asString ?: email,
                username = userObj?.get("username")?.asString ?: email.substringBefore("@"),
                photoURL = userObj?.get("photoURL")?.asString ?: "",
                platform = "android",
                isActivated = isActivated,
                tier = userObj?.get("tier")?.asString ?: "unactivated",
                activatedAt = userObj?.get("activatedAt")?.asLong,
                expiresAt = userObj?.get("expiresAt")?.asString,
                redeemedKey = userObj?.get("redeemedKey")?.asString,
                idToken = idToken,
                refreshToken = refreshToken
            )
            LoginResult(
                success = obj.get("success")?.asBoolean ?: true,
                isActivated = isActivated,
                user = user,
                message = obj.get("message")?.asString
            )
        }
    }

    /**
     * Refresh Firebase ID Token menggunakan Refresh Token
     */
    suspend fun refreshIdToken(refreshToken: String): String? = withContext(Dispatchers.IO) {
        if (refreshToken.isBlank()) return@withContext null
        try {
            val apiKey = "AIzaSyDIqvaTHjuwNboBXcVDoY1jJYntS0ECsJ0"
            val refreshUrl = "https://securetoken.googleapis.com/v1/token?key=$apiKey"
            val formBody = okhttp3.FormBody.Builder()
                .add("grant_type", "refresh_token")
                .add("refresh_token", refreshToken)
                .build()
            val req = Request.Builder()
                .url(refreshUrl)
                .post(formBody)
                .build()
            val resp = client.newCall(req).execute()
            val body = resp.body?.string() ?: ""
            if (resp.isSuccessful && body.isNotEmpty()) {
                val json = gson.fromJson(body, JsonObject::class.java)
                return@withContext json?.get("id_token")?.asString
            }
        } catch (_: Exception) {}
        null
    }

    /**
     * Dapatkan ID Token & Refresh Token baru dari server backend berdasarkan UID pengguna
     */
    suspend fun requestSessionToken(uid: String): Pair<String, String>? = withContext(Dispatchers.IO) {
        if (uid.isBlank()) return@withContext null
        val activeUser = AccountManager.getActiveUser()
        val payload = JsonObject().apply {
            addProperty("uid", uid.trim())
            if (!activeUser?.refreshToken.isNullOrBlank()) {
                addProperty("refreshToken", activeUser?.refreshToken)
            }
        }
        val res = postApi("/api/auth/android/session-token", payload.toString(), bearerToken = activeUser?.idToken)
        res.getOrNull()?.let { json ->
            try {
                val obj = gson.fromJson(json, JsonObject::class.java)
                val idToken = obj.get("idToken")?.asString
                val refreshToken = obj.get("refreshToken")?.asString ?: activeUser?.refreshToken
                if (!idToken.isNullOrBlank()) {
                    return@withContext Pair(idToken, refreshToken ?: "")
                }
            } catch (_: Exception) {}
        }
        null
    }

    private const val RTDB_BASE = "https://nux-production-default-rtdb.asia-southeast1.firebasedatabase.app"

    /**
     * 4. Aktivasi Akun Menggunakan Key Lisensi
     */
    suspend fun activateLicense(
        uid: String,
        licenseKey: String,
        userToken: String? = null
    ): Result<ActivateResult> = withContext(Dispatchers.IO) {
        val cleanKey = licenseKey.trim().uppercase()
        val payload = JsonObject().apply {
            addProperty("uid", uid.trim())
            addProperty("licenseKey", cleanKey)
        }

        // 1. Coba verifikasi & aktivasi via API backend
        val result = postApi("/api/auth/android/activate", payload.toString(), bearerToken = userToken)
        result.fold(
            onSuccess = { json ->
                try {
                    val obj = gson.fromJson(json, JsonObject::class.java)
                    val isSuccess = obj.get("success")?.asBoolean ?: false
                    val isActivated = obj.get("isActivated")?.asBoolean ?: false
                    val errorMsg = when {
                        obj.has("error") -> obj.get("error").asString
                        obj.has("message") && !isSuccess -> obj.get("message").asString
                        else -> null
                    }

                    if (!isSuccess || !isActivated || errorMsg != null) {
                        return@withContext Result.failure(
                            Exception(errorMsg ?: "Key lisensi tidak valid atau sudah digunakan oleh akun lain.")
                        )
                    }

                    val tier = obj.get("tier")?.asString ?: "lifetime"
                    val expiresAt = obj.get("expiresAt")?.asString ?: "lifetime"
                    val msg = obj.get("message")?.asString ?: "Aktivasi berhasil!"
                    Result.success(
                        ActivateResult(
                            success = true,
                            isActivated = true,
                            tier = tier,
                            expiresAt = expiresAt,
                            message = msg
                        )
                    )
                } catch (e: Exception) {
                    Result.failure(e)
                }
            },
            onFailure = { apiErr ->
                Result.failure(apiErr)
            }
        )
    }

    /**
     * Dapatkan rincian paket langganan aktif dari Firebase Realtime Database
     */
    suspend fun fetchSubscriptionInfo(uid: String, idToken: String? = null): SubscriptionInfo? = withContext(Dispatchers.IO) {
        if (uid.isBlank()) return@withContext null
        try {
            val url = if (!idToken.isNullOrBlank()) {
                "$RTDB_BASE/android/users/$uid/subscription.json?auth=$idToken"
            } else {
                "$RTDB_BASE/android/users/$uid/subscription.json"
            }
            val req = Request.Builder().url(url).get().build()
            val resp = client.newCall(req).execute()
            val body = resp.body?.string() ?: ""
            if (resp.isSuccessful && body.isNotBlank() && body != "null") {
                val obj = gson.fromJson(body, JsonObject::class.java)
                val isActivated = obj.get("isActivated")?.asBoolean ?: false
                val tier = obj.get("tier")?.asString ?: "unactivated"
                val activatedAt = if (obj.has("activatedAt") && !obj.get("activatedAt").isJsonNull) {
                    obj.get("activatedAt").asLong
                } else null
                val expiresAt = if (obj.has("expiresAt") && !obj.get("expiresAt").isJsonNull) {
                    obj.get("expiresAt").asString
                } else null
                val redeemedKey = if (obj.has("redeemedKey") && !obj.get("redeemedKey").isJsonNull) {
                    obj.get("redeemedKey").asString
                } else null

                return@withContext SubscriptionInfo(
                    isActivated = isActivated,
                    tier = tier,
                    activatedAt = activatedAt,
                    expiresAt = expiresAt,
                    redeemedKey = redeemedKey
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        null
    }

    /**
     * 5. Kirim Link Reset Password Menggunakan Layanan Email Bawaan Firebase
     */
    suspend fun sendPasswordResetEmail(email: String): Result<String> {
        val payload = JsonObject().apply {
            addProperty("email", email.trim().lowercase())
        }

        val result = postApi("/api/auth/reset-password", payload.toString())
        return result.mapCatching { json ->
            val obj = gson.fromJson(json, JsonObject::class.java)
            obj.get("message")?.asString
                ?: "Link reset kata sandi telah dikirim oleh Firebase ke email kamu. Silakan periksa Inbox atau folder SPAM kamu."
        }
    }

    suspend fun resetPassword(email: String, code: String = "", newPassword: String = ""): Result<String> {
        return sendPasswordResetEmail(email)
    }

    /**
     * 6. Perbarui Profil Akun (Username & Foto Profil)
     */
    suspend fun updateProfile(
        uid: String,
        newUsername: String? = null,
        photoURL: String? = null
    ): Result<AuthUser> {
        val payload = JsonObject().apply {
            addProperty("uid", uid.trim())
            if (newUsername != null) addProperty("newUsername", newUsername.trim())
            if (photoURL != null) addProperty("photoURL", photoURL.trim())
        }

        val activeUser = AccountManager.getActiveUser()
        var activeToken = activeUser?.idToken
        val refreshToken = activeUser?.refreshToken
        if (!refreshToken.isNullOrBlank()) {
            val refreshed = refreshIdToken(refreshToken)
            if (!refreshed.isNullOrBlank()) {
                activeToken = refreshed
                AccountManager.updateTokens(refreshed, refreshToken)
            }
        }

        val result = postApi("/api/auth/android/update-profile", payload.toString(), bearerToken = activeToken)
        return result.mapCatching { json ->
            val obj = gson.fromJson(json, JsonObject::class.java)
            val userObj = obj.getAsJsonObject("user")
            val updatedUser = AuthUser(
                uid = userObj?.get("uid")?.asString ?: uid,
                username = userObj?.get("username")?.asString ?: (newUsername ?: ""),
                photoURL = userObj?.get("photoURL")?.asString ?: (photoURL ?: "")
            )
            // Synchronize with shared_social public profile
            com.israadev.nuxlauncher.core.social.NuxSocialManager.syncProfileToPublic()
            updatedUser
        }
    }

    /**
     * 7. Unggah Foto Profil (Multi-tier: Update-Server -> ImgBB Backup)
     */
    suspend fun uploadProfileImage(context: Context, imageUri: Uri): Result<String> = withContext(Dispatchers.IO) {
        try {
            val bytes: ByteArray = context.contentResolver.openInputStream(imageUri)?.use { input ->
                val originalBitmap = BitmapFactory.decodeStream(input)
                if (originalBitmap != null) {
                    val maxDimension = 1024
                    val width = originalBitmap.width
                    val height = originalBitmap.height
                    val scaledBitmap = if (width > maxDimension || height > maxDimension) {
                        val ratio = width.toFloat() / height.toFloat()
                        val newWidth = if (width >= height) maxDimension else (maxDimension * ratio).toInt()
                        val newHeight = if (height > width) maxDimension else (maxDimension / ratio).toInt()
                        Bitmap.createScaledBitmap(originalBitmap, newWidth, newHeight, true)
                    } else {
                        originalBitmap
                    }
                    val baos = ByteArrayOutputStream()
                    scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 85, baos)
                    baos.toByteArray()
                } else {
                    null
                }
            } ?: return@withContext Result.failure(Exception("Gagal membaca file gambar dari galeri"))

            val requestBody = bytes.toRequestBody("image/jpeg".toMediaType())

            // 1. Upload ke Server Produksi NUX jika dikonfigurasi
            val uploadServerUrl = NuxConfig.UPLOAD_IMAGE_URL
            if (uploadServerUrl.isNotBlank()) {
                try {
                    val multipartBody = MultipartBody.Builder()
                        .setType(MultipartBody.FORM)
                        .addFormDataPart("image", "avatar_${System.currentTimeMillis()}.jpg", requestBody)
                        .build()

                    val req = Request.Builder()
                        .url(uploadServerUrl)
                        .post(multipartBody)
                        .build()

                    val resp = client.newCall(req).execute()
                    val respStr = resp.body?.string() ?: ""
                    if (resp.isSuccessful) {
                        val json = gson.fromJson(respStr, JsonObject::class.java)
                        val url = json?.get("url")?.asString
                        if (!url.isNullOrEmpty()) {
                            return@withContext Result.success(url)
                        }
                    } else {
                        try {
                            val json = gson.fromJson(respStr, JsonObject::class.java)
                            val err = json?.get("error")?.asString
                            if (!err.isNullOrEmpty()) {
                                return@withContext Result.failure(Exception("Gagal upload: $err"))
                            }
                        } catch (_: Exception) {}
                        return@withContext Result.failure(Exception("Server upload merespon error: ${resp.code}"))
                    }
                } catch (e: Exception) {
                    // Fallback darurat ke ImgBB jika server utama mengalami timeout/down
                }
            }

            // Fallback darurat ke ImgBB jika server utama tidak aktif atau gagal
            try {
                val imgbbBody = MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("image", "avatar_${System.currentTimeMillis()}.jpg", requestBody)
                    .build()

                val imgbbReq = Request.Builder()
                    .url("https://api.imgbb.com/1/upload?key=c3257ef84dcc0d3a9e26f50c15579f06")
                    .post(imgbbBody)
                    .build()

                val imgbbResp = client.newCall(imgbbReq).execute()
                val imgbbStr = imgbbResp.body?.string() ?: ""
                if (imgbbResp.isSuccessful) {
                    val json = gson.fromJson(imgbbStr, JsonObject::class.java)
                    val dataObj = json?.getAsJsonObject("data")
                    val url = dataObj?.get("url")?.asString ?: dataObj?.get("display_url")?.asString
                    if (!url.isNullOrEmpty()) {
                        return@withContext Result.success(url)
                    }
                }
            } catch (_: Exception) {}

            return@withContext Result.failure(Exception("Gagal mengunggah foto profil. Silakan periksa koneksi internet."))

            Result.failure(Exception("Gagal mengunggah foto profil. Silakan periksa koneksi internet."))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
