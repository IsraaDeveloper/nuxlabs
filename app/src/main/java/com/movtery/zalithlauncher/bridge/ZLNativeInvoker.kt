package com.movtery.zalithlauncher.bridge

import androidx.annotation.Keep

@Keep
object ZLNativeInvoker {
    @JvmStatic
    var onExitCallback: ((exitCode: Int, isSignal: Boolean) -> Unit)? = null

    @Keep
    @JvmStatic
    fun jvmExit(exitCode: Int, isSignal: Boolean) {
        onExitCallback?.invoke(exitCode, isSignal)
    }

    @Keep
    @JvmStatic
    fun openLink(link: String) {
        // Link opening handler
    }

    @Keep
    @JvmStatic
    fun querySystemClipboard() {
        ZLBridge.clipboardReceived(null, null)
    }

    @Keep
    @JvmStatic
    fun putClipboardData(data: String, mimeType: String) {
        // Optional clipboard copy handler
    }
}
