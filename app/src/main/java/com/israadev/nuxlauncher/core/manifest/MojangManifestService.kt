package com.israadev.nuxlauncher.core.manifest

import com.google.gson.Gson
import com.israadev.nuxlauncher.core.models.VersionDetail
import com.israadev.nuxlauncher.core.models.VersionManifest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.concurrent.TimeUnit

object MojangManifestService {
    private const val MANIFEST_URL = "https://piston-meta.mojang.com/mc/game/version_manifest_v2.json"
    private val gson = Gson()
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    private var cachedManifest: VersionManifest? = null

    suspend fun getVersionManifest(forceRefresh: Boolean = false): Result<VersionManifest> = withContext(Dispatchers.IO) {
        if (!forceRefresh && cachedManifest != null) {
            return@withContext Result.success(cachedManifest!!)
        }

        try {
            val request = Request.Builder().url(MANIFEST_URL).build()
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext Result.failure(IOException("HTTP Error ${response.code}"))
            }

            val json = response.body?.string() ?: return@withContext Result.failure(IOException("Empty response body"))
            val manifest = gson.fromJson(json, VersionManifest::class.java)
            cachedManifest = manifest
            Result.success(manifest)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getVersionDetail(url: String): Result<VersionDetail> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder().url(url).build()
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext Result.failure(IOException("HTTP Error ${response.code}"))
            }

            val json = response.body?.string() ?: return@withContext Result.failure(IOException("Empty response body"))
            val detail = gson.fromJson(json, VersionDetail::class.java)
            Result.success(detail)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
