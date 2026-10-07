package com.israadev.nuxlauncher.core.social

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.reflect.TypeToken
import com.israadev.nuxlauncher.core.account.AccountManager
import com.israadev.nuxlauncher.core.auth.AuthService
import com.israadev.nuxlauncher.core.auth.AuthUser
import com.israadev.nuxlauncher.core.network.NuxConfig
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 * Manajer Sosial Lintas Platform NUX Launcher (Android & Windows)
 * Sinkronisasi data real-time via Firebase Realtime Database (RTDB)
 * Node shared_social: public_profiles, friends, chats, chat_meta, typing, voice_rooms
 */
object NuxSocialManager {
    private const val TAG = "NuxSocialManager"
    private const val RTDB_BASE = "https://nux-production-default-rtdb.asia-southeast1.firebasedatabase.app"
    private val JSON_MEDIA = "application/json; charset=utf-8".toMediaType()

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // --- State Flows ---
    private val _friends = MutableStateFlow<List<NuxFriend>>(emptyList())
    val friends: StateFlow<List<NuxFriend>> = _friends.asStateFlow()

    private val _activeChatFriend = MutableStateFlow<NuxFriend?>(null)
    val activeChatFriend: StateFlow<NuxFriend?> = _activeChatFriend.asStateFlow()

    private val _chatMessages = MutableStateFlow<List<NuxChatMessage>>(emptyList())
    val chatMessages: StateFlow<List<NuxChatMessage>> = _chatMessages.asStateFlow()

    private val _isFriendTyping = MutableStateFlow(false)
    val isFriendTyping: StateFlow<Boolean> = _isFriendTyping.asStateFlow()

    private val _voiceRooms = MutableStateFlow<List<NuxVoiceRoom>>(emptyList())
    val voiceRooms: StateFlow<List<NuxVoiceRoom>> = _voiceRooms.asStateFlow()

    private val _activeVoiceRoom = MutableStateFlow<NuxVoiceRoom?>(null)
    val activeVoiceRoom: StateFlow<NuxVoiceRoom?> = _activeVoiceRoom.asStateFlow()

    private val _voiceMessages = MutableStateFlow<List<NuxVoiceMessage>>(emptyList())
    val voiceMessages: StateFlow<List<NuxVoiceMessage>> = _voiceMessages.asStateFlow()

    private val _searchResults = MutableStateFlow<List<NuxUserProfile>>(emptyList())
    val searchResults: StateFlow<List<NuxUserProfile>> = _searchResults.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    private val _onlineRecommendations = MutableStateFlow<List<NuxUserProfile>>(emptyList())
    val onlineRecommendations: StateFlow<List<NuxUserProfile>> = _onlineRecommendations.asStateFlow()

    private val _isLoadingRecommendations = MutableStateFlow(false)
    val isLoadingRecommendations: StateFlow<Boolean> = _isLoadingRecommendations.asStateFlow()

    // Jobs
    private var syncJob: Job? = null
    private var chatJob: Job? = null
    private var voiceRoomSyncJob: Job? = null
    private var typingJob: Job? = null
    private var presenceHeartbeatJob: Job? = null

    // Cache
    private data class CachedProfile(val profile: NuxUserProfile, val timestamp: Long)
    private val profileCache = ConcurrentHashMap<String, CachedProfile>()

    fun getCurrentUser(): AuthUser? = AccountManager.launcherUser.value

    fun notifyGameExitSync() {
        isInGame = false
        val user = getCurrentUser() ?: return
        try {
            val payload = org.json.JSONObject().apply {
                put("uid", user.uid)
                put("status", "online")
                put("platform", "android")
            }
            AuthService.postApiSync("/api/presence", payload.toString(), timeoutSec = 2)
        } catch (_: Throwable) {}
    }

    private var cachedToken: String? = null
    private var tokenExpiry: Long = 0L

    suspend fun getAuthToken(forceRefresh: Boolean = false): String? {
        val user = getCurrentUser() ?: return null
        val now = System.currentTimeMillis()

        // 1. Return cached in-memory token if still valid and not forcing refresh
        if (!forceRefresh && !cachedToken.isNullOrBlank() && now < tokenExpiry) {
            return cachedToken
        }

        // 2. Try Google's securetoken API using user.refreshToken
        val refreshToken = user.refreshToken
        if (refreshToken.isNotBlank()) {
            val freshIdToken = AuthService.refreshIdToken(refreshToken)
            if (!freshIdToken.isNullOrBlank()) {
                cachedToken = freshIdToken
                tokenExpiry = now + 50 * 60 * 1000L // 50 menit
                AccountManager.updateTokens(freshIdToken, refreshToken)
                return freshIdToken
            }
        }

        // 3. Request fresh session tokens from keystore backend via user.uid (Always works for registered users)
        if (user.uid.isNotBlank()) {
            val pair = AuthService.requestSessionToken(user.uid)
            if (pair != null && pair.first.isNotBlank()) {
                cachedToken = pair.first
                tokenExpiry = now + 50 * 60 * 1000L
                AccountManager.updateTokens(pair.first, pair.second)
                return pair.first
            }
        }

        // 4. Fallback to existing user.idToken if present and not expired
        if (!forceRefresh && user.idToken.isNotBlank()) {
            cachedToken = user.idToken
            tokenExpiry = now + 20 * 60 * 1000L
            return user.idToken
        }

        return null
    }

    private fun buildUrl(path: String, token: String?): String {
        return if (!token.isNullOrEmpty()) {
            "$RTDB_BASE/$path.json?auth=$token"
        } else {
            "$RTDB_BASE/$path.json"
        }
    }

    // =========================================================
    // 1. PRESENCE & SINKRONISASI
    // =========================================================

    fun startSync() {
        startPresenceHeartbeat()
        if (syncJob?.isActive == true) return
        syncJob = scope.launch {
            try {
                syncProfileToPublic()
                updateMyPresence()
            } catch (e: Exception) {
                Log.w(TAG, "Initial sync failed", e)
            }

            while (isActive) {
                try {
                    fetchFriends()
                    fetchVoiceRooms()
                } catch (e: Exception) {
                    Log.w(TAG, "Sync loop error", e)
                }
                delay(3500)
            }
        }
    }

    fun stopSync() {
        syncJob?.cancel()
        syncJob = null
        stopPresenceHeartbeat()
        closeChat()
    }

    private var isInGame = false

    fun setInGame(inGame: Boolean) {
        isInGame = inGame
        if (inGame) {
            startPresenceHeartbeat()
            scope.launch {
                try {
                    updateMyPresence("in_game")
                } catch (_: Exception) {}
            }
        } else {
            scope.launch {
                try {
                    updateMyPresence("online")
                } catch (_: Exception) {}
            }
        }
    }

    fun onAppForeground() {
        val user = getCurrentUser() ?: return
        startPresenceHeartbeat()
        scope.launch {
            try {
                val status = if (isInGame) "in_game" else "online"
                updateMyPresence(status)
            } catch (_: Exception) {}
        }
    }

    fun onAppBackground() {
        if (isInGame || _activeVoiceRoom.value != null || NuxVoiceManager.isConnected.value) {
            Log.d(TAG, "App in background but user is in-game or in voice room. Keeping presence alive.")
            if (isInGame) {
                scope.launch {
                    try {
                        updateMyPresence("in_game")
                    } catch (_: Exception) {}
                }
            }
            return
        }
        stopPresenceHeartbeat(setOffline = true)
    }

    fun startPresenceHeartbeat() {
        if (presenceHeartbeatJob?.isActive == true) return
        presenceHeartbeatJob = scope.launch {
            while (isActive) {
                try {
                    val status = if (isInGame) "in_game" else "online"
                    updateMyPresence(status)
                } catch (_: Exception) {}
                delay(15_000L) // 15 detik heartbeat
            }
        }
    }

    fun stopPresenceHeartbeat(setOffline: Boolean = true) {
        presenceHeartbeatJob?.cancel()
        presenceHeartbeatJob = null
        if (setOffline) {
            scope.launch {
                try {
                    updateMyPresence("offline")
                } catch (_: Exception) {}
            }
        }
    }

    suspend fun syncProfileToPublic() = withContext(Dispatchers.IO) {
        val user = getCurrentUser() ?: return@withContext
        val token = getAuthToken()
        val now = System.currentTimeMillis()

        val payload = JSONObject().apply {
            put("uid", user.uid)
            put("username", user.username)
            put("usernameLower", user.username.lowercase())
            put("photoURL", user.photoURL)
            put("platform", "android")
            put("isAndroid", true)
            put("status", "online")
            put("lastOnline", now)
            put("verified", false)
            put("isPremium", false)
            put("updatedAt", now)
        }.toString().toRequestBody(JSON_MEDIA)

        try {
            // shared_social/public_profiles/{uid}
            val req1 = Request.Builder()
                .url(buildUrl("shared_social/public_profiles/${user.uid}", token))
                .patch(payload)
                .build()
            client.newCall(req1).execute().close()

            // android/users/{uid}/profile fallback
            val req2 = Request.Builder()
                .url(buildUrl("android/users/${user.uid}/profile", token))
                .patch(payload)
                .build()
            client.newCall(req2).execute().close()
        } catch (e: Exception) {
            Log.w(TAG, "syncProfileToPublic failed", e)
        }
    }

    suspend fun updateMyPresence(forcedStatus: String? = null) = withContext(Dispatchers.IO) {
        val user = getCurrentUser() ?: return@withContext
        val token = getAuthToken()
        val now = System.currentTimeMillis()
        val status = forcedStatus ?: "online"

        val payload = JSONObject().apply {
            put("status", status)
            put("lastOnline", now)
            put("platform", "android")
            put("isAndroid", true)
        }.toString().toRequestBody(JSON_MEDIA)

        // 1. Direct Firebase RTDB REST
        try {
            val req1 = Request.Builder()
                .url(buildUrl("shared_social/public_profiles/${user.uid}", token))
                .patch(payload)
                .build()
            client.newCall(req1).execute().close()

            val req2 = Request.Builder()
                .url(buildUrl("android/users/${user.uid}/profile", token))
                .patch(payload)
                .build()
            client.newCall(req2).execute().close()
        } catch (_: Exception) {}

        // 2. Server Backend /api/presence via Admin SDK (100% reliably updates DB)
        try {
            val backendPayload = JSONObject().apply {
                put("uid", user.uid)
                put("status", status)
                put("platform", "android")
            }
            AuthService.postApi("/api/presence", backendPayload.toString())
        } catch (_: Exception) {}
    }

    // =========================================================
    // 2. PERTEMANAN (FRIENDS)
    // =========================================================

    suspend fun fetchFriends() = withContext(Dispatchers.IO) {
        val user = getCurrentUser() ?: return@withContext
        val token = getAuthToken()

        try {
            // Cek shared_social/friends/{uid}
            var url = buildUrl("shared_social/friends/${user.uid}", token)
            var resp = client.newCall(Request.Builder().url(url).get().build()).execute()
            var body = resp.body?.string() ?: ""
            resp.close()

            // Fallback ke legacy users/{uid}/friends jika kosong
            if (!resp.isSuccessful || body.isEmpty() || body == "null") {
                url = buildUrl("users/${user.uid}/friends", token)
                resp = client.newCall(Request.Builder().url(url).get().build()).execute()
                body = resp.body?.string() ?: ""
                resp.close()
            }

            if (body.isEmpty() || body == "null") {
                _friends.value = emptyList()
                return@withContext
            }

            val type = object : TypeToken<Map<String, String>>() {}.type
            val friendsMap: Map<String, String> = gson.fromJson(body, type) ?: emptyMap()

            val friendList = mutableListOf<NuxFriend>()
            val now = System.currentTimeMillis()

            for ((fUid, relStatus) in friendsMap) {
                val profile = fetchUserProfile(fUid, token)
                var unreadCount = 0
                var lastMsgTime: Long? = null

                // Cek chat_meta
                try {
                    val metaUrl = buildUrl("shared_social/chat_meta/${user.uid}/$fUid", token)
                    val metaResp = client.newCall(Request.Builder().url(metaUrl).get().build()).execute()
                    val metaBody = metaResp.body?.string() ?: ""
                    metaResp.close()
                    if (metaBody.isNotEmpty() && metaBody != "null") {
                        val metaJson = JSONObject(metaBody)
                        unreadCount = metaJson.optInt("unreadCount", 0)
                        lastMsgTime = if (metaJson.has("lastMessageTime")) metaJson.optLong("lastMessageTime") else null
                    }
                } catch (_: Exception) {}

                val pStatus = profile?.status ?: "offline"
                val pLastOnline = profile?.lastOnline
                val isFresh = if (pLastOnline != null) (now - pLastOnline) < 75_000L else false
                val isOnline = (pStatus == "online" || pStatus == "in_game") && isFresh
                val isInGame = pStatus == "in_game" && isFresh

                friendList.add(
                    NuxFriend(
                        uid = fUid,
                        username = profile?.username ?: "Pengguna",
                        photoUrl = profile?.photoURL ?: "",
                        status = relStatus,
                        lastMessageTime = lastMsgTime,
                        unreadCount = unreadCount,
                        isOnline = isOnline,
                        isInGame = isInGame,
                        lastOnline = pLastOnline,
                        presenceStatus = if (isOnline) pStatus else "offline",
                        platform = profile?.platform ?: "windows",
                        isAndroid = profile?.isAndroid ?: false,
                        isVerified = profile?.isVerified ?: false,
                        isPremium = profile?.isPremium ?: false
                    )
                )
            }

            _friends.value = friendList.sortedWith(
                compareByDescending<NuxFriend> { it.isOnline }
                    .thenByDescending { it.lastMessageTime ?: 0L }
                    .thenBy { it.username.lowercase() }
            )
        } catch (e: Exception) {
            Log.w(TAG, "fetchFriends error", e)
        }
    }

    suspend fun fetchUserProfile(uid: String, token: String?): NuxUserProfile? = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val cached = profileCache[uid]
        if (cached != null && (now - cached.timestamp) < 10_000L) {
            return@withContext cached.profile
        }

        try {
            // 1. shared_social/public_profiles/{uid}
            var url = buildUrl("shared_social/public_profiles/$uid", token)
            var resp = client.newCall(Request.Builder().url(url).get().build()).execute()
            var body = resp.body?.string() ?: ""
            resp.close()

            // 2. Fallback android/users/{uid}/profile
            if (!resp.isSuccessful || body.isEmpty() || body == "null") {
                url = buildUrl("android/users/$uid/profile", token)
                resp = client.newCall(Request.Builder().url(url).get().build()).execute()
                body = resp.body?.string() ?: ""
                resp.close()
            }

            // 3. Fallback windows/users/{uid}/profile
            if (!resp.isSuccessful || body.isEmpty() || body == "null") {
                url = buildUrl("windows/users/$uid/profile", token)
                resp = client.newCall(Request.Builder().url(url).get().build()).execute()
                body = resp.body?.string() ?: ""
                resp.close()
            }

            // 4. Fallback legacy users/{uid}/profile
            if (!resp.isSuccessful || body.isEmpty() || body == "null") {
                url = buildUrl("users/$uid/profile", token)
                resp = client.newCall(Request.Builder().url(url).get().build()).execute()
                body = resp.body?.string() ?: ""
                resp.close()
            }

            if (body.isNotEmpty() && body != "null") {
                val json = JSONObject(body)
                val uname = json.optString("username", "")
                var photo = json.optString("photoURL", json.optString("photoUrl", ""))
                val status = json.optString("status", "offline")
                val lastOnline = if (json.has("lastOnline")) json.optLong("lastOnline") else null
                val platform = json.optString("platform", "windows")
                val isAndroid = json.optBoolean("isAndroid", platform.equals("android", ignoreCase = true))
                val isVerified = json.optBoolean("verified", false) ||
                        json.optBoolean("isVerified", false) ||
                        json.optString("badge", "").equals("verified", ignoreCase = true)
                val isPremium = json.optBoolean("isPremium", false) ||
                        json.has("subscription") && json.optJSONObject("subscription")?.has("tier") == true

                // Fallback avatar jika photo kosong: Cek akun Minecraft user di Firebase
                if (photo.isBlank()) {
                    try {
                        val userUrl = buildUrl("users/$uid", token)
                        val uResp = client.newCall(Request.Builder().url(userUrl).get().build()).execute()
                        val uBody = uResp.body?.string() ?: ""
                        uResp.close()
                        if (uBody.isNotEmpty() && uBody != "null") {
                            val uJson = JSONObject(uBody)
                            if (uJson.has("accounts")) {
                                val accs = uJson.optJSONArray("accounts")
                                if (accs != null && accs.length() > 0) {
                                    for (i in 0 until accs.length()) {
                                        val acc = accs.optJSONObject(i) ?: continue
                                        val mcUuid = acc.optString("uuid", "")
                                        val mcUname = acc.optString("username", "")
                                        if (mcUuid.isNotEmpty()) {
                                            photo = "https://mc-heads.net/avatar/$mcUuid/100"
                                            break
                                        } else if (mcUname.isNotEmpty()) {
                                            photo = "https://mc-heads.net/avatar/$mcUname/100"
                                            break
                                        }
                                    }
                                }
                            }
                        }
                    } catch (_: Exception) {}

                    // Fallback umum menggunakan username
                    if (photo.isBlank() && uname.isNotBlank()) {
                        photo = "https://mc-heads.net/avatar/$uname/100"
                    }

                    // Sinkronkan photoURL kembali ke shared_social/public_profiles/$uid
                    if (photo.isNotBlank()) {
                        try {
                            val syncPayload = JSONObject().apply { put("photoURL", photo) }.toString().toRequestBody(JSON_MEDIA)
                            val syncReq = Request.Builder()
                                .url(buildUrl("shared_social/public_profiles/$uid", token))
                                .patch(syncPayload)
                                .build()
                            client.newCall(syncReq).execute().close()
                        } catch (_: Exception) {}
                    }
                }

                if (uname.isNotEmpty()) {
                    val prof = NuxUserProfile(
                        uid = uid,
                        username = uname,
                        photoURL = photo,
                        status = status,
                        lastOnline = lastOnline,
                        platform = platform,
                        isAndroid = isAndroid,
                        isVerified = isVerified,
                        isPremium = isPremium
                    )
                    profileCache[uid] = CachedProfile(prof, now)
                    return@withContext prof
                }
            }
        } catch (_: Exception) {}
        null
    }

    suspend fun searchUser(query: String) = withContext(Dispatchers.IO) {
        val q = query.trim().lowercase()
        if (q.isEmpty()) {
            _searchResults.value = emptyList()
            return@withContext
        }

        val myUid = getCurrentUser()?.uid ?: ""
        var token = getAuthToken()
        _isSearching.value = true

        val results = mutableListOf<NuxUserProfile>()
        val foundUids = mutableSetOf<String>()
        try {
            var url = buildUrl("shared_social/public_profiles", token)
            var resp = client.newCall(Request.Builder().url(url).get().build()).execute()
            if (resp.code == 401 || resp.code == 403) {
                resp.close()
                token = getAuthToken(forceRefresh = true)
                url = buildUrl("shared_social/public_profiles", token)
                resp = client.newCall(Request.Builder().url(url).get().build()).execute()
            }
            val body = resp.body?.string() ?: ""
            resp.close()

            if (resp.isSuccessful && body.isNotEmpty() && body != "null") {
                val json = JSONObject(body)
                for (key in json.keys()) {
                    if (key == myUid) continue
                    val uObj = json.optJSONObject(key) ?: continue
                    val uname = uObj.optString("username", "")
                    if (uname.lowercase().contains(q)) {
                        val isVerified = uObj.optBoolean("verified", false) || uObj.optBoolean("isVerified", false)
                        val isPremium = uObj.optBoolean("isPremium", false)
                        foundUids.add(key)
                        results.add(
                            NuxUserProfile(
                                uid = key,
                                username = uname,
                                photoURL = uObj.optString("photoURL", ""),
                                status = uObj.optString("status", "offline"),
                                platform = uObj.optString("platform", "windows"),
                                isAndroid = uObj.optBoolean("isAndroid", false),
                                isVerified = isVerified,
                                isPremium = isPremium
                            )
                        )
                    }
                }
            }

            // Fallback: Check global/auth_registry (public read rules) to find users not yet in public_profiles
            try {
                val authRegUrl = "$RTDB_BASE/global/auth_registry.json"
                val regResp = client.newCall(Request.Builder().url(authRegUrl).get().build()).execute()
                val regBody = regResp.body?.string() ?: ""
                regResp.close()
                if (regResp.isSuccessful && regBody.isNotEmpty() && regBody != "null") {
                    val regJson = JSONObject(regBody)
                    for (key in regJson.keys()) {
                        if (key == myUid || foundUids.contains(key)) continue
                        val uObj = regJson.optJSONObject(key) ?: continue
                        val uname = uObj.optString("username", "")
                        if (uname.lowercase().contains(q)) {
                            results.add(
                                NuxUserProfile(
                                    uid = key,
                                    username = uname,
                                    photoURL = uObj.optString("photoURL", ""),
                                    status = "offline",
                                    platform = uObj.optString("platform", "android"),
                                    isAndroid = uObj.optString("platform", "") == "android",
                                    isVerified = uObj.optBoolean("isActivated", false),
                                    isPremium = false
                                )
                            )
                        }
                    }
                }
            } catch (_: Exception) {}

        } catch (_: Exception) {} finally {
            _searchResults.value = results
            _isSearching.value = false
        }
    }

    /**
     * Mengambil rekomendasi pengguna yang sedang online (maksimal 50 user, acak/shuffled).
     * Jika online < 50, tampilkan semua user yang sedang online.
     * Jika tidak ada yang online, tampilkan pengguna terdaftar aktif secara acak.
     */
    suspend fun fetchOnlineRecommendations(maxLimit: Int = 50) = withContext(Dispatchers.IO) {
        val myUid = getCurrentUser()?.uid ?: ""
        var token = getAuthToken()
        _isLoadingRecommendations.value = true

        try {
            val now = System.currentTimeMillis()
            val onlineCandidates = mutableListOf<NuxUserProfile>()
            val otherCandidates = mutableListOf<NuxUserProfile>()
            val processedUids = mutableSetOf<String>()

            // 1. Ambil data public profiles
            var url = buildUrl("shared_social/public_profiles", token)
            var resp = client.newCall(Request.Builder().url(url).get().build()).execute()
            if (resp.code == 401 || resp.code == 403) {
                resp.close()
                token = getAuthToken(forceRefresh = true)
                url = buildUrl("shared_social/public_profiles", token)
                resp = client.newCall(Request.Builder().url(url).get().build()).execute()
            }
            val body = resp.body?.string() ?: ""
            resp.close()

            if (resp.isSuccessful && body.isNotEmpty() && body != "null") {
                val json = JSONObject(body)
                for (key in json.keys()) {
                    if (key == myUid) continue
                    val uObj = json.optJSONObject(key) ?: continue
                    val uname = uObj.optString("username", "")
                    if (uname.isBlank()) continue

                    val pStatus = uObj.optString("status", "offline")
                    val pLastOnline = if (uObj.has("lastOnline")) uObj.optLong("lastOnline") else 0L
                    val isFresh = if (pLastOnline > 0L) (now - pLastOnline) < 300_000L else false
                    val isOnline = (pStatus == "online" || pStatus == "in_game") && (isFresh || pLastOnline == 0L)

                    val isVerified = uObj.optBoolean("verified", false) || uObj.optBoolean("isVerified", false)
                    val isPremium = uObj.optBoolean("isPremium", false)
                    val pPlatform = uObj.optString("platform", "windows")
                    val isAndroid = uObj.optBoolean("isAndroid", pPlatform.equals("android", ignoreCase = true))

                    val profile = NuxUserProfile(
                        uid = key,
                        username = uname,
                        photoURL = uObj.optString("photoURL", ""),
                        status = if (isOnline) pStatus else "offline",
                        lastOnline = if (pLastOnline > 0L) pLastOnline else null,
                        platform = pPlatform,
                        isAndroid = isAndroid,
                        isVerified = isVerified,
                        isPremium = isPremium
                    )
                    processedUids.add(key)
                    if (isOnline) {
                        onlineCandidates.add(profile)
                    } else {
                        otherCandidates.add(profile)
                    }
                }
            }

            // 2. Fallback check global/auth_registry jika kandidat online masih sedikit
            if (onlineCandidates.size < maxLimit) {
                try {
                    val authRegUrl = "$RTDB_BASE/global/auth_registry.json"
                    val regResp = client.newCall(Request.Builder().url(authRegUrl).get().build()).execute()
                    val regBody = regResp.body?.string() ?: ""
                    regResp.close()
                    if (regResp.isSuccessful && regBody.isNotEmpty() && regBody != "null") {
                        val regJson = JSONObject(regBody)
                        for (key in regJson.keys()) {
                            if (key == myUid || processedUids.contains(key)) continue
                            val uObj = regJson.optJSONObject(key) ?: continue
                            val uname = uObj.optString("username", "")
                            if (uname.isBlank()) continue

                            val pStatus = uObj.optString("status", "offline")
                            val isOnline = pStatus == "online" || pStatus == "in_game"
                            val profile = NuxUserProfile(
                                uid = key,
                                username = uname,
                                photoURL = uObj.optString("photoURL", ""),
                                status = if (isOnline) pStatus else "offline",
                                platform = uObj.optString("platform", "android"),
                                isAndroid = true,
                                isVerified = uObj.optBoolean("isActivated", false),
                                isPremium = false
                            )
                            processedUids.add(key)
                            if (isOnline) {
                                onlineCandidates.add(profile)
                            } else {
                                otherCandidates.add(profile)
                            }
                        }
                    }
                } catch (_: Exception) {}
            }

            // Aturan: Tampilkan sampai 50 user yang sedang online (jika < 50 tampilkan semua yang online)
            // Hasil selalu di-acak (shuffled) agar fresh setiap kali dibuka
            val finalResult = if (onlineCandidates.isNotEmpty()) {
                onlineCandidates.shuffled().take(maxLimit)
            } else {
                otherCandidates.shuffled().take(maxLimit)
            }

            _onlineRecommendations.value = finalResult
        } catch (e: Exception) {
            Log.w(TAG, "fetchOnlineRecommendations error", e)
        } finally {
            _isLoadingRecommendations.value = false
        }
    }

    fun shuffleCurrentRecommendations() {
        if (_onlineRecommendations.value.isNotEmpty()) {
            _onlineRecommendations.value = _onlineRecommendations.value.shuffled()
        }
    }

    suspend fun sendFriendRequest(targetUid: String): Result<Unit> = withContext(Dispatchers.IO) {
        val myUid = getCurrentUser()?.uid ?: return@withContext Result.failure(Exception("Belum login"))
        val token = getAuthToken()

        try {
            // Target receives pending_received
            val targetReq = Request.Builder()
                .url(buildUrl("shared_social/friends/$targetUid/$myUid", token))
                .put("\"pending_received\"".toRequestBody(JSON_MEDIA))
                .build()
            client.newCall(targetReq).execute().close()

            // Sender records pending_sent
            val myReq = Request.Builder()
                .url(buildUrl("shared_social/friends/$myUid/$targetUid", token))
                .put("\"pending_sent\"".toRequestBody(JSON_MEDIA))
                .build()
            client.newCall(myReq).execute().close()

            fetchFriends()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun acceptFriendRequest(targetUid: String): Result<Unit> = withContext(Dispatchers.IO) {
        val myUid = getCurrentUser()?.uid ?: return@withContext Result.failure(Exception("Belum login"))
        val token = getAuthToken()

        try {
            val myReq = Request.Builder()
                .url(buildUrl("shared_social/friends/$myUid/$targetUid", token))
                .put("\"accepted\"".toRequestBody(JSON_MEDIA))
                .build()
            client.newCall(myReq).execute().close()

            val targetReq = Request.Builder()
                .url(buildUrl("shared_social/friends/$targetUid/$myUid", token))
                .put("\"accepted\"".toRequestBody(JSON_MEDIA))
                .build()
            client.newCall(targetReq).execute().close()

            fetchFriends()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun removeFriend(targetUid: String): Result<Unit> = withContext(Dispatchers.IO) {
        val myUid = getCurrentUser()?.uid ?: return@withContext Result.failure(Exception("Belum login"))
        val token = getAuthToken()

        try {
            val myReq = Request.Builder()
                .url(buildUrl("shared_social/friends/$myUid/$targetUid", token))
                .delete()
                .build()
            client.newCall(myReq).execute().close()

            val targetReq = Request.Builder()
                .url(buildUrl("shared_social/friends/$targetUid/$myUid", token))
                .delete()
                .build()
            client.newCall(targetReq).execute().close()

            fetchFriends()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun blockUser(targetUid: String): Result<Unit> = withContext(Dispatchers.IO) {
        val myUid = getCurrentUser()?.uid ?: return@withContext Result.failure(Exception("Belum login"))
        val token = getAuthToken()

        try {
            // Catat status "blocked" di daftar teman pengguna sendiri
            val myReq = Request.Builder()
                .url(buildUrl("shared_social/friends/$myUid/$targetUid", token))
                .put("\"blocked\"".toRequestBody(JSON_MEDIA))
                .build()
            client.newCall(myReq).execute().close()

            // Hapus relasi di pihak target
            val targetReq = Request.Builder()
                .url(buildUrl("shared_social/friends/$targetUid/$myUid", token))
                .delete()
                .build()
            client.newCall(targetReq).execute().close()

            // Tutup chat jika sedang aktif
            if (_activeChatFriend.value?.uid == targetUid) {
                closeChat()
            }

            fetchFriends()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun unblockUser(targetUid: String): Result<Unit> = withContext(Dispatchers.IO) {
        removeFriend(targetUid)
    }

    // =========================================================
    // 3. DIRECT CHAT 1-ON-1
    // =========================================================

    fun openChat(friend: NuxFriend) {
        _activeChatFriend.value = friend
        val myUid = getCurrentUser()?.uid ?: return
        val friendUid = friend.uid
        val chatId = if (myUid < friendUid) "${myUid}_${friendUid}" else "${friendUid}_${myUid}"

        chatJob?.cancel()
        chatJob = scope.launch {
            // Reset unread count
            val token = getAuthToken()
            try {
                val resetReq = Request.Builder()
                    .url(buildUrl("shared_social/chat_meta/$myUid/$friendUid/unreadCount", token))
                    .put("0".toRequestBody(JSON_MEDIA))
                    .build()
                client.newCall(resetReq).execute().close()
            } catch (_: Exception) {}

            while (isActive) {
                fetchChatMessages(chatId)
                fetchFriendTypingStatus(chatId, friendUid)
                delay(1200)
            }
        }
    }

    fun closeChat() {
        chatJob?.cancel()
        chatJob = null
        _activeChatFriend.value = null
        _chatMessages.value = emptyList()
        _isFriendTyping.value = false
    }

    private suspend fun fetchChatMessages(chatId: String) = withContext(Dispatchers.IO) {
        val token = getAuthToken()
        try {
            val url = buildUrl("shared_social/chats/$chatId", token)
            val resp = client.newCall(Request.Builder().url(url).get().build()).execute()
            val body = resp.body?.string() ?: ""
            resp.close()

            if (!resp.isSuccessful || body.isEmpty() || body == "null") {
                _chatMessages.value = emptyList()
                return@withContext
            }

            val json = JSONObject(body)
            val msgs = mutableListOf<NuxChatMessage>()
            for (key in json.keys()) {
                val item = json.getJSONObject(key)
                val replyObj = item.optJSONObject("replyTo")
                val reply = if (replyObj != null) {
                    NuxChatReply(
                        id = replyObj.optString("id", ""),
                        senderId = replyObj.optString("senderId", ""),
                        senderName = replyObj.optString("senderName", ""),
                        text = replyObj.optString("text", ""),
                        isVerified = replyObj.optBoolean("isVerified", false)
                    )
                } else null

                msgs.add(
                    NuxChatMessage(
                        id = key,
                        senderId = item.optString("senderId", ""),
                        senderName = item.optString("senderName", ""),
                        senderPhotoURL = item.optString("senderPhotoURL", ""),
                        text = item.optString("text", ""),
                        timestamp = item.optLong("timestamp", 0L),
                        imageUrl = if (item.has("imageUrl")) item.optString("imageUrl", "").ifBlank { null } else null,
                        replyTo = reply,
                        isVerified = item.optBoolean("isVerified", false),
                        isPremium = item.optBoolean("isPremium", false)
                    )
                )
            }

            msgs.sortBy { it.timestamp }
            _chatMessages.value = msgs
        } catch (_: Exception) {}
    }

    private suspend fun fetchFriendTypingStatus(chatId: String, friendUid: String) = withContext(Dispatchers.IO) {
        val token = getAuthToken()
        try {
            val url = buildUrl("shared_social/typing/$chatId/$friendUid", token)
            val resp = client.newCall(Request.Builder().url(url).get().build()).execute()
            val body = resp.body?.string() ?: ""
            resp.close()
            _isFriendTyping.value = body.trim() == "true"
        } catch (_: Exception) {
            _isFriendTyping.value = false
        }
    }

    fun setMyTyping(isTyping: Boolean) {
        val myUid = getCurrentUser()?.uid ?: return
        val friend = _activeChatFriend.value ?: return
        val chatId = if (myUid < friend.uid) "${myUid}_${friend.uid}" else "${friend.uid}_${myUid}"

        typingJob?.cancel()
        typingJob = scope.launch {
            val token = getAuthToken()
            try {
                val url = buildUrl("shared_social/typing/$chatId/$myUid", token)
                val req = if (isTyping) {
                    Request.Builder().url(url).put("true".toRequestBody(JSON_MEDIA)).build()
                } else {
                    Request.Builder().url(url).delete().build()
                }
                client.newCall(req).execute().close()

                if (isTyping) {
                    delay(2500)
                    val resetReq = Request.Builder().url(url).delete().build()
                    client.newCall(resetReq).execute().close()
                }
            } catch (_: Exception) {}
        }
    }

    suspend fun sendChatMessage(
        text: String,
        imageBytes: ByteArray? = null,
        replyTo: NuxChatReply? = null
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val user = getCurrentUser() ?: return@withContext Result.failure(Exception("Belum login"))
        val friend = _activeChatFriend.value ?: return@withContext Result.failure(Exception("Ruang obrolan belum terbuka"))
        val token = getAuthToken()
        val chatId = if (user.uid < friend.uid) "${user.uid}_${friend.uid}" else "${friend.uid}_${user.uid}"

        var uploadedUrl: String? = null
        if (imageBytes != null && imageBytes.isNotEmpty()) {
            val uploadRes = uploadChatImage(imageBytes)
            if (uploadRes.isSuccess) {
                uploadedUrl = uploadRes.getOrNull()
            } else {
                return@withContext Result.failure(uploadRes.exceptionOrNull() ?: Exception("Gagal mengunggah gambar"))
            }
        }

        val now = System.currentTimeMillis()
        val payload = JSONObject().apply {
            put("senderId", user.uid)
            put("senderName", user.username)
            put("senderPhotoURL", user.photoURL)
            put("text", text.trim())
            put("timestamp", now)
            if (!uploadedUrl.isNullOrEmpty()) {
                put("imageUrl", uploadedUrl)
            }
            if (replyTo != null) {
                put("replyTo", JSONObject().apply {
                    put("id", replyTo.id)
                    put("senderId", replyTo.senderId)
                    put("senderName", replyTo.senderName)
                    put("text", replyTo.text.take(120))
                    put("isVerified", replyTo.isVerified)
                })
            }
        }

        try {
            var activeToken = token
            var postReq = Request.Builder()
                .url(buildUrl("shared_social/chats/$chatId", activeToken))
                .post(payload.toString().toRequestBody(JSON_MEDIA))
                .build()
            var resp = client.newCall(postReq).execute()

            // Auto retry on 401 Unauthorized
            if (resp.code == 401) {
                resp.close()
                activeToken = getAuthToken(forceRefresh = true)
                if (!activeToken.isNullOrBlank()) {
                    postReq = Request.Builder()
                        .url(buildUrl("shared_social/chats/$chatId", activeToken))
                        .post(payload.toString().toRequestBody(JSON_MEDIA))
                        .build()
                    resp = client.newCall(postReq).execute()
                }
            }

            val isSuccess = resp.isSuccessful
            val code = resp.code
            resp.close()

            if (!isSuccess) {
                return@withContext Result.failure(Exception("Gagal mengirim pesan (HTTP $code)"))
            }

            // Dapatkan unreadCount saat ini di pihak teman
            var currentUnread = 0
            try {
                val unreadUrl = buildUrl("shared_social/chat_meta/${friend.uid}/${user.uid}/unreadCount", activeToken)
                val unreadResp = client.newCall(Request.Builder().url(unreadUrl).get().build()).execute()
                val unreadStr = unreadResp.body?.string()?.trim() ?: "0"
                unreadResp.close()
                currentUnread = unreadStr.toIntOrNull() ?: 0
            } catch (_: Exception) {}
            val newUnread = currentUnread + 1

            // Update chat_meta user sendiri
            val myMetaPayload = JSONObject().apply {
                put("lastMessageTime", now)
                put("lastMessageText", text.trim().take(60))
            }.toString().toRequestBody(JSON_MEDIA)
            val myMetaReq = Request.Builder()
                .url(buildUrl("shared_social/chat_meta/${user.uid}/${friend.uid}", activeToken))
                .patch(myMetaPayload)
                .build()
            client.newCall(myMetaReq).execute().close()

            // Update chat_meta teman (SHARED_SOCIAL) -> Memicu Notifikasi Pesan di Windows!
            val friendMetaPayload = JSONObject().apply {
                put("lastMessageTime", now)
                put("lastMessageText", text.trim().take(60))
                put("unreadCount", newUnread)
            }.toString().toRequestBody(JSON_MEDIA)
            val friendMetaReq = Request.Builder()
                .url(buildUrl("shared_social/chat_meta/${friend.uid}/${user.uid}", activeToken))
                .patch(friendMetaPayload)
                .build()
            client.newCall(friendMetaReq).execute().close()

            // Update legacy chatMeta untuk kompatibilitas Windows
            try {
                val legMetaReq = Request.Builder()
                    .url(buildUrl("users/${friend.uid}/chatMeta/${user.uid}", activeToken))
                    .patch(friendMetaPayload)
                    .build()
                client.newCall(legMetaReq).execute().close()
            } catch (_: Exception) {}

            setMyTyping(false)
            fetchChatMessages(chatId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun uploadChatImage(bytes: ByteArray): Result<String> = withContext(Dispatchers.IO) {
        try {
            val requestBody = bytes.toRequestBody("image/jpeg".toMediaType())
            val multipart = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("image", "chat_${System.currentTimeMillis()}.jpg", requestBody)
                .build()

            // Primary: NuxConfig.UPLOAD_IMAGE_URL
            val uploadUrl = NuxConfig.UPLOAD_IMAGE_URL
            if (uploadUrl.isNotBlank()) {
                val reqBuilder = Request.Builder()
                    .url(uploadUrl)
                    .post(multipart)
                NuxConfig.applyAuthHeaders(reqBuilder)
                val req = reqBuilder.build()

                val resp = client.newCall(req).execute()
                val body = resp.body?.string() ?: ""
                resp.close()

                if (resp.isSuccessful) {
                    val json = gson.fromJson(body, JsonObject::class.java)
                    val url = json?.get("url")?.asString
                    if (!url.isNullOrEmpty()) return@withContext Result.success(url)
                }
            }
        } catch (_: Exception) {}

        // Fallback: ImgBB
        try {
            val requestBody = bytes.toRequestBody("image/jpeg".toMediaType())
            val imgbbBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("image", "chat_${System.currentTimeMillis()}.jpg", requestBody)
                .build()

            val imgbbReq = Request.Builder()
                .url("https://api.imgbb.com/1/upload?key=c3257ef84dcc0d3a9e26f50c15579f06")
                .post(imgbbBody)
                .build()

            val imgbbResp = client.newCall(imgbbReq).execute()
            val imgbbStr = imgbbResp.body?.string() ?: ""
            imgbbResp.close()

            if (imgbbResp.isSuccessful) {
                val json = gson.fromJson(imgbbStr, JsonObject::class.java)
                val dataObj = json?.getAsJsonObject("data")
                val url = dataObj?.get("url")?.asString ?: dataObj?.get("display_url")?.asString
                if (!url.isNullOrEmpty()) return@withContext Result.success(url)
            }
        } catch (_: Exception) {}

        Result.failure(Exception("Gagal mengunggah lampiran gambar."))
    }

    // =========================================================
    // 4. VOICE ROOMS
    // =========================================================

    suspend fun fetchVoiceRooms() = withContext(Dispatchers.IO) {
        var token = getAuthToken()
        try {
            var url = buildUrl("shared_social/voice_rooms", token)
            var resp = client.newCall(Request.Builder().url(url).get().build()).execute()
            var body = resp.body?.string() ?: ""

            // Auto retry on 401
            if (resp.code == 401) {
                resp.close()
                token = getAuthToken(forceRefresh = true)
                url = buildUrl("shared_social/voice_rooms", token)
                resp = client.newCall(Request.Builder().url(url).get().build()).execute()
                body = resp.body?.string() ?: ""
            }
            resp.close()

            var json: JSONObject? = null
            if (body.isNotEmpty() && body != "null") {
                try {
                    json = JSONObject(body)
                } catch (_: Exception) {}
            }

            // Fallback: baca juga dari root voice_rooms jika shared_social kosong
            if (json == null || json.length() == 0) {
                try {
                    val rootUrl = buildUrl("voice_rooms", token)
                    val rResp = client.newCall(Request.Builder().url(rootUrl).get().build()).execute()
                    val rBody = rResp.body?.string() ?: ""
                    rResp.close()
                    if (rBody.isNotEmpty() && rBody != "null") {
                        json = JSONObject(rBody)
                    }
                } catch (_: Exception) {}
            }

            if (json == null || json.length() == 0) {
                _voiceRooms.value = emptyList()
                return@withContext
            }

            val rooms = mutableListOf<NuxVoiceRoom>()
            val now = System.currentTimeMillis()

            for (roomId in json.keys()) {
                val rObj = json.optJSONObject(roomId) ?: continue
                val participantsJson = rObj.optJSONObject("participants")
                val participants = mutableMapOf<String, NuxParticipant>()

                if (participantsJson != null) {
                    for (pUid in participantsJson.keys()) {
                        val pObj = participantsJson.optJSONObject(pUid) ?: continue
                        val pLastSeen = pObj.optLong("lastSeen", 0L)
                        // Bersihkan peserta android yang stale lebih dari 60s
                        val isStaleAndroid = pObj.optString("platform") == "android" && pLastSeen > 0L && (now - pLastSeen > 60_000L)
                        if (isStaleAndroid) {
                            try {
                                val delUrl = buildUrl("shared_social/voice_rooms/$roomId/participants/$pUid", token)
                                client.newCall(Request.Builder().url(delUrl).delete().build()).execute().close()
                                val rootDelUrl = buildUrl("voice_rooms/$roomId/participants/$pUid", token)
                                client.newCall(Request.Builder().url(rootDelUrl).delete().build()).execute().close()
                            } catch (_: Exception) {}
                            continue
                        }

                        participants[pUid] = NuxParticipant(
                            uid = pUid,
                            username = pObj.optString("username", "Peserta"),
                            photoURL = pObj.optString("photoURL", pObj.optString("photoUrl", "")),
                            isVerified = pObj.optBoolean("verified", false) || pObj.optBoolean("isVerified", false),
                            isPremium = pObj.optBoolean("isPremium", false),
                            platform = pObj.optString("platform", "android"),
                            isAndroid = pObj.optBoolean("isAndroid", true)
                        )
                    }
                }

                // Jika tidak ada peserta, bersihkan room kosong dari shared_social dan root voice_rooms
                if (participants.isEmpty()) {
                    try {
                        val del1 = buildUrl("shared_social/voice_rooms/$roomId", token)
                        client.newCall(Request.Builder().url(del1).delete().build()).execute().close()
                        val del2 = buildUrl("voice_rooms/$roomId", token)
                        client.newCall(Request.Builder().url(del2).delete().build()).execute().close()
                    } catch (_: Exception) {}
                    continue
                }

                rooms.add(
                    NuxVoiceRoom(
                        id = roomId,
                        name = rObj.optString("name", "Voice Room"),
                        hostUid = rObj.optString("hostUid", ""),
                        hostName = rObj.optString("hostName", "Host"),
                        password = rObj.optString("password", ""),
                        maxUsers = rObj.optInt("maxUsers", 5),
                        createdAt = rObj.optLong("createdAt", 0L),
                        participants = participants
                    )
                )
            }

            _voiceRooms.value = rooms.filter { it.participantCount > 0 }

            val curActive = _activeVoiceRoom.value
            if (curActive != null) {
                val updated = rooms.find { it.id == curActive.id }
                if (updated != null) {
                    _activeVoiceRoom.value = updated
                }
            }
        } catch (_: Exception) {}
    }

    suspend fun createVoiceRoom(name: String, password: String, maxUsers: Int): Result<NuxVoiceRoom> = withContext(Dispatchers.IO) {
        val user = getCurrentUser() ?: return@withContext Result.failure(Exception("Belum login"))
        var token = getAuthToken()
        if (token.isNullOrBlank()) {
            token = getAuthToken(forceRefresh = true)
        }
        if (token.isNullOrBlank()) {
            return@withContext Result.failure(Exception("Sesi login berakhir. Harap login kembali."))
        }

        val now = System.currentTimeMillis()

        val payload = JSONObject().apply {
            put("name", name.trim())
            put("hostUid", user.uid)
            put("hostName", user.username)
            put("creatorPlatform", "android")
            put("password", password.trim())
            put("maxUsers", maxUsers.coerceIn(2, 20))
            put("createdAt", now)
            put("participants", JSONObject().apply {
                put(user.uid, JSONObject().apply {
                    put("uid", user.uid)
                    put("username", user.username)
                    put("photoURL", user.photoURL)
                    put("platform", "android")
                    put("isAndroid", true)
                    put("lastSeen", now)
                })
            })
        }

        try {
            var req = Request.Builder()
                .url(buildUrl("shared_social/voice_rooms", token))
                .post(payload.toString().toRequestBody(JSON_MEDIA))
                .build()
            var resp = client.newCall(req).execute()
            var body = resp.body?.string() ?: ""

            // Auto retry on 401 Unauthorized
            if (resp.code == 401) {
                resp.close()
                token = getAuthToken(forceRefresh = true)
                if (!token.isNullOrBlank()) {
                    req = Request.Builder()
                        .url(buildUrl("shared_social/voice_rooms", token))
                        .post(payload.toString().toRequestBody(JSON_MEDIA))
                        .build()
                    resp = client.newCall(req).execute()
                    body = resp.body?.string() ?: ""
                }
            }

            val isSuccess = resp.isSuccessful
            val code = resp.code
            resp.close()

            if (isSuccess && body.isNotEmpty()) {
                val roomId = JSONObject(body).optString("name", "")

                // Mirror ke root voice_rooms seperti pada versi Windows
                if (roomId.isNotEmpty() && !token.isNullOrBlank()) {
                    try {
                        val rootUrl = buildUrl("voice_rooms/$roomId", token)
                        client.newCall(Request.Builder().url(rootUrl).put(payload.toString().toRequestBody(JSON_MEDIA)).build()).execute().close()
                    } catch (_: Exception) {}
                }

                val newRoom = NuxVoiceRoom(
                    id = roomId,
                    name = name.trim(),
                    hostUid = user.uid,
                    hostName = user.username,
                    password = password.trim(),
                    maxUsers = maxUsers,
                    createdAt = now,
                    participants = mapOf(
                        user.uid to NuxParticipant(
                            uid = user.uid,
                            username = user.username,
                            photoURL = user.photoURL,
                            platform = "android",
                            isAndroid = true
                        )
                    )
                )
                joinVoiceRoomInternal(newRoom)
                fetchVoiceRooms()
                Result.success(newRoom)
            } else {
                Result.failure(Exception("Gagal membuat room ($code)"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun joinVoiceRoom(room: NuxVoiceRoom, inputPassword: String = ""): Result<Unit> = withContext(Dispatchers.IO) {
        val user = getCurrentUser() ?: return@withContext Result.failure(Exception("Belum login"))
        var token = getAuthToken()

        if (room.isLocked && room.password != inputPassword) {
            return@withContext Result.failure(Exception("Password voice room salah!"))
        }

        if (room.isFull && !room.participants.containsKey(user.uid)) {
            return@withContext Result.failure(Exception("Voice room sudah penuh!"))
        }

        try {
            val partPayload = JSONObject().apply {
                put("uid", user.uid)
                put("username", user.username)
                put("photoURL", user.photoURL)
                put("platform", "android")
                put("isAndroid", true)
                put("lastSeen", System.currentTimeMillis())
            }

            var req = Request.Builder()
                .url(buildUrl("shared_social/voice_rooms/${room.id}/participants/${user.uid}", token))
                .put(partPayload.toString().toRequestBody(JSON_MEDIA))
                .build()
            var resp = client.newCall(req).execute()

            if (resp.code == 401) {
                resp.close()
                token = getAuthToken(forceRefresh = true)
                if (!token.isNullOrBlank()) {
                    req = Request.Builder()
                        .url(buildUrl("shared_social/voice_rooms/${room.id}/participants/${user.uid}", token))
                        .put(partPayload.toString().toRequestBody(JSON_MEDIA))
                        .build()
                    resp = client.newCall(req).execute()
                }
            }

            val isSuccess = resp.isSuccessful
            val code = resp.code
            resp.close()

            if (!isSuccess) {
                return@withContext Result.failure(Exception("Gagal bergabung ke room ($code)"))
            }

            joinVoiceRoomInternal(room)
            fetchVoiceRooms()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun joinVoiceRoomInternal(room: NuxVoiceRoom) {
        closeChat()
        _activeVoiceRoom.value = room

        voiceRoomSyncJob?.cancel()
        voiceRoomSyncJob = scope.launch {
            while (isActive) {
                fetchVoiceRoomMessages(room.id)
                fetchVoiceRooms()

                val curU = getCurrentUser()
                val tok = getAuthToken()
                if (curU != null) {
                    try {
                        val hbUrl = buildUrl("shared_social/voice_rooms/${room.id}/participants/${curU.uid}", tok)
                        val hbBody = JSONObject().apply {
                            put("lastSeen", System.currentTimeMillis())
                        }.toString().toRequestBody(JSON_MEDIA)
                        client.newCall(Request.Builder().url(hbUrl).patch(hbBody).build()).execute().close()
                    } catch (_: Exception) {}
                }

                delay(2000)
            }
        }

        val user = getCurrentUser()
        if (user != null) {
            NuxVoiceManager.joinVoiceRoom(room.id, user)
        }
    }

    fun leaveVoiceRoom() {
        scope.launch {
            try {
                NuxVoiceManager.leaveVoiceRoom()
            } catch (_: Exception) {}

            val user = getCurrentUser()
            val room = _activeVoiceRoom.value
            val token = getAuthToken()

            voiceRoomSyncJob?.cancel()
            voiceRoomSyncJob = null
            _activeVoiceRoom.value = null
            _voiceMessages.value = emptyList()

            if (user != null && room != null) {
                try {
                    val req = Request.Builder()
                        .url(buildUrl("shared_social/voice_rooms/${room.id}/participants/${user.uid}", token))
                        .delete()
                        .build()
                    client.newCall(req).execute().close()

                    try {
                        val rootPartReq = Request.Builder()
                            .url(buildUrl("voice_rooms/${room.id}/participants/${user.uid}", token))
                            .delete()
                            .build()
                        client.newCall(rootPartReq).execute().close()
                    } catch (_: Exception) {}

                    // Hapus room jika kosong
                    val checkUrl = buildUrl("shared_social/voice_rooms/${room.id}/participants", token)
                    val checkResp = client.newCall(Request.Builder().url(checkUrl).get().build()).execute()
                    val checkBody = checkResp.body?.string() ?: ""
                    checkResp.close()

                    if (checkBody.isEmpty() || checkBody == "null" || checkBody == "{}" || checkBody == "[]") {
                        val delReq = Request.Builder()
                            .url(buildUrl("shared_social/voice_rooms/${room.id}", token))
                            .delete()
                            .build()
                        client.newCall(delReq).execute().close()

                        try {
                            val rootDelReq = Request.Builder()
                                .url(buildUrl("voice_rooms/${room.id}", token))
                                .delete()
                                .build()
                            client.newCall(rootDelReq).execute().close()
                        } catch (_: Exception) {}
                    }
                } catch (_: Exception) {}
            }

            fetchVoiceRooms()
        }
    }

    private suspend fun fetchVoiceRoomMessages(roomId: String) = withContext(Dispatchers.IO) {
        val token = getAuthToken()
        try {
            val url = buildUrl("shared_social/voice_rooms/$roomId/messages", token)
            val resp = client.newCall(Request.Builder().url(url).get().build()).execute()
            val body = resp.body?.string() ?: ""
            resp.close()

            if (!resp.isSuccessful || body.isEmpty() || body == "null") {
                _voiceMessages.value = emptyList()
                return@withContext
            }

            val json = JSONObject(body)
            val msgs = mutableListOf<NuxVoiceMessage>()
            for (key in json.keys()) {
                val item = json.getJSONObject(key)
                msgs.add(
                    NuxVoiceMessage(
                        id = key,
                        senderId = item.optString("senderId", ""),
                        senderName = item.optString("senderName", "Peserta"),
                        senderPhotoURL = item.optString("senderPhotoURL", ""),
                        text = item.optString("text", ""),
                        timestamp = item.optLong("timestamp", 0L),
                        isVerified = item.optBoolean("isVerified", false)
                    )
                )
            }
            msgs.sortBy { it.timestamp }
            _voiceMessages.value = msgs
        } catch (_: Exception) {}
    }

    suspend fun sendVoiceRoomMessage(text: String): Result<Unit> = withContext(Dispatchers.IO) {
        val user = getCurrentUser() ?: return@withContext Result.failure(Exception("Belum login"))
        val room = _activeVoiceRoom.value ?: return@withContext Result.failure(Exception("Tidak dalam voice room"))
        var token = getAuthToken()

        val payload = JSONObject().apply {
            put("senderId", user.uid)
            put("senderName", user.username)
            put("senderPhotoURL", user.photoURL)
            put("text", text.trim())
            put("timestamp", System.currentTimeMillis())
        }

        try {
            var req = Request.Builder()
                .url(buildUrl("shared_social/voice_rooms/${room.id}/messages", token))
                .post(payload.toString().toRequestBody(JSON_MEDIA))
                .build()
            var resp = client.newCall(req).execute()

            // Auto retry on 401 Unauthorized
            if (resp.code == 401) {
                resp.close()
                token = getAuthToken(forceRefresh = true)
                if (!token.isNullOrBlank()) {
                    req = Request.Builder()
                        .url(buildUrl("shared_social/voice_rooms/${room.id}/messages", token))
                        .post(payload.toString().toRequestBody(JSON_MEDIA))
                        .build()
                    resp = client.newCall(req).execute()
                }
            }

            val isSuccess = resp.isSuccessful
            val code = resp.code
            resp.close()

            if (!isSuccess) {
                return@withContext Result.failure(Exception("Gagal mengirim pesan room (HTTP $code)"))
            }

            fetchVoiceRoomMessages(room.id)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
