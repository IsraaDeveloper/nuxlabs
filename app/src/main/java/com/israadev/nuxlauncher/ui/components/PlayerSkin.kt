package com.israadev.nuxlauncher.ui.components

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.util.Base64
import android.util.Log
import android.webkit.ConsoleMessage
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.webkit.WebViewAssetLoader
import com.israadev.nuxlauncher.core.skin.SkinUtils
import java.io.File
import java.io.InputStream

@SuppressLint("SetJavaScriptEnabled")
class PlayerSkin(
    val context: Context,
    localSkinsDir: File = SkinUtils.getSkinsDir(context),
    localCapesDir: File = SkinUtils.getCapesDir(context)
) {
    private val assetLoader = WebViewAssetLoader.Builder()
        .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(context))
        .addPathHandler(
            "/skins/",
            WebViewAssetLoader.InternalStoragePathHandler(context, localSkinsDir)
        )
        .addPathHandler(
            "/capes/",
            WebViewAssetLoader.InternalStoragePathHandler(context, localCapesDir)
        )
        .build()

    private var webview: WebView? = null

    private val skinViewUrl = "https://appassets.androidplatform.net/assets/skinview/skinview.html"
    private val defaultSkinUrl = "https://appassets.androidplatform.net/assets/steve.png"

    fun loadWebView(
        context: Context,
        onPageFinished: () -> Unit = {}
    ): WebView {
        val view = WebView(context).apply {
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                allowFileAccess = true
                allowContentAccess = true
                loadWithOverviewMode = true
                useWideViewPort = true
            }
            setBackgroundColor(Color.TRANSPARENT)
            overScrollMode = WebView.OVER_SCROLL_NEVER
            isVerticalScrollBarEnabled = false
            isHorizontalScrollBarEnabled = false

            webViewClient = object : WebViewClient() {
                override fun shouldInterceptRequest(
                    view: WebView,
                    request: WebResourceRequest
                ): WebResourceResponse? {
                    return assetLoader.shouldInterceptRequest(request.url)
                }

                override fun onPageFinished(view: WebView, url: String) {
                    super.onPageFinished(view, url)
                    onPageFinished()
                }
            }

            webChromeClient = object : WebChromeClient() {
                override fun onConsoleMessage(consoleMessage: ConsoleMessage): Boolean {
                    Log.d("SkinViewerConsole", "${consoleMessage.message()} (L${consoleMessage.lineNumber()})")
                    return true
                }
            }

            loadUrl(skinViewUrl)
        }
        this.webview = view
        return view
    }

    fun loadSkin(file: File?, model: String = "classic") {
        if (file != null && file.exists()) {
            runCatching {
                file.inputStream().use { stream ->
                    loadSkinStream(stream, model)
                }
            }.onFailure {
                loadSkinUrl(defaultSkinUrl, model)
            }
        } else {
            loadSkinUrl(defaultSkinUrl, model)
        }
    }

    fun loadSkinStream(inputStream: InputStream?, model: String = "classic") {
        val modelParam = if (model.lowercase() == "slim") "slim" else "default"
        if (inputStream != null) {
            val bytes = inputStream.readBytes()
            val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
            val dataUrl = "data:image/png;base64,$base64"
            webview?.evaluateJavascript("loadSkin('$dataUrl', '$modelParam')", null)
        } else {
            loadSkinUrl(defaultSkinUrl, modelParam)
        }
    }

    fun loadSkinUrl(url: String, model: String = "classic") {
        val modelParam = if (model.lowercase() == "slim") "slim" else "default"
        webview?.evaluateJavascript("loadSkin('$url', '$modelParam')", null)
    }

    fun loadCape(file: File?) {
        if (file != null && file.exists()) {
            runCatching {
                file.inputStream().use { stream ->
                    val bytes = stream.readBytes()
                    val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
                    val dataUrl = "data:image/png;base64,$base64"
                    webview?.evaluateJavascript("loadCape('$dataUrl')", null)
                }
            }.onFailure {
                webview?.evaluateJavascript("loadCape(null)", null)
            }
        } else {
            webview?.evaluateJavascript("loadCape(null)", null)
        }
    }

    fun loadCapeUrl(url: String?) {
        if (!url.isNullOrBlank()) {
            val safeUrl = if (url.startsWith("http://")) url.replace("http://", "https://") else url
            webview?.evaluateJavascript("loadCape('$safeUrl')", null)
        } else {
            webview?.evaluateJavascript("loadCape(null)", null)
        }
    }

    fun loadAccount(
        account: com.israadev.nuxlauncher.core.models.UserAccount,
        customSkinFile: File? = null,
        customCapeFile: File? = null
    ) {
        val skinFile = customSkinFile ?: SkinUtils.resolveSkinFile(context, account)
        val capeFile = customCapeFile ?: SkinUtils.resolveCapeFile(context, account)
        val model = account.safeSkinModel

        if (skinFile != null && skinFile.exists() && skinFile.length() > 0) {
            loadSkin(skinFile, model)
        } else if (!account.skinUrl.isNullOrBlank()) {
            val safeSkinUrl = if (account.skinUrl.startsWith("http://")) {
                account.skinUrl.replace("http://", "https://")
            } else {
                account.skinUrl
            }
            loadSkinUrl(safeSkinUrl, model)
        } else if (!account.isOffline) {
            loadSkinUrl("https://minotar.net/skin/${account.username}", model)
        } else {
            loadSkin(null, model)
        }

        if (capeFile != null && capeFile.exists() && capeFile.length() > 0) {
            loadCape(capeFile)
        } else {
            loadCape(null)
        }
    }

    fun resetSkin() {
        loadSkinUrl(defaultSkinUrl, "classic")
        webview?.evaluateJavascript("loadCape(null)", null)
    }

    fun startAnim(animName: String = "Walking", speed: Float = 0.8f) {
        webview?.evaluateJavascript("startAnim('$animName', $speed)", null)
    }

    fun setCamera(azimuthDeg: Int = 0, pitchDeg: Int = 10, distance: Int = 60) {
        webview?.evaluateJavascript("setAzimuthAndPitch($azimuthDeg, $pitchDeg, $distance)", null)
    }

    fun setAutoRotate(enable: Boolean, speed: Float = 0.8f) {
        webview?.evaluateJavascript("setAutoRotate($enable, $speed)", null)
    }

    fun resetCamera() {
        webview?.evaluateJavascript("resetCamera()", null)
    }

    fun destroy() {
        webview?.apply {
            stopLoading()
            loadUrl("about:blank")
            clearHistory()
            removeAllViews()
            destroy()
        }
        webview = null
    }
}
