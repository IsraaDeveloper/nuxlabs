package com.israadev.nuxlauncher.core.update

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.content.pm.PackageInfoCompat
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import com.israadev.nuxlauncher.core.network.NuxConfig
import java.util.concurrent.TimeUnit

data class AndroidUpdateInfo(
    val code: Int,
    val version: String,
    val localVersion: String,
    val localCode: Long,
    val createdAt: String,
    val downloadUrl: String,
    val fileName: String,
    val fileSize: Long,
    val changelog: String,
    val isUpdateAvailable: Boolean
)

object UpdateManager {
    private val UPDATE_ENDPOINT: String
        get() = NuxConfig.UPDATE_ENDPOINT

    private val client = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    suspend fun checkForUpdate(context: Context): Result<AndroidUpdateInfo> = withContext(Dispatchers.IO) {
        if (UPDATE_ENDPOINT.isBlank()) {
            return@withContext Result.failure(Exception("Server pembaruan belum dikonfigurasi di local.properties."))
        }
        try {
            val reqBuilder = Request.Builder()
                .url(UPDATE_ENDPOINT)
                .header("User-Agent", "NuxLauncher-Android/1.0.4")
                .get()
            val req = reqBuilder.build()

            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) {
                    return@withContext Result.failure(Exception("Server HTTP ${resp.code}"))
                }
                val bodyStr = resp.body.string()

                val rootObj = JsonParser.parseString(bodyStr).asJsonObject

                val remoteCode = if (rootObj.has("code") && !rootObj.get("code").isJsonNull) {
                    rootObj.get("code").asInt
                } else 1

                val remoteVersion = if (rootObj.has("version") && !rootObj.get("version").isJsonNull) {
                    rootObj.get("version").asString.trim().removePrefix("v")
                } else "1.0.0"

                val createdAt = if (rootObj.has("created_at") && !rootObj.get("created_at").isJsonNull) {
                    rootObj.get("created_at").asString
                } else ""

                // Extract download files info
                var downloadUrl = NuxConfig.getDownloadUrl("uploads/installers/NuxLauncher.apk")
                var fileName = "NuxLauncher-Android.apk"
                var fileSize = 0L

                if (rootObj.has("files") && rootObj.get("files").isJsonArray) {
                    val filesArr = rootObj.getAsJsonArray("files")
                    if (filesArr.size() > 0) {
                        val firstFile = filesArr.get(0).asJsonObject
                        if (firstFile.has("uri") && !firstFile.get("uri").isJsonNull) {
                            downloadUrl = NuxConfig.getDownloadUrl(firstFile.get("uri").asString)
                        }
                        if (firstFile.has("file_name") && !firstFile.get("file_name").isJsonNull) {
                            fileName = firstFile.get("file_name").asString
                        }
                        if (firstFile.has("size") && !firstFile.get("size").isJsonNull) {
                            fileSize = firstFile.get("size").asLong
                        }
                    }
                }

                // Changelog extraction
                var changelogText = ""
                if (rootObj.has("default_body") && rootObj.get("default_body").isJsonObject) {
                    val defBody = rootObj.getAsJsonObject("default_body")
                    if (defBody.has("markdown") && !defBody.get("markdown").isJsonNull) {
                        changelogText = defBody.get("markdown").asString
                    }
                }

                if (changelogText.isBlank() && rootObj.has("bodies") && rootObj.get("bodies").isJsonArray) {
                    val bodies = rootObj.getAsJsonArray("bodies")
                    if (bodies.size() > 0) {
                        val b0 = bodies.get(0).asJsonObject
                        if (b0.has("markdown") && !b0.get("markdown").isJsonNull) {
                            changelogText = b0.get("markdown").asString
                        }
                    }
                }

                if (changelogText.isBlank()) {
                    changelogText = "• Peluncuran Resmi NUX Launcher Android\n• Peningkatan Performa & Sinkronisasi Game"
                }

                // Local package info
                val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    context.packageManager.getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(0))
                } else {
                    @Suppress("DEPRECATION")
                    context.packageManager.getPackageInfo(context.packageName, 0)
                }
                val localCode = PackageInfoCompat.getLongVersionCode(packageInfo)
                val localVersionName = (packageInfo.versionName ?: "1.0.4").trim().removePrefix("v")

                val isNewerCode = remoteCode > localCode
                val isNewerVer = compareVersions(remoteVersion, localVersionName) > 0
                val isAvailable = isNewerCode || isNewerVer

                val updateInfo = AndroidUpdateInfo(
                    code = remoteCode,
                    version = remoteVersion,
                    localVersion = localVersionName,
                    localCode = localCode,
                    createdAt = createdAt,
                    downloadUrl = downloadUrl,
                    fileName = fileName,
                    fileSize = fileSize,
                    changelog = changelogText,
                    isUpdateAvailable = isAvailable
                )

                Result.success(updateInfo)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun compareVersions(ver1: String, ver2: String): Int {
        val parts1 = ver1.split('.').mapNotNull { it.filter { c -> c.isDigit() }.toIntOrNull() }
        val parts2 = ver2.split('.').mapNotNull { it.filter { c -> c.isDigit() }.toIntOrNull() }
        val maxLen = maxOf(parts1.size, parts2.size)
        for (i in 0 until maxLen) {
            val p1 = parts1.getOrElse(i) { 0 }
            val p2 = parts2.getOrElse(i) { 0 }
            if (p1 != p2) return p1.compareTo(p2)
        }
        return 0
    }

    fun openDownloadUrl(context: Context, url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {
        }
    }
}
