package com.israadev.nuxlauncher.core.crash

import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.util.concurrent.TimeUnit

object MCLogsUploader {
    private const val API_URL = "https://api.mclo.gs/1/log"
    private const val MAX_LOG_SIZE_BYTES = 2 * 1024 * 1024 // 2 MB limit for mclo.gs

    private val client by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .build()
    }

    private val gson = Gson()

    private data class MCLogsResponse(
        val success: Boolean = false,
        val url: String? = null,
        val id: String? = null,
        val error: String? = null
    )

    /**
     * Memeriksa apakah file log memenuhi kriteria ukuran untuk diunggah (< 2MB)
     */
    fun canUpload(file: File?): Boolean {
        if (file == null || !file.exists() || !file.isFile) return false
        return file.length() in 1..MAX_LOG_SIZE_BYTES
    }

    /**
     * Mengunggah konten log ke layanan publik mclo.gs
     * Mengembalikan URL publik atau error
     */
    suspend fun uploadLog(content: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            if (content.isBlank()) {
                return@withContext Result.failure(IllegalArgumentException("Konten log kosong."))
            }

            // Jika konten terlalu panjang, ambil potongan terpenting (2MB teratas / terbaru)
            val trimmedContent = if (content.toByteArray(Charsets.UTF_8).size > MAX_LOG_SIZE_BYTES) {
                content.takeLast(100_000)
            } else {
                content
            }

            val formBody = FormBody.Builder()
                .add("content", trimmedContent)
                .build()

            val request = Request.Builder()
                .url(API_URL)
                .post(formBody)
                .header("User-Agent", "NUX-Launcher-Android/1.0")
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body.string()

            if (!response.isSuccessful) {
                return@withContext Result.failure(
                    RuntimeException("Gagal mengunggah log (HTTP ${response.code}): $responseBody")
                )
            }

            val jsonResponse = gson.fromJson(responseBody, MCLogsResponse::class.java)
            if (jsonResponse != null && jsonResponse.success && !jsonResponse.url.isNullOrBlank()) {
                Result.success(jsonResponse.url.replace("\\/", "/"))
            } else {
                val err = jsonResponse?.error ?: "Server mclo.gs tidak mengembalikan URL."
                Result.failure(RuntimeException(err))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Mengunggah file log ke mclo.gs
     */
    suspend fun uploadLogFile(file: File): Result<String> {
        return try {
            if (!file.exists()) {
                return Result.failure(java.io.FileNotFoundException("File log tidak ditemukan: ${file.absolutePath}"))
            }
            val content = file.readText(Charsets.UTF_8)
            uploadLog(content)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
