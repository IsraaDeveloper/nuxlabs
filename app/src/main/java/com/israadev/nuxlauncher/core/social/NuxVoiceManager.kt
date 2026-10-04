package com.israadev.nuxlauncher.core.social

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.util.Log
import android.view.ViewGroup
import android.webkit.*
import androidx.webkit.WebViewAssetLoader
import com.israadev.nuxlauncher.core.auth.AuthUser
import com.israadev.nuxlauncher.core.network.NuxConfig
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

/**
 * Manajer Audio Voice Room berbasis LiveKit WebRTC
 * Menggunakan WebRTC audio bridge di dalam WebView yang dimuat via WebViewAssetLoader (Secure Context)
 */
@SuppressLint("SetJavaScriptEnabled")
object NuxVoiceManager {
    private const val TAG = "NuxVoiceManager"
    private const val LIVEKIT_SERVER_URL = "wss://nux-launcher-9nbr9gp5.livekit.cloud"
    private val TOKEN_API_BASE: String
        get() = NuxConfig.LIVEKIT_TOKEN_URL
    private const val BRIDGE_URL = "https://appassets.androidplatform.net/assets/livekit/livekit_bridge.html"

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private var webView: WebView? = null
    private var isBridgeReady = false
    private var pendingConnectAction: (() -> Unit)? = null

    private val _isConnecting = MutableStateFlow(false)
    val isConnecting: StateFlow<Boolean> = _isConnecting.asStateFlow()

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _isMuted = MutableStateFlow(false)
    val isMuted: StateFlow<Boolean> = _isMuted.asStateFlow()

    private val _activeSpeakers = MutableStateFlow<Set<String>>(emptySet())
    val activeSpeakers: StateFlow<Set<String>> = _activeSpeakers.asStateFlow()

    private val _currentRoomId = MutableStateFlow<String?>(null)
    val currentRoomId: StateFlow<String?> = _currentRoomId.asStateFlow()

    fun init(context: Context) {
        if (webView != null) {
            attachToActivityIfNeeded(context)
            return
        }
        scope.launch {
            try {
                val wv = WebView(context)
                initWebView(wv, context)
                webView = wv
            } catch (e: Exception) {
                Log.e(TAG, "init failed", e)
            }
        }
    }

    fun keepAliveInBackground() {
        try {
            webView?.resumeTimers()
        } catch (_: Exception) {}
    }

    private fun attachToActivityIfNeeded(context: Context) {
        val activity = context as? Activity
        if (activity != null && webView != null && webView?.parent == null) {
            val decorView = activity.window?.decorView as? ViewGroup
            if (decorView != null) {
                val params = ViewGroup.LayoutParams(1, 1)
                webView?.alpha = 0.01f
                decorView.addView(webView, params)
                Log.d(TAG, "WebView attached to Activity window decorView")
            }
        }
    }

    private fun initWebView(wv: WebView, context: Context) {
        try {
            WebView.setWebContentsDebuggingEnabled(true)
        } catch (_: Exception) {}

        val assetLoader = WebViewAssetLoader.Builder()
            .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(context.applicationContext))
            .build()

        wv.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            mediaPlaybackRequiresUserGesture = false
            cacheMode = WebSettings.LOAD_DEFAULT
            allowFileAccess = true
            allowContentAccess = true
        }

        wv.webChromeClient = object : WebChromeClient() {
            override fun onPermissionRequest(request: PermissionRequest?) {
                Log.i(TAG, "WebChromeClient onPermissionRequest: ${request?.resources?.joinToString()}")
                try {
                    request?.grant(request.resources)
                } catch (e: Exception) {
                    Log.e(TAG, "Error granting WebChromeClient permission", e)
                }
            }

            override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                val msg = consoleMessage?.message() ?: ""
                val line = consoleMessage?.lineNumber() ?: 0
                val src = consoleMessage?.sourceId() ?: ""
                Log.i("LiveKitBridge", "[$src:$line] $msg")
                return true
            }
        }

        wv.webViewClient = object : WebViewClient() {
            override fun shouldInterceptRequest(
                view: WebView,
                request: WebResourceRequest
            ): WebResourceResponse? {
                return assetLoader.shouldInterceptRequest(request.url)
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                Log.i(TAG, "LiveKit bridge onPageFinished: $url")
                isBridgeReady = true
                pendingConnectAction?.invoke()
                pendingConnectAction = null
            }

            override fun onReceivedError(
                view: WebView?,
                request: WebResourceRequest?,
                error: WebResourceError?
            ) {
                super.onReceivedError(view, request, error)
                Log.e(TAG, "LiveKit bridge onReceivedError: ${error?.description} for ${request?.url}")
            }
        }

        wv.addJavascriptInterface(VoiceBridgeInterface(), "AndroidVoiceBridge")

        // Attach to Activity window so WebRTC and Audio have a real window context
        val activity = context as? Activity
        if (activity != null) {
            val decorView = activity.window?.decorView as? ViewGroup
            if (decorView != null && wv.parent == null) {
                val params = ViewGroup.LayoutParams(1, 1)
                wv.alpha = 0.01f
                decorView.addView(wv, params)
                Log.i(TAG, "WebView initially attached to Activity window decorView")
            }
        }

        Log.i(TAG, "Loading LiveKit bridge URL: $BRIDGE_URL")
        wv.loadUrl(BRIDGE_URL)
    }

    /**
     * Terhubung ke voice room menggunakan token dari server NUX
     */
    fun joinVoiceRoom(roomId: String, user: AuthUser) {
        Log.i(TAG, "joinVoiceRoom called for roomId: $roomId, user: ${user.username}")
        if (TOKEN_API_BASE.isBlank()) {
            Log.w(TAG, "LiveKit token API is not configured in local.properties")
            return
        }
        _isConnecting.value = true
        _currentRoomId.value = roomId

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val metaObj = JSONObject().apply {
                    put("name", user.username)
                    put("photoURL", user.photoURL)
                    put("platform", "android")
                    put("isAndroid", true)
                    put("verified", user.isActivated)
                    put("isPremium", false)
                }

                val encodedUid = URLEncoder.encode(user.uid, "UTF-8")
                val encodedMeta = URLEncoder.encode(metaObj.toString(), "UTF-8")
                val tokenUrl = "$TOKEN_API_BASE?room=$roomId&participant=$encodedUid&metadata=$encodedMeta"
                Log.i(TAG, "Requesting LiveKit token from: $tokenUrl")

                val reqBuilder = Request.Builder()
                    .url(tokenUrl)
                    .header("User-Agent", "Mozilla/5.0 (Linux; Android) NuxLauncher/2.5")
                    .get()
                NuxConfig.applyAuthHeaders(reqBuilder)
                val req = reqBuilder.build()

                val resp = client.newCall(req).execute()
                val body = resp.body?.string() ?: ""
                resp.close()

                if (!resp.isSuccessful || body.isEmpty()) {
                    Log.e(TAG, "Failed to get LiveKit token: HTTP ${resp.code} body: $body")
                    withContext(Dispatchers.Main) {
                        _isConnecting.value = false
                    }
                    return@launch
                }

                val tokenJson = JSONObject(body)
                val token = tokenJson.optString("token", "")
                if (token.isEmpty()) {
                    Log.e(TAG, "LiveKit token is empty in response: $body")
                    withContext(Dispatchers.Main) {
                        _isConnecting.value = false
                    }
                    return@launch
                }

                Log.i(TAG, "LiveKit token received. Calling connectVoice in WebView...")
                withContext(Dispatchers.Main) {
                    val connectAction: () -> Unit = {
                        Log.i(TAG, "Invoking connectVoice in WebView...")
                        webView?.evaluateJavascript("connectVoice('$LIVEKIT_SERVER_URL', '$token');") { result ->
                            Log.i(TAG, "evaluateJavascript connectVoice evaluated: $result")
                        }
                    }

                    if (isBridgeReady && webView != null) {
                        connectAction()
                    } else {
                        Log.i(TAG, "Bridge not yet ready, saving pendingConnectAction")
                        pendingConnectAction = connectAction
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "joinVoiceRoom error", e)
                withContext(Dispatchers.Main) {
                    _isConnecting.value = false
                }
            }
        }
    }

    /**
     * Putuskan koneksi audio dan keluar dari voice room
     */
    fun leaveVoiceRoom() {
        Log.d(TAG, "leaveVoiceRoom called")
        scope.launch {
            webView?.evaluateJavascript("disconnectVoice();", null)
            _isConnected.value = false
            _isConnecting.value = false
            _activeSpeakers.value = emptySet()
            _currentRoomId.value = null
            _isMuted.value = false
        }
    }

    /**
     * Atur status bisu (mute/unmute) mikrofon
     */
    fun setMuted(muted: Boolean) {
        _isMuted.value = muted
        scope.launch {
            webView?.evaluateJavascript("setMute($muted);", null)
        }
    }

    fun toggleMute() {
        setMuted(!_isMuted.value)
    }

    private class VoiceBridgeInterface {
        @JavascriptInterface
        fun onConnected() {
            Log.i(TAG, "VoiceBridgeInterface: onConnected received from JS!")
            scope.launch {
                _isConnected.value = true
                _isConnecting.value = false
            }
        }

        @JavascriptInterface
        fun onDisconnected() {
            Log.i(TAG, "VoiceBridgeInterface: onDisconnected received from JS")
            scope.launch {
                _isConnected.value = false
                _isConnecting.value = false
                _activeSpeakers.value = emptySet()
            }
        }

        @JavascriptInterface
        fun onActiveSpeakersChanged(speakersJson: String) {
            scope.launch {
                try {
                    val arr = JSONArray(speakersJson)
                    val set = mutableSetOf<String>()
                    for (i in 0 until arr.length()) {
                        set.add(arr.getString(i))
                    }
                    _activeSpeakers.value = set
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing active speakers JSON", e)
                }
            }
        }

        @JavascriptInterface
        fun onError(error: String) {
            Log.e(TAG, "VoiceBridgeInterface: onError received: $error")
            scope.launch {
                _isConnecting.value = false
            }
        }
    }
}
