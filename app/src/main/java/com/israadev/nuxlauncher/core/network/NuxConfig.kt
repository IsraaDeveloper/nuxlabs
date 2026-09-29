package com.israadev.nuxlauncher.core.network

import com.israadev.nuxlauncher.BuildConfig

/**
 * Konfigurasi Endpoint Server Nux Launcher.
 *
 * Nilai dasar diambil dari BuildConfig.SERVER_BASE_URL yang diinjeksi saat build
 * dari file `local.properties` (properti: `nux.server.url`).
 *
 * Jika proyek di-clone tanpa local.properties, nilai SERVER_BASE_URL akan kosong (""),
 * sehingga endpoint private produksi tidak bocor ke publik atau disalahgunakan di fork publik.
 */
object NuxConfig {
    val SERVER_BASE_URL: String = BuildConfig.SERVER_BASE_URL.trim().trimEnd('/')

    val isConfigured: Boolean
        get() = SERVER_BASE_URL.isNotBlank()

    val CANDIDATE_BASES: List<String>
        get() = if (isConfigured) listOf(SERVER_BASE_URL) else emptyList()

    val UPLOAD_IMAGE_URL: String
        get() = if (isConfigured) "$SERVER_BASE_URL/upload/image" else ""

    val LIVEKIT_TOKEN_URL: String
        get() = if (isConfigured) "$SERVER_BASE_URL/livekit/token" else ""

    val UPDATE_ENDPOINT: String
        get() = if (isConfigured) "$SERVER_BASE_URL/getAndroidVersion" else ""

    val BUY_KEY_URL: String
        get() = if (isConfigured) "$SERVER_BASE_URL/android-key" else ""

    /**
     * Menghasilkan URL download lengkap.
     * Jika URL sudah absolut (http/https), dikembalikan langsung.
     * Jika relatif, digabungkan dengan SERVER_BASE_URL.
     */
    fun getDownloadUrl(relativeOrAbsolute: String): String {
        if (relativeOrAbsolute.startsWith("http://", ignoreCase = true) ||
            relativeOrAbsolute.startsWith("https://", ignoreCase = true)
        ) {
            return relativeOrAbsolute
        }
        val cleanPath = relativeOrAbsolute.removePrefix("/")
        return if (isConfigured) "$SERVER_BASE_URL/$cleanPath" else ""
    }
}
