package com.israadev.nuxlauncher.core.account.elyby

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.israadev.nuxlauncher.core.account.AccountManager
import com.israadev.nuxlauncher.core.models.UserAccount
import com.israadev.nuxlauncher.core.skin.SkinUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.UUID
import java.util.concurrent.TimeUnit

data class ElyByDeviceCode(
    val deviceCode: String,
    val userCode: String,
    val verificationUri: String,
    val expiresIn: Long,
    val interval: Long
) {
    val verificationUrlWithCode: String
        get() {
            val base = if (verificationUri.isNotBlank()) {
                if (verificationUri.startsWith("http://")) verificationUri.replace("http://", "https://")
                else verificationUri
            } else {
                "https://account.ely.by/code"
            }
            return if (base.contains("?")) {
                "$base&user_code=$userCode"
            } else {
                "$base?user_code=$userCode"
            }
        }
}

sealed class ElyByLoginState {
    object Idle : ElyByLoginState()
    object Starting : ElyByLoginState()
    data class WaitingForApproval(
        val deviceCode: ElyByDeviceCode,
        val statusMsg: String
    ) : ElyByLoginState()
    data class Success(val account: UserAccount) : ElyByLoginState()
    data class Error(val message: String) : ElyByLoginState()
}

object ElyByAuthService {
    const val AUTHLIB_INJECTOR_URL = "https://authserver.ely.by/api/authlib-injector"
    private const val CLIENT_ID = "ely"
    private const val SCOPE = "account_info minecraft_server_session"
    private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

    private val gson = Gson()
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var activeJob: Job? = null

    private val _loginState = MutableStateFlow<ElyByLoginState>(ElyByLoginState.Idle)
    val loginState: StateFlow<ElyByLoginState> = _loginState.asStateFlow()

    fun resetState() {
        activeJob?.cancel()
        activeJob = null
        _loginState.value = ElyByLoginState.Idle
    }

    fun cancelLogin() {
        activeJob?.cancel()
        activeJob = null
        _loginState.value = ElyByLoginState.Idle
    }

    fun openBrowser(context: Context, url: String) {
        runCatching {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
    }

    /**
     * Memulai alur login browser Ely.by OAuth Device Code.
     * Berjalan di serviceScope sehingga tidak terputus saat pengguna berpindah ke browser.
     */
    fun startLogin(context: Context) {
        activeJob?.cancel()
        _loginState.value = ElyByLoginState.Starting

        activeJob = serviceScope.launch {
            try {
                val dcRes = startDeviceCodeFlow()
                if (dcRes.isFailure) {
                    _loginState.value = ElyByLoginState.Error(
                        dcRes.exceptionOrNull()?.message ?: "Gagal memulai otorisasi Ely.by."
                    )
                    return@launch
                }

                val dc = dcRes.getOrThrow()
                _loginState.value = ElyByLoginState.WaitingForApproval(
                    deviceCode = dc,
                    statusMsg = "Menunggu otorisasi di browser Ely.by..."
                )

                // Salin kode ke clipboard
                runCatching {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                    val clip = ClipData.newPlainText("Ely.by User Code", dc.userCode)
                    clipboard?.setPrimaryClip(clip)
                }

                // Buka browser ke URL otorisasi
                openBrowser(context, dc.verificationUrlWithCode)

                // Polling token sampai selesai atau dibatalkan
                val pollRes = pollForToken(
                    deviceCode = dc,
                    isCancelled = { activeJob?.isCancelled == true },
                    onStatusUpdate = { msg ->
                        val current = _loginState.value
                        if (current is ElyByLoginState.WaitingForApproval) {
                            _loginState.value = current.copy(statusMsg = msg)
                        }
                    }
                )

                if (pollRes.isSuccess) {
                    val rawAcc = pollRes.getOrThrow()
                    val cachedAcc = SkinUtils.ensureSkinAndCapeCached(context, rawAcc)
                    AccountManager.addAccount(context, cachedAcc)
                    _loginState.value = ElyByLoginState.Success(cachedAcc)
                } else {
                    _loginState.value = ElyByLoginState.Error(
                        pollRes.exceptionOrNull()?.message ?: "Otorisasi Ely.by gagal."
                    )
                }
            } catch (e: Exception) {
                _loginState.value = ElyByLoginState.Error(
                    e.message ?: "Terjadi kesalahan saat login Ely.by."
                )
            }
        }
    }

    /**
     * Memulai request device code ke Ely.by API
     */
    suspend fun startDeviceCodeFlow(): Result<ElyByDeviceCode> = withContext(Dispatchers.IO) {
        try {
            val formBody = FormBody.Builder()
                .add("client_id", CLIENT_ID)
                .add("scope", SCOPE)
                .build()

            val request = Request.Builder()
                .url("https://account.ely.by/api/oauth2/v1/devicecode")
                .post(formBody)
                .build()

            httpClient.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string() ?: ""
                if (!response.isSuccessful) {
                    return@withContext Result.failure(Exception("Gagal memulai login Ely.by Device Code (${response.code}): $bodyStr"))
                }

                val json = JsonParser.parseString(bodyStr).asJsonObject
                val deviceCode = json.get("device_code")?.asString ?: ""
                val userCode = json.get("user_code")?.asString ?: ""
                val verificationUri = json.get("verification_uri")?.asString ?: "https://account.ely.by/code"
                val expiresIn = json.get("expires_in")?.asLong ?: 600L
                val interval = json.get("interval")?.asLong ?: 5L

                Result.success(
                    ElyByDeviceCode(
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
     * Melakukan polling ke server Ely.by sampai otorisasi browser selesai
     */
    suspend fun pollForToken(
        deviceCode: ElyByDeviceCode,
        isCancelled: () -> Boolean,
        onStatusUpdate: (String) -> Unit
    ): Result<UserAccount> = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        val timeoutMs = deviceCode.expiresIn * 1000L
        var pollIntervalMs = (if (deviceCode.interval <= 0) 5L else deviceCode.interval) * 1000L

        var accessToken: String? = null

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
                    .build()

                val request = Request.Builder()
                    .url("https://account.ely.by/api/oauth2/v1/token")
                    .post(formBody)
                    .build()

                httpClient.newCall(request).execute().use { response ->
                    val bodyStr = response.body?.string() ?: ""
                    val json = runCatching { JsonParser.parseString(bodyStr).asJsonObject }.getOrNull()

                    if (response.isSuccessful && json != null) {
                        val token = json.get("access_token")?.asString
                        if (!token.isNullOrBlank()) {
                            accessToken = token
                        }
                    }

                    if (json != null && json.has("error")) {
                        val err = json.get("error").asString
                        when (err) {
                            "authorization_pending" -> {
                                onStatusUpdate("Menunggu konfirmasi di browser...")
                            }
                            "slow_down" -> {
                                pollIntervalMs += 2000L
                                onStatusUpdate("Memperlambat polling...")
                            }
                            else -> {
                                return@withContext Result.failure(Exception("Ely.by OAuth Error: $err"))
                            }
                        }
                    }
                }

                if (accessToken != null) {
                    break
                }
            } catch (e: Exception) {
                // Abaikan error jaringan sementara
            }
        }

        val token = accessToken ?: return@withContext Result.failure(Exception("Waktu otorisasi Ely.by habis."))

        // Step 2: Ambil info akun (username & email)
        onStatusUpdate("Mengambil informasi akun Ely.by...")
        val infoReq = Request.Builder()
            .url("https://account.ely.by/api/account/v1/info")
            .header("Authorization", "Bearer $token")
            .get()
            .build()

        var username = "Player"
        var email = ""
        httpClient.newCall(infoReq).execute().use { response ->
            val bodyStr = response.body?.string() ?: ""
            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("Gagal mengambil info akun Ely.by (${response.code})"))
            }
            val json = JsonParser.parseString(bodyStr).asJsonObject
            username = json.get("username")?.asString ?: "Player"
            email = json.get("email")?.asString ?: ""
        }

        // Step 3: Ambil Minecraft UUID dari server Ely.by
        onStatusUpdate("Mengambil UUID Minecraft untuk $username...")
        var playerUuid = UUID.nameUUIDFromBytes("OfflinePlayer:$username".toByteArray(Charsets.UTF_8)).toString().replace("-", "")
        try {
            val uuidReq = Request.Builder()
                .url("https://authserver.ely.by/api/users/profiles/minecraft/$username")
                .get()
                .build()

            httpClient.newCall(uuidReq).execute().use { response ->
                if (response.isSuccessful) {
                    val bodyStr = response.body?.string() ?: ""
                    val json = JsonParser.parseString(bodyStr).asJsonObject
                    val id = json.get("id")?.asString
                    if (!id.isNullOrBlank()) {
                        playerUuid = id.replace("-", "")
                    }
                }
            }
        } catch (_: Exception) {}

        val cleanUuid = playerUuid.replace("-", "")
        val skinUrl = "https://skinsystem.ely.by/skins/$username.png"
        val account = UserAccount(
            id = "ely_$cleanUuid",
            username = username,
            uuid = cleanUuid,
            accessToken = token,
            isOffline = false,
            accountType = "elyby",
            authServerUrl = AUTHLIB_INJECTOR_URL,
            email = email,
            skinUrl = skinUrl
        )

        Result.success(account)
    }

    /**
     * Resolves authentic 32-character Ely.by Yggdrasil UUID for username
     */
    fun fetchRealPlayerUuid(username: String): String {
        return try {
            val uuidReq = Request.Builder()
                .url("https://authserver.ely.by/api/users/profiles/minecraft/$username")
                .get()
                .build()
            httpClient.newCall(uuidReq).execute().use { response ->
                if (response.isSuccessful) {
                    val bodyStr = response.body?.string() ?: ""
                    val json = JsonParser.parseString(bodyStr).asJsonObject
                    json.get("id")?.asString?.replace("-", "") ?: ""
                } else ""
            }
        } catch (_: Exception) {
            ""
        }
    }

    /**
     * Fallback login langsung (tidak ditampilkan di UI, disimpan untuk kompatibilitas)
     */
    suspend fun loginWithCredentials(
        usernameOrEmail: String,
        password: String
    ): Result<UserAccount> = withContext(Dispatchers.IO) {
        try {
            val clientToken = UUID.randomUUID().toString().replace("-", "")
            val payload = JsonObject().apply {
                val agent = JsonObject().apply {
                    addProperty("name", "Minecraft")
                    addProperty("version", 1)
                }
                add("agent", agent)
                addProperty("username", usernameOrEmail.trim())
                addProperty("password", password)
                addProperty("requestUser", true)
                addProperty("clientToken", clientToken)
            }

            val request = Request.Builder()
                .url("$AUTHLIB_INJECTOR_URL/authserver/authenticate")
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .post(payload.toString().toRequestBody(JSON_MEDIA_TYPE))
                .build()

            httpClient.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string() ?: ""
                val json = runCatching { JsonParser.parseString(bodyStr).asJsonObject }.getOrNull()

                if (json != null && json.has("errorMessage")) {
                    val errMsg = json.get("errorMessage").asString
                    return@withContext Result.failure(Exception("Ely.by: $errMsg"))
                }

                if (!response.isSuccessful || json == null) {
                    return@withContext Result.failure(Exception("Gagal login ke Ely.by (HTTP ${response.code}): $bodyStr"))
                }

                val accessToken = json.get("accessToken")?.asString ?: "0"
                val resClientToken = json.get("clientToken")?.asString ?: clientToken
                val selectedProfile = json.getAsJsonObject("selectedProfile")
                    ?: return@withContext Result.failure(Exception("Profil Ely.by tidak ditemukan dalam respon."))

                val profileId = selectedProfile.get("id")?.asString ?: UUID.randomUUID().toString().replace("-", "")
                val profileName = selectedProfile.get("name")?.asString ?: usernameOrEmail.trim()
                val skinUrl = "https://skinsystem.ely.by/skins/$profileName.png"

                val account = UserAccount(
                    id = "ely_${profileId.replace("-", "")}",
                    username = profileName,
                    uuid = profileId.replace("-", ""),
                    accessToken = accessToken,
                    refreshToken = resClientToken,
                    isOffline = false,
                    accountType = "elyby",
                    authServerUrl = AUTHLIB_INJECTOR_URL,
                    email = usernameOrEmail.trim(),
                    skinUrl = skinUrl
                )

                Result.success(account)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
