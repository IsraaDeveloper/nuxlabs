package com.israadev.nuxlauncher.core.download

import android.content.Context
import com.google.gson.Gson
import com.israadev.nuxlauncher.core.instance.InstanceManager
import com.israadev.nuxlauncher.core.manifest.MojangManifestService
import com.israadev.nuxlauncher.core.models.AssetIndexContent
import com.israadev.nuxlauncher.core.models.Instance
import com.israadev.nuxlauncher.core.models.Library
import com.israadev.nuxlauncher.core.models.VersionDetail
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class MinecraftDownloader(
    private val context: Context,
    clientOverride: OkHttpClient? = null,
    private val gson: Gson = Gson()
) {
    private val client: OkHttpClient = clientOverride ?: OkHttpClient.Builder()
        .dispatcher(okhttp3.Dispatcher().apply {
            maxRequests = 128
            maxRequestsPerHost = 64
        })
        .connectionPool(okhttp3.ConnectionPool(64, 5, TimeUnit.MINUTES))
        .dns(com.israadev.nuxlauncher.core.network.NuxDns)
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .followRedirects(true)
        .retryOnConnectionFailure(true)
        .build()

    suspend fun downloadInstance(
        instance: Instance,
        onProgress: (Float, String) -> Unit
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            onProgress(0.05f, "Mengambil metadata versi ${instance.mcVersion}...")
            val manifestRes = MojangManifestService.getVersionManifest()
            val manifest = manifestRes.getOrNull() ?: return@withContext Result.failure(manifestRes.exceptionOrNull()!!)

            val versionItem = manifest.versions.find { it.id == instance.mcVersion }
                ?: return@withContext Result.failure(IllegalStateException("Versi ${instance.mcVersion} tidak ditemukan di manifest Mojang"))

            onProgress(0.10f, "Membaca konfigurasi versi...")
            val detailRes = MojangManifestService.getVersionDetail(versionItem.url)
            val detail = detailRes.getOrNull() ?: return@withContext Result.failure(detailRes.exceptionOrNull()!!)

            val versionDir = File(InstanceManager.getVersionsDir(context), instance.mcVersion)
            if (!versionDir.exists()) versionDir.mkdirs()

            // 1. Save version.json
            val versionJsonFile = File(versionDir, "${instance.mcVersion}.json")
            if (!versionJsonFile.exists()) {
                downloadFile(versionItem.url, versionJsonFile)
            }

            // 2. Download Client JAR
            val clientJar = File(versionDir, "${instance.mcVersion}.jar")
            val clientUrl = detail.downloads?.client?.url
            val expectedClientSize = detail.downloads?.client?.size ?: 0L
            val needsClientDownload = !clientJar.exists() || clientJar.length() == 0L || (expectedClientSize > 0L && clientJar.length() != expectedClientSize)
            if (clientUrl != null && needsClientDownload) {
                downloadFile(
                    url = clientUrl,
                    targetFile = clientJar,
                    expectedSize = expectedClientSize,
                    onProgress = { bytesRead, totalBytes ->
                        val total = if (totalBytes > 0) totalBytes else expectedClientSize
                        val mbRead = bytesRead / (1024 * 1024f)
                        val mbTotal = total / (1024 * 1024f)
                        val frac = if (total > 0) (bytesRead.toFloat() / total).coerceIn(0f, 1f) else 0f
                        val p = 0.20f + frac * 0.15f
                        val percent = (frac * 100).toInt()
                        onProgress(p, String.format(java.util.Locale.US, "Mengunduh client Minecraft (%.1f / %.1f MB - %d%%)...", mbRead, mbTotal, percent))
                    }
                )
            }

            // 3. Download Libraries (Parallel with 24 concurrent workers)
            val libraries = detail.libraries ?: emptyList()
            val allowedLibs = libraries.filter { isLibraryAllowed(it) }
            val libDir = InstanceManager.getLibrariesDir(context)
            if (!libDir.exists()) libDir.mkdirs()

            val totalLibs = allowedLibs.size
            if (totalLibs > 0) {
                val downloadedLibs = AtomicInteger(0)
                val semaphore = Semaphore(48) // Parallel 48 concurrent workers (high-speed mirror)

                val libJobs = allowedLibs.map { lib ->
                    async {
                        semaphore.withPermit {
                            val artifact = lib.downloads?.artifact
                            if (artifact?.url != null && artifact.path != null) {
                                val targetFile = File(libDir, artifact.path)
                                val expectedSize = artifact.size ?: 0L
                                val needsDownload = !targetFile.exists() || targetFile.length() == 0L || (expectedSize > 0L && targetFile.length() != expectedSize)
                                if (needsDownload) {
                                    targetFile.parentFile?.mkdirs()
                                    try {
                                        downloadFile(artifact.url, targetFile, expectedSize)
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                }
                            }
                            val count = downloadedLibs.incrementAndGet()
                            val p = 0.20f + (count.toFloat() / totalLibs) * 0.40f
                            onProgress(p, "Mengunduh libraries ($count/$totalLibs - 48 Threads)...")
                        }
                    }
                }
                libJobs.awaitAll()
            }

            // 4. Download Asset Index
            val assetIndex = detail.assetIndex
            if (assetIndex != null) {
                onProgress(0.65f, "Memverifikasi asset index ${assetIndex.id}...")
                val assetsDir = InstanceManager.getAssetsDir(context)
                val indexesDir = File(assetsDir, "indexes")
                if (!indexesDir.exists()) indexesDir.mkdirs()
                val indexFile = File(indexesDir, "${assetIndex.id}.json")
                if (!indexFile.exists() || indexFile.length() == 0L) {
                    downloadFile(assetIndex.url, indexFile)
                }

                // Download objects (Parallel with 32 concurrent workers)
                if (indexFile.exists()) {
                    try {
                        val indexContent = gson.fromJson(indexFile.readText(), AssetIndexContent::class.java)
                        val objects = indexContent.objects.entries.toList()
                        val totalObjects = objects.size
                        val objectsDir = File(assetsDir, "objects")

                        val downloadedAssets = AtomicInteger(0)
                        val semaphore = Semaphore(64) // Parallel 64 concurrent workers for game assets

                        val assetJobs = objects.map { entry ->
                            async {
                                semaphore.withPermit {
                                    val hash = entry.value.hash
                                    val prefix = hash.substring(0, 2)
                                    val targetObj = File(objectsDir, "$prefix/$hash")
                                    val expectedSize = entry.value.size
                                    if (!targetObj.exists() || targetObj.length() == 0L || (expectedSize > 0L && targetObj.length() != expectedSize)) {
                                        targetObj.parentFile?.mkdirs()
                                        val objUrl = "https://resources.download.minecraft.net/$prefix/$hash"
                                        try {
                                            downloadFile(objUrl, targetObj, expectedSize)
                                        } catch (_: Exception) {}
                                    }
                                    val count = downloadedAssets.incrementAndGet()
                                    if (count % 10 == 0 || count == totalObjects) {
                                        val p = 0.65f + (count.toFloat() / totalObjects) * 0.27f
                                        onProgress(p, "Mengunduh aset game ($count/$totalObjects - 64 Threads)...")
                                    }
                                }
                            }
                        }
                        assetJobs.awaitAll()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }

            // 5. Download Fabric Loader & Libraries if instance uses Fabric
            if (instance.loader.equals("fabric", ignoreCase = true)) {
                onProgress(0.92f, "Menyiapkan Fabric Loader...")
                var loaderVer = instance.loaderVersion
                if (loaderVer.isBlank()) {
                    val loaderListRes = com.israadev.nuxlauncher.core.fabric.FabricService.getFabricLoaders(instance.mcVersion)
                    val list = loaderListRes.getOrNull()
                    if (!list.isNullOrEmpty()) {
                        loaderVer = list.first()
                        instance.loaderVersion = loaderVer
                    } else {
                        throw IllegalStateException("Tidak dapat menemukan Fabric loader untuk versi ${instance.mcVersion}")
                    }
                }

                onProgress(0.94f, "Mengambil profil Fabric ($loaderVer)...")
                val profileRes = com.israadev.nuxlauncher.core.fabric.FabricService.getFabricProfile(instance.mcVersion, loaderVer)
                val profile = profileRes.getOrNull()
                    ?: throw IllegalStateException("Gagal memuat profil Fabric: ${profileRes.exceptionOrNull()?.message}")

                // Simpan file profil Fabric
                val fabricJsonFile = File(versionDir, "fabric-$loaderVer.json")
                fabricJsonFile.writeText(gson.toJson(profile))

                // Unduh seluruh library Fabric (Parallel with 16 concurrent workers)
                val fabricLibs = profile.libraries.filter { !it.name.contains("org.lwjgl") }
                val totalFabricLibs = fabricLibs.size
                if (totalFabricLibs > 0) {
                    val downloadedFabric = AtomicInteger(0)
                    val sem = Semaphore(32) // Parallel 32 concurrent workers for Fabric
                    val fabricJobs = fabricLibs.map { lib ->
                        async {
                            sem.withPermit {
                                val relPath = com.israadev.nuxlauncher.core.fabric.FabricService.artifactToPath(lib.name)
                                if (relPath != null) {
                                    val targetFile = File(libDir, relPath)
                                    val expectedSize = lib.size ?: 0L
                                    val needsLibDownload = !targetFile.exists() || targetFile.length() == 0L || (expectedSize > 0L && targetFile.length() != expectedSize)
                                    if (needsLibDownload) {
                                        targetFile.parentFile?.mkdirs()
                                        val candidateUrls = com.israadev.nuxlauncher.core.fabric.FabricService.getLibraryCandidateUrls(lib)
                                        var success = false
                                        for (u in candidateUrls) {
                                            try {
                                                downloadFile(u, targetFile, expectedSize)
                                                if (targetFile.exists() && targetFile.length() > 0L) {
                                                    success = true
                                                    break
                                                }
                                            } catch (_: Exception) {}
                                        }
                                        if (!success) {
                                            android.util.Log.w("MinecraftDownloader", "Gagal mengunduh library Fabric: ${lib.name}")
                                        }
                                    }
                                }
                                val count = downloadedFabric.incrementAndGet()
                                val p = 0.94f + (count.toFloat() / totalFabricLibs) * 0.05f
                                onProgress(p, "Mengunduh library Fabric ($count/$totalFabricLibs - 32 Threads)...")
                            }
                        }
                    }
                    fabricJobs.awaitAll()
                }
            }

            onProgress(1.0f, "Instalasi selesai!")
            instance.isDownloaded = true
            InstanceManager.updateInstance(context, instance)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun isLibraryAllowed(library: Library): Boolean {
        val rules = library.rules ?: return true
        var allowed = false
        for (rule in rules) {
            if (rule.action == "allow") {
                if (rule.os == null || rule.os.name == "linux") {
                    allowed = true
                }
            } else if (rule.action == "disallow") {
                if (rule.os != null && (rule.os.name == "osx" || rule.os.name == "windows")) {
                    // Do nothing
                } else {
                    allowed = false
                }
            }
        }
        return allowed
    }

    private fun getMirrorUrls(url: String): List<String> {
        val urls = mutableListOf<String>()
        if (url.startsWith("https://piston-data.mojang.com")) {
            urls.add(url.replace("https://piston-data.mojang.com", "https://bmclapi2.bangbang93.com"))
        } else if (url.startsWith("https://piston-meta.mojang.com")) {
            urls.add(url.replace("https://piston-meta.mojang.com", "https://bmclapi2.bangbang93.com"))
        } else if (url.startsWith("https://libraries.minecraft.net")) {
            urls.add(url.replace("https://libraries.minecraft.net", "https://bmclapi2.bangbang93.com/libraries"))
        } else if (url.startsWith("https://resources.download.minecraft.net")) {
            urls.add(url.replace("https://resources.download.minecraft.net", "https://bmclapi2.bangbang93.com/assets"))
        } else if (url.startsWith("https://meta.fabricmc.net")) {
            urls.add(url.replace("https://meta.fabricmc.net", "https://bmclapi2.bangbang93.com/fabric-meta"))
        } else if (url.startsWith("https://maven.fabricmc.net")) {
            urls.add(url.replace("https://maven.fabricmc.net", "https://bmclapi2.bangbang93.com/maven"))
        }
        urls.add(url)
        return urls
    }

    /**
     * Mengunduh file secara cepat menggunakan buffer 64KB dan retry otomatis jika terjadi gangguan jaringan.
     * Menggunakan mirror BMCLAPI berkecepatan tinggi dengan fallback otomatis ke server resmi Mojang.
     */
    private fun downloadFile(
        url: String,
        targetFile: File,
        expectedSize: Long = 0L,
        onProgress: ((bytesRead: Long, totalBytes: Long) -> Unit)? = null,
        retries: Int = 3
    ) {
        val candidateUrls = getMirrorUrls(url)
        var lastError: Exception? = null
        val isLargeFile = expectedSize > 5 * 1024 * 1024L
        val tempFile = if (isLargeFile) File(targetFile.parentFile, "${targetFile.name}.part_${System.nanoTime()}") else targetFile

        for (candidateUrl in candidateUrls) {
            for (attempt in 1..retries) {
                try {
                    val request = Request.Builder()
                        .url(candidateUrl)
                        .header("User-Agent", "NuxLauncher/1.0 (Android)")
                        .build()
                    val response = client.newCall(request).execute()
                    if (!response.isSuccessful) {
                        response.close()
                        throw IOException("Failed to download $candidateUrl: ${response.code}")
                    }

                    val body = response.body ?: throw IOException("Empty body for $candidateUrl")
                    val totalLength = if (body.contentLength() > 0) body.contentLength() else expectedSize
                    targetFile.parentFile?.mkdirs()

                    var bytesCopied = 0L
                    var lastReportTime = 0L
                    FileOutputStream(tempFile).use { output ->
                        body.byteStream().use { input ->
                            val buffer = ByteArray(64 * 1024)
                            var read: Int
                            while (input.read(buffer).also { read = it } != -1) {
                                output.write(buffer, 0, read)
                                bytesCopied += read
                                if (onProgress != null) {
                                    val now = System.currentTimeMillis()
                                    if (now - lastReportTime >= 100L) {
                                        lastReportTime = now
                                        onProgress.invoke(bytesCopied, totalLength)
                                    }
                                }
                            }
                            output.flush()
                        }
                    }
                    onProgress?.invoke(bytesCopied, totalLength)

                    if (isLargeFile) {
                        if (tempFile.exists() && tempFile.length() > 0L) {
                            if (targetFile.exists()) targetFile.delete()
                            if (tempFile.renameTo(targetFile)) {
                                return
                            } else {
                                tempFile.copyTo(targetFile, overwrite = true)
                                tempFile.delete()
                                return
                            }
                        }
                    } else {
                        if (targetFile.exists() && targetFile.length() > 0L) {
                            return
                        }
                    }
                } catch (e: Exception) {
                    lastError = e
                    if (isLargeFile) {
                        try { tempFile.delete() } catch (_: Exception) {}
                    }
                    if (attempt < retries) {
                        Thread.sleep(100L * attempt)
                    }
                }
            }
        }
        throw lastError ?: IOException("Failed to download $url")
    }
}
