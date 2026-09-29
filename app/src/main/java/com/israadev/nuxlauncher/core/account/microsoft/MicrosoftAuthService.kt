package com.israadev.nuxlauncher.core.account.microsoft

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.israadev.nuxlauncher.core.models.UserAccount
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.UUID
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

data class MicrosoftDeviceCode(
    val deviceCode: String,
    val userCode: String,
    val verificationUri: String,
    val expiresIn: Long,
    val interval: Long
)

object MicrosoftAuthService {
    private const val CLIENT_ID = "c36a9fb6-4f2a-41ff-90bd-ae7cc92031eb"
    private const val SCOPE = "XboxLive.signin offline_access openid profile email"
    private const val TENANT = "/consumers"
    private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

    private val gson = Gson()
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    // Mencegah multiple concurrent polling sessions
    private val isCurrentlyPolling = AtomicBoolean(false)

    /**
     * Memulai alur Microsoft Device Code Authorization
     */
    suspend fun startDeviceCodeFlow(): Result<MicrosoftDeviceCode> = withContext(Dispatchers.IO) {
        try {
            val formBody = FormBody.Builder()
                .add("client_id", CLIENT_ID)
                .add("scope", SCOPE)
                .build()

            val request = Request.Builder()
                .url("https://login.microsoftonline.com$TENANT/oauth2/v2.0/devicecode")
                .post(formBody)
                .build()

            httpClient.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string() ?: ""
                if (!response.isSuccessful) {
                    return@withContext Result.failure(Exception("Gagal memulai login Microsoft (HTTP ${response.code}): $bodyStr"))
                }

                val json = JsonParser.parseString(bodyStr).asJsonObject
                val deviceCode = json.get("device_code")?.asString ?: ""
                val userCode = json.get("user_code")?.asString ?: ""
                val verificationUri = json.get("verification_uri")?.asString ?: "https://microsoft.com/link"
                val expiresIn = json.get("expires_in")?.asLong ?: 900L
                val interval = json.get("interval")?.asLong ?: 5L

                Result.success(
                    MicrosoftDeviceCode(
                        deviceCode = deviceCode,
                        userCode = userCode,
                        verificationUri = verificationUri,
                        expiresIn = expiresIn,
                        interval = interval
                    )
                )
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Melakukan polling ke server Microsoft sampai user mengizinkan di browser/WebView
     */
    suspend fun pollForToken(
        deviceCode: MicrosoftDeviceCode,
        isCancelled: () -> Boolean,
        onStatusUpdate: (String) -> Unit
    ): Result<Pair<String, String>> = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        val timeoutMs = deviceCode.expiresIn * 1000L
        var pollIntervalMs = (if (deviceCode.interval <= 0) 5L else deviceCode.interval) * 1000L
        var consecutiveNetworkFailures = 0

        while (System.currentTimeMillis() - startTime < timeoutMs) {
            if (isCancelled()) {
                return@withContext Result.failure(Exception("Login dibatalkan oleh pengguna."))
            }

            delay(pollIntervalMs)

            if (isCancelled()) {
                return@withContext Result.failure(Exception("Login dibatalkan oleh pengguna."))
            }

            try {
                val formBody = FormBody.Builder()
                    .add("grant_type", "urn:ietf:params:oauth:grant-type:device_code")
                    .add("client_id", CLIENT_ID)
                    .add("device_code", deviceCode.deviceCode)
                    .add("tenant", TENANT)
                    .build()

                val request = Request.Builder()
                    .url("https://login.microsoftonline.com$TENANT/oauth2/v2.0/token")
                    .post(formBody)
                    .build()

                httpClient.newCall(request).execute().use { response ->
                    val bodyStr = response.body?.string() ?: ""
                    val json = runCatching { JsonParser.parseString(bodyStr).asJsonObject }.getOrNull()

                    consecutiveNetworkFailures = 0

                    if (response.isSuccessful && json != null) {
                        val accessToken = json.get("access_token")?.asString
                        val refreshToken = json.get("refresh_token")?.asString ?: ""
                        if (!accessToken.isNullOrBlank()) {
                            onStatusUpdate("Token berhasil diterima! Menghubungkan ke Xbox Live...")
                            return@withContext Result.success(Pair(accessToken, refreshToken))
                        }
                    }

                    if (json != null && json.has("error")) {
                        val err = json.get("error").asString
                        val desc = json.get("error_description")?.asString ?: err

                        when (err) {
                            "authorization_pending" -> {
                                onStatusUpdate("Menunggu otorisasi di halaman web...")
                                // Lanjut polling normal
                            }
                            "slow_down" -> {
                                pollIntervalMs += 2000L
                                onStatusUpdate("Memperlambat frekuensi polling...")
                            }
                            "code_expired" -> {
                                return@withContext Result.failure(Exception("Kode login kedaluwarsa. Silakan mulai ulang."))
                            }
                            "invalid_grant" -> {
                                if (desc.contains("AADSTS70000") || desc.contains("already been used")) {
                                    return@withContext Result.failure(
                                        Exception("Kode otorisasi telah digunakan atau kedaluwarsa. Silakan klik Login untuk mendapatkan kode baru.")
                                    )
                                } else {
                                    return@withContext Result.failure(Exception("Otorisasi ditolak ($err): $desc"))
                                }
                            }
                            else -> {
                                return@withContext Result.failure(Exception("Microsoft OAuth Error: $desc"))
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                // Jangan batalkan polling hanya karena fluktuasi jaringan sementara
                consecutiveNetworkFailures++
                if (consecutiveNetworkFailures >= 6) {
                    return@withContext Result.failure(Exception("Koneksi jaringan terputus berulang kali saat polling. Silakan periksa koneksi internet."))
                }
            }
        }

        Result.failure(Exception("Waktu otorisasi habis (timeout). Silakan coba lagi."))
    }

    /**
     * Menukarkan Token Microsoft menjadi Profil & Token Minecraft Java
     * Mengadopsi arsitektur tangguh Zalith Launcher (XBL fallback, penanganan XErr XSTS lengkap)
     */
    suspend fun completeMinecraftAuth(
        msAccessToken: String,
        msRefreshToken: String,
        onStatusUpdate: (String) -> Unit
    ): Result<UserAccount> = withContext(Dispatchers.IO) {
        try {
            // =========================================================
            // 1. Authenticate with Xbox Live (XBL) dengan Fallback RpsTicket
            // =========================================================
            onStatusUpdate("Autentikasi dengan Xbox Live...")

            fun doXblRequest(rpsTicket: String): Pair<Int, String> {
                val xblPayload = JsonObject().apply {
                    val props = JsonObject().apply {
                        addProperty("AuthMethod", "RPS")
                        addProperty("SiteName", "user.auth.xboxlive.com")
                        addProperty("RpsTicket", rpsTicket)
                    }
                    add("Properties", props)
                    addProperty("RelyingParty", "http://auth.xboxlive.com")
                    addProperty("TokenType", "JWT")
                }

                val xblReq = Request.Builder()
                    .url("https://user.auth.xboxlive.com/user/authenticate")
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .header("x-xbl-contract-version", "1")
                    .post(xblPayload.toString().toRequestBody(JSON_MEDIA_TYPE))
                    .build()

                return httpClient.newCall(xblReq).execute().use { response ->
                    Pair(response.code, response.body?.string() ?: "")
                }
            }

            // Pertama coba dengan prefix "d=" (standar MSA)
            var (xblCode, xblBody) = doXblRequest("d=$msAccessToken")

            // Jika menerima HTTP 400 Bad Request, coba tanpa prefix "d=" (sesuai dokumentasi & implementasi Zalith)
            if (xblCode == 400) {
                val fallbackRes = doXblRequest(msAccessToken)
                xblCode = fallbackRes.first
                xblBody = fallbackRes.second
            }

            if (xblCode !in 200..299) {
                return@withContext Result.failure(Exception("Gagal autentikasi Xbox Live (HTTP $xblCode): $xblBody"))
            }

            val xblJson = JsonParser.parseString(xblBody).asJsonObject
            val xblToken = xblJson.get("Token")?.asString ?: ""
            val claims = xblJson.getAsJsonObject("DisplayClaims")
            val xui = claims?.getAsJsonArray("xui")
            val userHash = xui?.get(0)?.asJsonObject?.get("uhs")?.asString ?: ""

            if (xblToken.isBlank() || userHash.isBlank()) {
                return@withContext Result.failure(Exception("Gagal memperoleh token Xbox Live atau UserHash."))
            }

            // =========================================================
            // 2. Authenticate with Xbox Secure Token Service (XSTS)
            // =========================================================
            onStatusUpdate("Meminta otorisasi XSTS Minecraft...")
            val xstsPayload = JsonObject().apply {
                val props = JsonObject().apply {
                    addProperty("SandboxId", "RETAIL")
                    val tokensArray = com.google.gson.JsonArray().apply { add(xblToken) }
                    add("UserTokens", tokensArray)
                }
                add("Properties", props)
                addProperty("RelyingParty", "rp://api.minecraftservices.com/")
                addProperty("TokenType", "JWT")
            }

            val xstsReq = Request.Builder()
                .url("https://xsts.auth.xboxlive.com/xsts/authorize")
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .header("x-xbl-contract-version", "1")
                .post(xstsPayload.toString().toRequestBody(JSON_MEDIA_TYPE))
                .build()

            var xstsToken = ""
            httpClient.newCall(xstsReq).execute().use { response ->
                val bodyStr = response.body?.string() ?: ""

                if (!response.isSuccessful) {
                    val xErrJson = runCatching { JsonParser.parseString(bodyStr).asJsonObject }.getOrNull()
                    val xErr = xErrJson?.get("XErr")?.asString ?: ""

                    when (xErr) {
                        "2148916227" -> return@withContext Result.failure(Exception("Akun ini telah dilarang/diblokir dari Xbox Live."))
                        "2148916229" -> return@withContext Result.failure(Exception("Akun dibatasi oleh kontrol orang tua (Restricted)."))
                        "2148916233" -> return@withContext Result.failure(Exception("Akun Microsoft ini belum memiliki profil Xbox. Silakan buat gamertag di xbox.com terlebih dahulu."))
                        "2148916234" -> return@withContext Result.failure(Exception("Akun belum menyetujui Ketentuan Layanan Xbox Live."))
                        "2148916235" -> return@withContext Result.failure(Exception("Layanan Xbox Live tidak tersedia di negara/wilayah akun Anda."))
                        "2148916236" -> return@withContext Result.failure(Exception("Akun memerlukan verifikasi bukti usia dewasa (Proof of Age)."))
                        "2148916237" -> return@withContext Result.failure(Exception("Batas waktu bermain harian Xbox Live telah tercapai."))
                        "2148916238" -> return@withContext Result.failure(Exception("Akun di bawah umur (Child Account). Harus ditautkan ke akun keluarga (Family Safety) oleh orang tua."))
                        else -> {
                            val msg = xErrJson?.get("Message")?.asString ?: bodyStr
                            return@withContext Result.failure(Exception("Otorisasi XSTS gagal (HTTP ${response.code}): $msg"))
                        }
                    }
                }

                val json = JsonParser.parseString(bodyStr).asJsonObject
                xstsToken = json.get("Token")?.asString ?: ""
            }

            if (xstsToken.isBlank()) {
                return@withContext Result.failure(Exception("Gagal memperoleh token XSTS."))
            }

            // =========================================================
            // 3. Login with Xbox to Mojang Minecraft Services
            // =========================================================
            onStatusUpdate("Menghubungkan ke Mojang Minecraft Services...")
            val mojangPayload = JsonObject().apply {
                addProperty("identityToken", "XBL3.0 x=$userHash;$xstsToken")
                addProperty("ensureLegacyEnabled", true)
            }

            val mojangReq = Request.Builder()
                .url("https://api.minecraftservices.com/authentication/login_with_xbox")
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .post(mojangPayload.toString().toRequestBody(JSON_MEDIA_TYPE))
                .build()

            var mcAccessToken = ""
            httpClient.newCall(mojangReq).execute().use { response ->
                val bodyStr = response.body?.string() ?: ""
                if (!response.isSuccessful) {
                    return@withContext Result.failure(Exception("Gagal login ke layanan Minecraft (${response.code}): $bodyStr"))
                }
                val json = JsonParser.parseString(bodyStr).asJsonObject
                mcAccessToken = json.get("access_token")?.asString ?: ""
            }

            if (mcAccessToken.isBlank()) {
                return@withContext Result.failure(Exception("Layanan Minecraft tidak mengembalikan token akses."))
            }

            // =========================================================
            // 4. Fetch Minecraft Profile
            // =========================================================
            onStatusUpdate("Mengambil profil pemain Minecraft Java...")
            val profileReq = Request.Builder()
                .url("https://api.minecraftservices.com/minecraft/profile")
                .header("Authorization", "Bearer $mcAccessToken")
                .get()
                .build()

            httpClient.newCall(profileReq).execute().use { response ->
                val bodyStr = response.body?.string() ?: ""
                if (response.code == 404) {
                    return@withContext Result.failure(Exception("Akun Microsoft ini belum memiliki lisensi game Minecraft: Java Edition."))
                }
                if (!response.isSuccessful) {
                    return@withContext Result.failure(Exception("Gagal mengambil profil Minecraft (${response.code}): $bodyStr"))
                }

                val json = JsonParser.parseString(bodyStr).asJsonObject
                val profileId = json.get("id")?.asString ?: UUID.randomUUID().toString()
                val profileName = json.get("name")?.asString ?: "Player"

                var skinUrl: String? = null
                val skinsArray = json.getAsJsonArray("skins")
                if (skinsArray != null && skinsArray.size() > 0) {
                    val firstSkin = skinsArray.get(0).asJsonObject
                    if (firstSkin.has("url")) {
                        skinUrl = firstSkin.get("url").asString
                    }
                }

                val account = UserAccount(
                    id = "ms_$profileId",
                    username = profileName,
                    uuid = profileId,
                    accessToken = mcAccessToken,
                    refreshToken = msRefreshToken.ifBlank { "0" },
                    isOffline = false,
                    accountType = "microsoft",
                    xuid = userHash,
                    skinUrl = skinUrl
                )

                Result.success(account)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Memperbarui sesi akun Microsoft yang sudah tersimpan menggunakan refresh_token
     */
    suspend fun refreshMicrosoftAccount(account: UserAccount): Result<UserAccount> = withContext(Dispatchers.IO) {
        if (account.safeRefreshToken.isBlank() || account.safeRefreshToken == "0") {
            return@withContext Result.failure(Exception("Tidak ada refresh token tersimpan."))
        }

        try {
            val formBody = FormBody.Builder()
                .add("grant_type", "refresh_token")
                .add("client_id", CLIENT_ID)
                .add("scope", SCOPE)
                .add("refresh_token", account.safeRefreshToken)
                .add("tenant", TENANT)
                .build()

            val request = Request.Builder()
                .url("https://login.microsoftonline.com$TENANT/oauth2/v2.0/token")
                .post(formBody)
                .build()

            var newAccessToken = ""
            var newRefreshToken = account.safeRefreshToken

            httpClient.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string() ?: ""
                if (!response.isSuccessful) {
                    return@withContext Result.failure(Exception("Gagal memperbarui token Microsoft (${response.code}): $bodyStr"))
                }
                val json = JsonParser.parseString(bodyStr).asJsonObject
                newAccessToken = json.get("access_token")?.asString ?: ""
                val ref = json.get("refresh_token")?.asString
                if (!ref.isNullOrBlank()) newRefreshToken = ref
            }

            if (newAccessToken.isBlank()) {
                return@withContext Result.failure(Exception("Token pembaruan kosong."))
            }

            completeMinecraftAuth(newAccessToken, newRefreshToken) { _ -> }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
