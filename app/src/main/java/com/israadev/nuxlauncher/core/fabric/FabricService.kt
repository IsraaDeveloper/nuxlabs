package com.israadev.nuxlauncher.core.fabric

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.israadev.nuxlauncher.core.models.FabricLibrary
import com.israadev.nuxlauncher.core.models.FabricLoaderItem
import com.israadev.nuxlauncher.core.models.FabricProfile
import com.israadev.nuxlauncher.core.network.NuxDns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.concurrent.TimeUnit

object FabricService {
    private const val META_URL = "https://meta.fabricmc.net/v2/versions/loader"
    private const val MIRROR_META_URL = "https://bmclapi2.bangbang93.com/fabric-meta/v2/versions/loader"

    private val gson = Gson()
    val client = OkHttpClient.Builder()
        .dispatcher(okhttp3.Dispatcher().apply {
            maxRequests = 64
            maxRequestsPerHost = 32
        })
        .connectionPool(okhttp3.ConnectionPool(32, 5, TimeUnit.MINUTES))
        .dns(NuxDns)
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .followRedirects(true)
        .retryOnConnectionFailure(true)
        .build()

    /**
     * Mengambil daftar versi Fabric Loader untuk versi game Minecraft tertentu
     */
    suspend fun getFabricLoaders(gameVersion: String): Result<List<String>> = withContext(Dispatchers.IO) {
        val urls = listOf(
            "$META_URL/$gameVersion",
            "$MIRROR_META_URL/$gameVersion"
        )

        var lastException: Exception? = null
        for (url in urls) {
            try {
                val request = Request.Builder().url(url).build()
                val response = client.newCall(request).execute()
                if (!response.isSuccessful) {
                    lastException = IOException("HTTP Error ${response.code} from $url")
                    continue
                }

                val json = response.body?.string() ?: continue
                val type = object : TypeToken<List<FabricLoaderItem>>() {}.type
                val items: List<FabricLoaderItem> = gson.fromJson(json, type)
                val versions = items.map { it.loader.version }
                if (versions.isNotEmpty()) {
                    return@withContext Result.success(versions)
                }
            } catch (e: Exception) {
                lastException = e
            }
        }
        Result.failure(lastException ?: IOException("Tidak dapat memuat versi Fabric untuk $gameVersion"))
    }

    /**
     * Mengambil profil lengkap Fabric Loader (mainClass, library list, dll.)
     */
    suspend fun getFabricProfile(gameVersion: String, loaderVersion: String): Result<FabricProfile> = withContext(Dispatchers.IO) {
        val safeVersion = gameVersion.replace("∞", "infinite")
        val urls = listOf(
            "$META_URL/$safeVersion/$loaderVersion/profile/json",
            "$MIRROR_META_URL/$safeVersion/$loaderVersion/profile/json"
        )

        var lastException: Exception? = null
        for (url in urls) {
            try {
                val request = Request.Builder().url(url).build()
                val response = client.newCall(request).execute()
                if (!response.isSuccessful) {
                    lastException = IOException("HTTP Error ${response.code} from $url")
                    continue
                }

                val json = response.body?.string() ?: continue
                val profile: FabricProfile = gson.fromJson(json, FabricProfile::class.java)
                return@withContext Result.success(profile)
            } catch (e: Exception) {
                lastException = e
            }
        }
        Result.failure(lastException ?: IOException("Gagal memuat profil Fabric untuk $gameVersion ($loaderVersion)"))
    }

    /**
     * Mengonversi koordinat Maven 'group:artifact:version[:classifier]' menjadi relative file path
     */
    fun artifactToPath(name: String): String? {
        val parts = name.split(":")
        if (parts.size < 3) return null
        val group = parts[0].replace('.', '/')
        val artifact = parts[1]
        val version = parts[2]
        val classifier = if (parts.size > 3) "-${parts[3]}" else ""
        return "$group/$artifact/$version/$artifact-$version$classifier.jar"
    }

    /**
     * Mengembalikan kandidat URL untuk mengunduh library Fabric (resmi & mirror)
     */
    fun getLibraryCandidateUrls(library: FabricLibrary): List<String> {
        val path = artifactToPath(library.name) ?: return emptyList()
        val urls = mutableListOf<String>()

        val rawBase = library.url?.takeIf { it.isNotBlank() } ?: "https://maven.fabricmc.net/"
        val base = if (rawBase.endsWith("/")) rawBase else "$rawBase/"
        urls.add("$base$path")

        // Jika berbasis maven fabricmc, tambahkan mirror BMCLAPI
        if (base.contains("maven.fabricmc.net")) {
            urls.add("https://bmclapi2.bangbang93.com/maven/$path")
        }

        // Tambahkan Maven Central sebagai fallback
        urls.add("https://repo1.maven.org/maven2/$path")

        return urls
    }
}
