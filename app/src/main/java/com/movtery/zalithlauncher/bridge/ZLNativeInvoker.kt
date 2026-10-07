package com.movtery.zalithlauncher.bridge

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.annotation.Keep

@Keep
object ZLNativeInvoker {
    @JvmStatic
    var onExitCallback: ((exitCode: Int, isSignal: Boolean) -> Unit)? = null

    @JvmStatic
    var appContext: Context? = null

    private val mainHandler = Handler(Looper.getMainLooper())

    @Keep
    @JvmStatic
    fun jvmExit(exitCode: Int, isSignal: Boolean) {
        onExitCallback?.invoke(exitCode, isSignal)
    }

    @Keep
    @JvmStatic
    fun openLink(link: String) {
        appContext?.let { ctx ->
            mainHandler.post {
                try {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(link)).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    ctx.startActivity(intent)
                } catch (e: Exception) {
                    Toast.makeText(ctx, "Gagal membuka link: $link", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    @Keep
    @JvmStatic
    fun querySystemClipboard() {
        appContext?.let { ctx ->
            mainHandler.post {
                try {
                    val clipboard = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                    val clipData = clipboard?.primaryClip
                    val clipText = clipData?.getItemAt(0)?.text?.toString()
                    ZLBridge.clipboardReceived(clipText, "plain")
                } catch (e: Exception) {
                    ZLBridge.clipboardReceived(null, null)
                }
            }
        } ?: run {
            ZLBridge.clipboardReceived(null, null)
        }
    }

    @Keep
    @JvmStatic
    fun putClipboardData(data: String, mimeType: String) {
        appContext?.let { ctx ->
            mainHandler.post {
                try {
                    val clipboard = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                    val clip = when (mimeType) {
                        "text/html" -> ClipData.newHtmlText("NuxLauncher", data, data)
                        else -> ClipData.newPlainText("NuxLauncher", data)
                    }
                    clipboard?.setPrimaryClip(clip)
                    Toast.makeText(ctx, "Teks error berhasil disalin ke papan klip", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }
}
