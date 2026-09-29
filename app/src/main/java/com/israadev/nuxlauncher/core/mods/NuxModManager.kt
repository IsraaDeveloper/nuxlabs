package com.israadev.nuxlauncher.core.mods

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.google.gson.Gson
import com.google.gson.JsonParser
import com.google.gson.reflect.TypeToken
import com.israadev.nuxlauncher.core.instance.InstanceManager
import com.israadev.nuxlauncher.core.models.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.net.URLEncoder
import java.util.concurrent.TimeUnit
import java.util.zip.ZipFile

data class WorldInfo(
    val folderName: String,
    val displayName: String,
    val lastPlayed: Long = 0L,
    val iconFile: File? = null
)

object NuxModManager {

    private val gson = Gson()
    private val httpClient = OkHttpClient.Builder()
        .dns(com.israadev.nuxlauncher.core.network.NuxDns)
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    private const val USER_AGENT = "NUX-Launcher/1.4.0 (contact@nuxlauncher.site)"
    private const val MODRINTH_API_BASE = "https://api.modrinth.com/v2"

    /**
     * Membaca daftar dunia (saves) yang ada di dalam instance
     */
    fun getInstanceWorlds(context: Context, instance: Instance): List<WorldInfo> {
        val gameDir = InstanceManager.getInstanceGameDir(context, instance)
        val savesDir = File(gameDir, "saves")
        if (!savesDir.exists() || !savesDir.isDirectory) return emptyList()

        val dirs = savesDir.listFiles { file -> file.isDirectory } ?: return emptyList()
        return dirs.map { worldFolder ->
            val iconFile = File(worldFolder, "icon.png").takeIf { it.exists() }
            val levelDat = File(worldFolder, "level.dat")
            val lastPlayed = if (levelDat.exists()) levelDat.lastModified() else worldFolder.lastModified()
            WorldInfo(
                folderName = worldFolder.name,
                displayName = worldFolder.name,
                lastPlayed = lastPlayed,
                iconFile = iconFile
            )
        }.sortedByDescending { it.lastPlayed }
    }

    /**
     * Menghasilkan folder target sesuai jenis addon dan instance Minecraft
     */
    fun getTargetDir(context: Context, instance: Instance, itemType: String, worldName: String? = null): File {
        val gameDir = InstanceManager.getInstanceGameDir(context, instance)
        val dir = when (itemType) {
            "datapacks" -> {
                if (!worldName.isNullOrBlank()) {
                    File(gameDir, "saves/$worldName/datapacks")
                } else {
                    File(gameDir, "datapacks")
                }
            }
            "resourcepacks" -> File(gameDir, "resourcepacks")
            "shaderpacks" -> File(gameDir, "shaderpacks")
            "modpacks" -> File(gameDir, "modpacks")
            else -> File(gameDir, "mods")
        }
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    /**
     * Membaca semua item yang telah terpasang di dalam instance untuk jenis addon tertentu
     */
    suspend fun getInstalledItems(
        context: Context,
        instance: Instance,
        itemType: String,
        worldName: String? = null
    ): List<InstalledModItem> = withContext(Dispatchers.IO) {
        val targetDir = getTargetDir(context, instance, itemType, worldName)
        val gameDir = InstanceManager.getInstanceGameDir(context, instance)
        if (!targetDir.exists() || !targetDir.isDirectory) return@withContext emptyList()

        val files = targetDir.listFiles() ?: return@withContext emptyList()
        val result = mutableListOf<InstalledModItem>()

        for (file in files) {
            if (file.isDirectory && itemType != "datapacks") continue

            val originalFilename = file.name
            val isEnabled = !originalFilename.endsWith(".disabled")
            val cleanFilename = if (!isEnabled) originalFilename.removeSuffix(".disabled") else originalFilename
            val fileStem = File(cleanFilename).nameWithoutExtension

            var title = fileStem
            var version = "Lokal"
            var description = cleanFilename

            // Coba ekstrak metadata dari file JAR mod jika bertipe mods
            if (itemType == "mods" && cleanFilename.endsWith(".jar", ignoreCase = true) && file.isFile) {
                try {
                    ZipFile(file).use { zip ->
                        // 1. Fabric mod
                        val fabricEntry = zip.getEntry("fabric.mod.json")
                        if (fabricEntry != null) {
                            zip.getInputStream(fabricEntry).bufferedReader().use { reader ->
                                val json = JsonParser.parseReader(reader).asJsonObject
                                if (json.has("name")) title = json.get("name").asString
                                if (json.has("version")) version = json.get("version").asString
                                if (json.has("description")) description = json.get("description").asString
                            }
                        } else {
                            // 2. Forge mods.toml
                            val tomlEntry = zip.getEntry("META-INF/mods.toml")
                            if (tomlEntry != null) {
                                zip.getInputStream(tomlEntry).bufferedReader().use { reader ->
                                    val lines = reader.readLines()
                                    var inModBlock = false
                                    for (line in lines) {
                                        val trimmed = line.trim()
                                        if (trimmed.startsWith("[[mods]]")) {
                                            inModBlock = true
                                        } else if (inModBlock) {
                                            if (trimmed.startsWith("displayName")) {
                                                title = trimmed.substringAfter("=").trim().trim('"', '\'')
                                            } else if (trimmed.startsWith("version")) {
                                                version = trimmed.substringAfter("=").trim().trim('"', '\'')
                                            } else if (trimmed.startsWith("description")) {
                                                description = trimmed.substringAfter("=").trim().trim('"', '\'')
                                                break
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                } catch (_: Exception) {
                    // Abaikan jika JAR tidak dapat diekstrak, fallback ke nama file
                }
            } else if (itemType == "modpacks" && (cleanFilename.endsWith(".mrpack", ignoreCase = true) || cleanFilename.endsWith(".zip", ignoreCase = true)) && file.isFile) {
                val manifest = loadModpackManifest(gameDir, cleanFilename)
                if (manifest != null) {
                    if (manifest.modpackName.isNotBlank()) title = manifest.modpackName
                    if (manifest.version.isNotBlank()) version = manifest.version
                    val modCount = manifest.installedFiles.count { it.startsWith("mods/") || it.endsWith(".jar", ignoreCase = true) }
                    description = if (modCount > 0) "$modCount mod terpasang" else cleanFilename
                } else {
                    try {
                        ZipFile(file).use { zip ->
                            val indexEntry = zip.getEntry("modrinth.index.json")
                                ?: zip.entries().asSequence().firstOrNull { it.name.endsWith("modrinth.index.json") }
                            if (indexEntry != null) {
                                zip.getInputStream(indexEntry).bufferedReader().use { reader ->
                                    val json = JsonParser.parseReader(reader).asJsonObject
                                    if (json.has("name")) title = json.get("name").asString
                                    if (json.has("versionId")) version = json.get("versionId").asString
                                    if (json.has("summary")) description = json.get("summary").asString
                                }
                            }
                        }
                    } catch (_: Exception) {
                    }
                }
            }

            result.add(
                InstalledModItem(
                    id = cleanFilename,
                    filename = cleanFilename,
                    originalFilename = originalFilename,
                    name = title,
                    description = description,
                    version = version,
                    category = itemType.replaceFirstChar { it.uppercase() },
                    isEnabled = isEnabled,
                    sizeBytes = if (file.isFile) file.length() else 0L,
                    itemType = itemType
                )
            )
        }

        result.sortedBy { it.name.lowercase() }
    }

    /**
     * Mengubah status aktif/nonaktif mod dengan menambah atau melepas ekstensi .disabled
     */
    suspend fun toggleItemEnabled(
        context: Context,
        instance: Instance,
        itemType: String,
        item: InstalledModItem,
        worldName: String? = null
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val targetDir = getTargetDir(context, instance, itemType, worldName)
            val currentFile = File(targetDir, item.originalFilename)
            val gameDir = InstanceManager.getInstanceGameDir(context, instance)

            if (itemType == "modpacks") {
                val cleanFilename = if (item.originalFilename.endsWith(".disabled")) {
                    item.originalFilename.removeSuffix(".disabled")
                } else {
                    item.originalFilename
                }

                // Ambil daftar file modpack dari manifest tracking atau arsip
                val manifest = loadModpackManifest(gameDir, cleanFilename)
                val fileList = manifest?.installedFiles ?: getModpackInstalledFilesFromArchive(currentFile)

                if (item.isEnabled) {
                    // Dinonaktifkan: rename semua mod jar milik modpack menjadi .disabled
                    for (relPath in fileList) {
                        if (relPath.startsWith("mods/") || relPath.endsWith(".jar", ignoreCase = true)) {
                            val activeFile = File(gameDir, relPath)
                            if (activeFile.exists()) {
                                val disabledFile = File(gameDir, "$relPath.disabled")
                                activeFile.renameTo(disabledFile)
                            }
                        }
                    }
                    val newFile = File(targetDir, "$cleanFilename.disabled")
                    if (currentFile.exists() && currentFile.name != newFile.name) {
                        currentFile.renameTo(newFile)
                    }
                } else {
                    // Diaktifkan: rename semua mod jar .disabled milik modpack kembali ke normal
                    for (relPath in fileList) {
                        if (relPath.startsWith("mods/") || relPath.endsWith(".jar", ignoreCase = true)) {
                            val disabledFile = File(gameDir, "$relPath.disabled")
                            if (disabledFile.exists()) {
                                val activeFile = File(gameDir, relPath)
                                disabledFile.renameTo(activeFile)
                            }
                        }
                    }
                    val newFile = File(targetDir, cleanFilename)
                    if (currentFile.exists() && currentFile.name != newFile.name) {
                        currentFile.renameTo(newFile)
                    }
                }
                return@withContext true
            }

            if (!currentFile.exists()) return@withContext false

            val newFilename = if (item.isEnabled) {
                "${item.filename}.disabled"
            } else {
                item.filename
            }

            val newFile = File(targetDir, newFilename)
            currentFile.renameTo(newFile)
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Menghapus file mod / addon dari instance
     */
    suspend fun deleteItem(
        context: Context,
        instance: Instance,
        itemType: String,
        item: InstalledModItem,
        worldName: String? = null
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val targetDir = getTargetDir(context, instance, itemType, worldName)
            val file = File(targetDir, item.originalFilename)
            val gameDir = InstanceManager.getInstanceGameDir(context, instance)

            if (itemType == "modpacks") {
                val cleanFilename = if (item.originalFilename.endsWith(".disabled")) {
                    item.originalFilename.removeSuffix(".disabled")
                } else {
                    item.originalFilename
                }

                // 1. Baca manifest modpack (atau fallback dari arsip zip jika metadata belum ada)
                val manifest = loadModpackManifest(gameDir, cleanFilename)
                val fileList = (manifest?.installedFiles ?: getModpackInstalledFilesFromArchive(file)).toMutableSet()

                // 2. Hapus HANYA file-file yang terpasang oleh modpack ini (baik aktif maupun disabled)
                for (relPath in fileList) {
                    val activeFile = File(gameDir, relPath)
                    if (activeFile.exists()) {
                        if (activeFile.isDirectory) activeFile.deleteRecursively() else activeFile.delete()
                    }
                    val disabledFile = File(gameDir, "$relPath.disabled")
                    if (disabledFile.exists()) {
                        if (disabledFile.isDirectory) disabledFile.deleteRecursively() else disabledFile.delete()
                    }
                }

                // 3. Hapus file metadata manifest
                val metaFile = getModpackMetadataFile(gameDir, cleanFilename)
                if (metaFile.exists()) {
                    metaFile.delete()
                }

                // 4. Hapus file modpack itu sendiri (baik .mrpack maupun .mrpack.disabled)
                if (file.exists()) {
                    if (file.isDirectory) file.deleteRecursively() else file.delete()
                }
                val cleanArchive = File(targetDir, cleanFilename)
                if (cleanArchive.exists()) cleanArchive.delete()
                val disabledArchive = File(targetDir, "$cleanFilename.disabled")
                if (disabledArchive.exists()) disabledArchive.delete()

                true
            } else {
                if (file.exists()) {
                    if (file.isDirectory) file.deleteRecursively() else file.delete()
                } else {
                    false
                }
            }
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Mengimpor file lokal dari HP pengguna ke dalam folder instance
     */
    suspend fun importLocalFile(
        context: Context,
        instance: Instance,
        itemType: String,
        uri: Uri,
        worldName: String? = null,
        onProgress: ((statusText: String, current: Int, total: Int) -> Unit)? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            var fileName = "imported_addon"
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1 && cursor.moveToFirst()) {
                    fileName = cursor.getString(nameIndex)
                }
            }

            // Validasi format ekstensi file sesuai tipe
            val lowerName = fileName.lowercase()
            when (itemType) {
                "mods" -> {
                    if (!lowerName.endsWith(".jar")) {
                        return@withContext Result.failure(IllegalArgumentException("File mod harus berformat .jar"))
                    }
                }
                "modpacks" -> {
                    if (!lowerName.endsWith(".mrpack") && !lowerName.endsWith(".zip")) {
                        return@withContext Result.failure(IllegalArgumentException("File modpack harus berformat .mrpack atau .zip"))
                    }
                }
                "shaderpacks", "resourcepacks", "datapacks" -> {
                    if (!lowerName.endsWith(".zip")) {
                        return@withContext Result.failure(IllegalArgumentException("File $itemType harus berformat .zip"))
                    }
                }
            }

            val targetDir = getTargetDir(context, instance, itemType, worldName)
            val destFile = File(targetDir, fileName)

            onProgress?.invoke("Menyalin file $fileName...", 0, 0)
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            } ?: return@withContext Result.failure(Exception("Gagal membaca file sumber"))

            // Jika modpack (.mrpack atau .zip), baca daftar mod dan unduh semua file seperti versi Windows!
            if (itemType == "modpacks" && (lowerName.endsWith(".mrpack") || lowerName.endsWith(".zip"))) {
                onProgress?.invoke("Membaca daftar modpack...", 0, 0)
                processMrpackFile(instance, destFile, context) { statusText, cur, tot ->
                    onProgress?.invoke(statusText, cur, tot)
                }
            }

            Result.success(fileName)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Pencarian Modrinth dengan Auto-Version dan Auto-Loader filter
     */
    suspend fun searchProjects(
        query: String,
        itemType: String,
        instance: Instance?,
        category: String? = null,
        filterByGameVersion: Boolean = true,
        page: Int = 1,
        limit: Int = 18
    ): Result<ModrinthSearchResponse> = withContext(Dispatchers.IO) {
        try {
            val facets = mutableListOf<List<String>>()

            // 1. Project type facet
            val projectType = when (itemType) {
                "resourcepacks" -> "resourcepack"
                "shaderpacks" -> "shader"
                "datapacks" -> "datapack"
                "modpacks" -> "modpack"
                else -> "mod"
            }
            facets.add(listOf("project_type:$projectType"))

            // 2. Category facet (jika dipilih user)
            if (!category.isNullOrBlank()) {
                facets.add(listOf("categories:$category"))
            }

            // 3. Game Version filter (Auto-Version dari instance)
            if (filterByGameVersion && instance != null && instance.mcVersion.isNotBlank()) {
                facets.add(listOf("versions:${instance.mcVersion}"))
            }

            // 4. Loader filter (Auto-Loader dari instance)
            if ((itemType == "mods" || itemType == "modpacks") && instance != null && instance.loader.isNotBlank()) {
                val loaderLower = instance.loader.lowercase()
                if (loaderLower != "vanilla") {
                    facets.add(listOf("categories:$loaderLower"))
                }
            }

            val facetsJson = gson.toJson(facets)
            val offset = (page - 1) * limit

            val url = StringBuilder("$MODRINTH_API_BASE/search?")
                .append("query=").append(URLEncoder.encode(query, "UTF-8"))
                .append("&limit=").append(limit)
                .append("&offset=").append(offset)
                .append("&facets=").append(URLEncoder.encode(facetsJson, "UTF-8"))
                .toString()

            val request = Request.Builder()
                .url(url)
                .header("User-Agent", USER_AGENT)
                .build()

            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("Modrinth API error code: ${response.code}"))
            }

            val body = response.body?.string() ?: return@withContext Result.failure(Exception("Empty response"))
            val searchRes = gson.fromJson(body, ModrinthSearchResponse::class.java)
            Result.success(searchRes)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Mengambil detail lengkap suatu project dari Modrinth
     */
    suspend fun getProjectDetails(projectId: String): Result<ModrinthProject> = withContext(Dispatchers.IO) {
        try {
            val url = "$MODRINTH_API_BASE/project/$projectId"
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", USER_AGENT)
                .build()

            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("Gagal mengambil detail project: ${response.code}"))
            }

            val body = response.body?.string() ?: return@withContext Result.failure(Exception("Empty response"))
            val project = gson.fromJson(body, ModrinthProject::class.java)
            Result.success(project)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Mengambil seluruh daftar rilis versi untuk project tertentu
     */
    suspend fun getProjectVersions(projectId: String): Result<List<ModrinthVersion>> = withContext(Dispatchers.IO) {
        try {
            val url = "$MODRINTH_API_BASE/project/$projectId/version"
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", USER_AGENT)
                .build()

            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("Gagal mengambil versi project: ${response.code}"))
            }

            val body = response.body?.string() ?: return@withContext Result.failure(Exception("Empty response"))
            val listType = object : TypeToken<List<ModrinthVersion>>() {}.type
            val versions: List<ModrinthVersion> = gson.fromJson(body, listType)
            Result.success(versions)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Mengambil dan menyaring secara ketat versi yang kompatibel dengan instance aktif (Auto-Version)
     */
    suspend fun getCompatibleVersions(
        projectId: String,
        instance: Instance?,
        itemType: String,
        filterByGameVersion: Boolean = true
    ): Result<List<ModrinthVersion>> = withContext(Dispatchers.IO) {
        val allVersResult = getProjectVersions(projectId)
        if (allVersResult.isFailure) return@withContext allVersResult

        val allVersions = allVersResult.getOrNull() ?: emptyList()
        if (instance == null) return@withContext Result.success(allVersions)

        val compatible = allVersions.filter { v ->
            // Cek kompatibilitas versi Minecraft
            val matchesVersion = !filterByGameVersion || instance.mcVersion.isBlank() || v.gameVersions.contains(instance.mcVersion)

            // Cek kompatibilitas Loader jika tipe mods atau modpacks
            val matchesLoader = if (itemType == "mods" || itemType == "modpacks") {
                val instLoader = instance.loader.lowercase()
                if (instLoader == "vanilla" || instLoader.isBlank()) {
                    true
                } else {
                    v.loaders.any { it.equals(instLoader, ignoreCase = true) }
                }
            } else {
                true
            }

            matchesVersion && matchesLoader
        }

        Result.success(compatible)
    }

    /**
     * Memeriksa struktur isi file ZIP secara cerdas untuk menentukan jenis addon Minecraft
     */
    fun inspectZipType(zipFile: File): String {
        try {
            ZipFile(zipFile).use { zip ->
                val entries = zip.entries()
                var hasShaders = false
                var hasPackMcmeta = false
                var hasAssets = false
                var hasData = false
                var hasModpackIndex = false
                var hasCurseforgeManifest = false
                var hasModsFolder = false
                var hasJarFiles = false

                while (entries.hasMoreElements()) {
                    val entry = entries.nextElement()
                    val name = entry.name.lowercase()
                    if (name.startsWith("shaders/") || name.contains("/shaders/")) {
                        hasShaders = true
                    }
                    if (name == "pack.mcmeta" || name.endsWith("/pack.mcmeta")) {
                        hasPackMcmeta = true
                    }
                    if (name.startsWith("assets/") || name.contains("/assets/")) {
                        hasAssets = true
                    }
                    if (name.startsWith("data/") || name.contains("/data/")) {
                        hasData = true
                    }
                    if (name.endsWith("modrinth.index.json")) {
                        hasModpackIndex = true
                    }
                    if (name.endsWith("manifest.json")) {
                        hasCurseforgeManifest = true
                    }
                    if (name.startsWith("mods/") || name.contains("/mods/")) {
                        hasModsFolder = true
                    }
                    if (name.endsWith(".jar")) {
                        hasJarFiles = true
                    }
                }

                return when {
                    hasModpackIndex || hasCurseforgeManifest -> "modpacks"
                    hasShaders -> "shaderpacks"
                    hasPackMcmeta && hasAssets -> "resourcepacks"
                    hasPackMcmeta && hasData -> "datapacks"
                    hasModsFolder || hasJarFiles -> "modpacks"
                    else -> "unknown"
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("NuxModManager", "Gagal inspect ZIP: ${e.message}", e)
            return "unknown"
        }
    }

    /**
     * Sistem Auto-Download Dependensi:
     * Mengunduh file utama versi target, lalu secara rekursif mencari dan mengunduh dependensi wajib (required)
     * yang kompatibel dengan instance dan belum terpasang.
     */
    suspend fun downloadVersionAndDependencies(
        context: Context,
        instance: Instance,
        version: ModrinthVersion,
        itemType: String,
        worldName: String? = null,
        onProgress: (statusText: String) -> Unit = {}
    ): Result<List<String>> = withContext(Dispatchers.IO) {
        val visitedProjects = mutableSetOf<String>()
        val downloadedFiles = mutableListOf<String>()

        try {
            // Ambil daftar file yang sudah terpasang agar tidak mendownload ulang
            val installedList = getInstalledItems(context, instance, itemType, worldName)
            val installedFilenames = installedList.map { it.filename.lowercase() }.toMutableSet()

            suspend fun resolveAndDownload(targetVer: ModrinthVersion) {
                if (targetVer.files.isEmpty()) return

                // 1. Pilih primary file atau file pertama
                val primaryFile = targetVer.files.firstOrNull { it.primary } ?: targetVer.files.first()
                val filename = primaryFile.filename

                if (targetVer.projectId.isNotBlank() && visitedProjects.contains(targetVer.projectId)) {
                    return
                }
                if (targetVer.projectId.isNotBlank()) {
                    visitedProjects.add(targetVer.projectId)
                }

                // Download file jika belum ada
                if (!installedFilenames.contains(filename.lowercase())) {
                    onProgress("Mengunduh $filename...")
                    val targetDir = getTargetDir(context, instance, itemType, worldName)
                    val destFile = File(targetDir, filename)

                    downloadFile(primaryFile.url, destFile)
                    installedFilenames.add(filename.lowercase())
                    downloadedFiles.add(filename)

                    if (itemType == "modpacks" && (filename.endsWith(".mrpack", ignoreCase = true) || filename.endsWith(".zip", ignoreCase = true))) {
                        onProgress("Mengekstrak dan menyiapkan modpack...")
                        processMrpackFile(instance, destFile, context) { status, _, _ ->
                            onProgress(status)
                        }
                    }
                }

                // 2. Periksa dependensi wajib (required dependencies)
                val requiredDeps = targetVer.dependencies?.filter {
                    it.dependencyType.equals("required", ignoreCase = true) &&
                            (!it.versionId.isNullOrBlank() || !it.projectId.isNullOrBlank())
                } ?: emptyList()

                for (dep in requiredDeps) {
                    try {
                        if (!dep.projectId.isNullOrBlank() && visitedProjects.contains(dep.projectId)) {
                            continue
                        }

                        var depVersionObj: ModrinthVersion? = null

                        // Cari berdasarkan version_id jika ada
                        if (!dep.versionId.isNullOrBlank()) {
                            val vUrl = "$MODRINTH_API_BASE/version/${dep.versionId}"
                            val req = Request.Builder().url(vUrl).header("User-Agent", USER_AGENT).build()
                            val resp = httpClient.newCall(req).execute()
                            if (resp.isSuccessful) {
                                val vBody = resp.body?.string()
                                if (vBody != null) {
                                    depVersionObj = gson.fromJson(vBody, ModrinthVersion::class.java)
                                }
                            }
                        }

                        // Jika tidak ada version_id atau gagal, cari berdasarkan project_id versi yang kompatibel
                        if (depVersionObj == null && !dep.projectId.isNullOrBlank()) {
                            val compRes = getCompatibleVersions(dep.projectId, instance, itemType)
                            val compList = compRes.getOrNull()
                            if (!compList.isNullOrEmpty()) {
                                depVersionObj = compList.first()
                            }
                        }

                        if (depVersionObj != null && depVersionObj.files.isNotEmpty()) {
                            val depPrimary = depVersionObj.files.firstOrNull { it.primary } ?: depVersionObj.files.first()
                            if (!installedFilenames.contains(depPrimary.filename.lowercase())) {
                                onProgress("Menyelesaikan dependensi: ${depPrimary.filename}...")
                                resolveAndDownload(depVersionObj)
                            }
                        }
                    } catch (e: Exception) {
                        // Jangan batalkan seluruh instalasi jika salah satu dependensi sekunder gagal diakses
                    }
                }
            }

            resolveAndDownload(version)
            Result.success(downloadedFiles)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun processMrpackFile(
        instance: Instance,
        mrpackFile: File,
        context: Context,
        onProgress: (statusText: String, current: Int, total: Int) -> Unit = { _, _, _ -> }
    ) = withContext(Dispatchers.IO) {
        try {
            val gameDir = InstanceManager.getInstanceGameDir(context, instance)
            val cleanFilename = mrpackFile.name.removeSuffix(".disabled")
            val installedRelativeFiles = mutableListOf<String>()
            var packTitle = mrpackFile.nameWithoutExtension
            var packVersion = ""

            ZipFile(mrpackFile).use { zip ->
                // 1. Cari modrinth.index.json
                val indexEntry = zip.getEntry("modrinth.index.json")
                    ?: zip.entries().asSequence().firstOrNull { it.name.endsWith("modrinth.index.json") }

                if (indexEntry != null) {
                    val prefix = if (indexEntry.name.contains("/")) indexEntry.name.substringBeforeLast("/") + "/" else ""
                    val filesToDownload = mutableListOf<Pair<String, String>>() // Pair(downloadUrl, relativePath)

                    zip.getInputStream(indexEntry).bufferedReader().use { reader ->
                        val indexJson = JsonParser.parseReader(reader).asJsonObject
                        if (indexJson.has("name")) {
                            packTitle = indexJson.get("name").asString
                        }
                        if (indexJson.has("versionId")) {
                            packVersion = indexJson.get("versionId").asString
                        }

                        if (indexJson.has("files")) {
                            val files = indexJson.getAsJsonArray("files")
                            for (element in files) {
                                val fileObj = element.asJsonObject
                                val env = fileObj.getAsJsonObject("env")
                                if (env != null && env.has("client") && env.get("client").asString.equals("unsupported", ignoreCase = true)) {
                                    continue
                                }
                                val relPath = fileObj.get("path")?.asString ?: continue
                                val downloads = fileObj.getAsJsonArray("downloads")
                                if (downloads != null && downloads.size() > 0) {
                                    val dlUrl = downloads.get(0).asString
                                    filesToDownload.add(Pair(dlUrl, relPath))
                                }
                            }
                        }
                    }

                    val totalFiles = filesToDownload.size
                    onProgress("Membaca daftar modpack ($totalFiles mod ditemukan)...", 0, totalFiles)
                    var currentIdx = 0
                    for ((dlUrl, relPath) in filesToDownload) {
                        currentIdx++
                        val targetFile = File(gameDir, relPath)
                        installedRelativeFiles.add(relPath)
                        if (!targetFile.exists()) {
                            targetFile.parentFile?.mkdirs()
                            onProgress("Mengunduh mod ($currentIdx/$totalFiles): ${targetFile.name}...", currentIdx, totalFiles)
                            try {
                                downloadFile(dlUrl, targetFile)
                            } catch (e: Exception) {
                                android.util.Log.e("NuxModManager", "Gagal unduh file modpack: $dlUrl", e)
                            }
                        } else {
                            onProgress("Mod sudah ada ($currentIdx/$totalFiles): ${targetFile.name}", currentIdx, totalFiles)
                        }
                    }

                    // 2. Extract overrides/ and client-overrides/
                    onProgress("Mengekstrak konfigurasi modpack...", totalFiles, totalFiles)
                    val overridesPrefix = "${prefix}overrides/"
                    val clientOverridesPrefix = "${prefix}client-overrides/"
                    val entries = zip.entries()
                    while (entries.hasMoreElements()) {
                        val entry = entries.nextElement()
                        val name = entry.name
                        val rel = when {
                            name.startsWith(overridesPrefix) -> name.removePrefix(overridesPrefix)
                            name.startsWith(clientOverridesPrefix) -> name.removePrefix(clientOverridesPrefix)
                            else -> null
                        }
                        if (rel != null && rel.isNotBlank() && !rel.startsWith("saves/")) {
                            val outFile = File(gameDir, rel)
                            installedRelativeFiles.add(rel)
                            if (entry.isDirectory) {
                                outFile.mkdirs()
                            } else {
                                outFile.parentFile?.mkdirs()
                                zip.getInputStream(entry).use { input ->
                                    FileOutputStream(outFile).use { output ->
                                        input.copyTo(output)
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // Fallback jika file zip modpack bukan standar Modrinth mrpack
                    // (misal zip kumpulan mod yang langsung berisi folder mods/ atau file .jar)
                    onProgress("Mengekstrak file arsip modpack...", 0, 0)
                    val entries = zip.entries()
                    while (entries.hasMoreElements()) {
                        val entry = entries.nextElement()
                        val name = entry.name
                        if (entry.isDirectory) continue

                        val (outFile, relPath) = when {
                            name.startsWith("mods/") || name.startsWith("config/") ||
                            name.startsWith("shaderpacks/") || name.startsWith("resourcepacks/") ||
                            name.startsWith("datapacks/") -> {
                                Pair(File(gameDir, name), name)
                            }
                            name.endsWith(".jar", ignoreCase = true) && !name.contains("/") -> {
                                Pair(File(File(gameDir, "mods"), name), "mods/$name")
                            }
                            else -> Pair(null, null)
                        }

                        if (outFile != null && relPath != null) {
                            installedRelativeFiles.add(relPath)
                            outFile.parentFile?.mkdirs()
                            zip.getInputStream(entry).use { input ->
                                FileOutputStream(outFile).use { output ->
                                    input.copyTo(output)
                                }
                            }
                        }
                    }
                }
            }

            // Simpan manifest modpack untuk pelacakan delete & toggle
            val manifest = ModpackManifest(
                modpackName = packTitle,
                version = packVersion,
                archiveFilename = cleanFilename,
                installedFiles = installedRelativeFiles.distinct()
            )
            saveModpackManifest(gameDir, manifest)

            val installedModCount = installedRelativeFiles.count { it.startsWith("mods/") || it.endsWith(".jar", ignoreCase = true) }
            onProgress("Selesai! $installedModCount mod berhasil dipasang.", installedRelativeFiles.size, installedRelativeFiles.size)
        } catch (e: Exception) {
            android.util.Log.e("NuxModManager", "Error processing mrpack: ${e.message}", e)
            throw e
        }
    }

    private fun downloadFile(url: String, destFile: File) {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", USER_AGENT)
            .build()

        val response = httpClient.newCall(request).execute()
        if (!response.isSuccessful) {
            throw Exception("Gagal mengunduh file: HTTP ${response.code}")
        }

        response.body?.byteStream()?.use { input ->
            FileOutputStream(destFile).use { output ->
                input.copyTo(output)
            }
        } ?: throw Exception("Body stream kosong")
    }

    fun getModpackMetadataFile(gameDir: File, cleanFilename: String): File {
        val metaDir = File(gameDir, "modpacks/.metadata")
        if (!metaDir.exists()) metaDir.mkdirs()
        val baseName = if (cleanFilename.endsWith(".disabled")) cleanFilename.removeSuffix(".disabled") else cleanFilename
        return File(metaDir, "$baseName.json")
    }

    fun saveModpackManifest(gameDir: File, manifest: ModpackManifest) {
        try {
            val metaFile = getModpackMetadataFile(gameDir, manifest.archiveFilename)
            metaFile.writeText(gson.toJson(manifest))
        } catch (e: Exception) {
            android.util.Log.e("NuxModManager", "Gagal menyimpan metadata modpack: ${e.message}", e)
        }
    }

    fun loadModpackManifest(gameDir: File, cleanFilename: String): ModpackManifest? {
        try {
            val metaFile = getModpackMetadataFile(gameDir, cleanFilename)
            if (metaFile.exists() && metaFile.isFile) {
                val json = metaFile.readText()
                return gson.fromJson(json, ModpackManifest::class.java)
            }
        } catch (e: Exception) {
            android.util.Log.e("NuxModManager", "Gagal membaca metadata modpack: ${e.message}", e)
        }
        return null
    }

    fun getModpackInstalledFilesFromArchive(archiveFile: File): List<String> {
        val files = mutableListOf<String>()
        if (!archiveFile.exists() || !archiveFile.isFile) return files
        try {
            ZipFile(archiveFile).use { zip ->
                val indexEntry = zip.getEntry("modrinth.index.json")
                    ?: zip.entries().asSequence().firstOrNull { it.name.endsWith("modrinth.index.json") }
                if (indexEntry != null) {
                    val prefix = if (indexEntry.name.contains("/")) indexEntry.name.substringBeforeLast("/") + "/" else ""
                    zip.getInputStream(indexEntry).bufferedReader().use { reader ->
                        val indexJson = JsonParser.parseReader(reader).asJsonObject
                        if (indexJson.has("files")) {
                            for (el in indexJson.getAsJsonArray("files")) {
                                val rel = el.asJsonObject.get("path")?.asString
                                if (!rel.isNullOrBlank()) files.add(rel)
                            }
                        }
                    }
                    val overridesPrefix = "${prefix}overrides/"
                    val clientOverridesPrefix = "${prefix}client-overrides/"
                    val entries = zip.entries()
                    while (entries.hasMoreElements()) {
                        val entry = entries.nextElement()
                        if (entry.isDirectory) continue
                        val name = entry.name
                        val rel = when {
                            name.startsWith(overridesPrefix) -> name.removePrefix(overridesPrefix)
                            name.startsWith(clientOverridesPrefix) -> name.removePrefix(clientOverridesPrefix)
                            else -> null
                        }
                        if (rel != null && rel.isNotBlank() && !rel.startsWith("saves/")) {
                            files.add(rel)
                        }
                    }
                } else {
                    val entries = zip.entries()
                    while (entries.hasMoreElements()) {
                        val entry = entries.nextElement()
                        if (entry.isDirectory) continue
                        val name = entry.name
                        when {
                            name.startsWith("mods/") || name.startsWith("config/") ||
                            name.startsWith("shaderpacks/") || name.startsWith("resourcepacks/") ||
                            name.startsWith("datapacks/") -> {
                                files.add(name)
                            }
                            name.endsWith(".jar", ignoreCase = true) && !name.contains("/") -> {
                                files.add("mods/$name")
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("NuxModManager", "Error getModpackInstalledFilesFromArchive: ${e.message}", e)
        }
        return files.distinct()
    }
}
