package com.israadev.nuxlauncher.core.instance

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.israadev.nuxlauncher.core.models.Instance
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.util.UUID

object InstanceManager {
    private val gson = Gson()
    private val _instances = MutableStateFlow<List<Instance>>(emptyList())
    val instances: StateFlow<List<Instance>> = _instances.asStateFlow()

    private val _selectedInstance = MutableStateFlow<Instance?>(null)
    val selectedInstance: StateFlow<Instance?> = _selectedInstance.asStateFlow()

    fun getNuxDir(context: Context): File {
        // 1. Primary user-visible storage: /storage/emulated/0/nuxlauncher
        try {
            val extStorage = android.os.Environment.getExternalStorageDirectory()
            val nuxPublicDir = File(extStorage, "nuxlauncher")
            if (!nuxPublicDir.exists()) {
                nuxPublicDir.mkdirs()
            }
            if (nuxPublicDir.exists() && nuxPublicDir.canWrite()) {
                return nuxPublicDir
            }
        } catch (_: Exception) {}

        // 2. Secondary fallback: /storage/emulated/0/Android/data/com.israadev.nuxlauncher/files/nuxlauncher
        val appExt = context.getExternalFilesDir(null)
        if (appExt != null) {
            val appExtDir = File(appExt, "nuxlauncher")
            if (appExtDir.exists() || appExtDir.mkdirs()) {
                return appExtDir
            }
        }

        // 3. Fallback: internal app storage
        val internalDir = File(context.filesDir, "nuxlauncher")
        if (!internalDir.exists()) internalDir.mkdirs()
        return internalDir
    }

    fun getAssetsDir(context: Context): File {
        val dir = File(getNuxDir(context), "assets")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun getLibrariesDir(context: Context): File {
        val dir = File(getNuxDir(context), "libraries")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun getVersionsDir(context: Context): File {
        val dir = File(getNuxDir(context), "versions")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun getInstancesDir(context: Context): File {
        val dir = File(getNuxDir(context), "instances")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun getInstanceDir(context: Context, instance: Instance): File {
        return getInstanceDirById(context, instance.id)
    }

    fun getInstanceDirById(context: Context, instanceId: String): File {
        val dir = File(getInstancesDir(context), instanceId)
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun getInstanceGameDir(context: Context, instance: Instance): File {
        val dir = File(getInstanceDir(context, instance), "minecraft")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun openInstanceFolder(context: Context, instance: Instance) {
        val targetDir = getInstanceDir(context, instance)
        if (!targetDir.exists()) {
            targetDir.mkdirs()
        }
        openFolder(context, targetDir)
    }

    fun openFolder(context: Context, folder: File) {
        if (!folder.exists()) {
            folder.mkdirs()
        }

        val primaryStorage = android.os.Environment.getExternalStorageDirectory().absolutePath
        val relativePath = if (folder.absolutePath.startsWith(primaryStorage)) {
            folder.absolutePath.removePrefix(primaryStorage).trimStart('/')
        } else {
            folder.name
        }

        // 1. ZArchiver (priority for Android Minecraft community)
        val pm = context.packageManager
        val isZArchiverInstalled = try {
            pm.getPackageInfo("ru.zdevs.zarchiver", 0)
            true
        } catch (_: Exception) {
            false
        }

        if (isZArchiverInstalled) {
            try {
                val oldPolicy = android.os.StrictMode.getVmPolicy()
                try {
                    android.os.StrictMode.setVmPolicy(android.os.StrictMode.VmPolicy.Builder().build())
                    val zarchiverIntent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                        setClassName("ru.zdevs.zarchiver", "ru.zdevs.zarchiver.ZArchiver")
                        setDataAndType(android.net.Uri.fromFile(folder), "resource/folder")
                        addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(zarchiverIntent)
                    return
                } finally {
                    android.os.StrictMode.setVmPolicy(oldPolicy)
                }
            } catch (e: Exception) {
                android.util.Log.w("InstanceManager", "Failed to launch ZArchiver: ${e.message}")
            }
        }

        // 2. DocumentsUI (System Files)
        try {
            val documentUri = android.provider.DocumentsContract.buildDocumentUri(
                "com.android.externalstorage.documents",
                "primary:$relativePath"
            )
            val docIntent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                setDataAndType(documentUri, android.provider.DocumentsContract.Document.MIME_TYPE_DIR)
                addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(docIntent)
            return
        } catch (e: Exception) {
            android.util.Log.w("InstanceManager", "Failed to launch DocumentsUI: ${e.message}")
        }

        // 3. DocumentsUI alternate MIME
        try {
            val documentUri = android.provider.DocumentsContract.buildDocumentUri(
                "com.android.externalstorage.documents",
                "primary:$relativePath"
            )
            val docIntent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                setDataAndType(documentUri, "vnd.android.document/directory")
                addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(docIntent)
            return
        } catch (e: Exception) {
            android.util.Log.w("InstanceManager", "Failed to launch DocumentsUI vnd: ${e.message}")
        }

        // 4. FileProvider Chooser
        try {
            val fileUri = androidx.core.content.FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                folder
            )
            val chooserIntent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                setDataAndType(fileUri, "resource/folder")
                addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooser = android.content.Intent.createChooser(chooserIntent, "Buka Folder").apply {
                addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
            return
        } catch (e: Exception) {
            android.util.Log.w("InstanceManager", "Failed to launch FileProvider: ${e.message}")
        }

        // 5. Fallback: Copy to clipboard
        try {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
            val clip = android.content.ClipData.newPlainText("Folder Path", folder.absolutePath)
            clipboard.setPrimaryClip(clip)
            android.widget.Toast.makeText(
                context,
                "Lokasi folder disalin ke clipboard:\n${folder.absolutePath}",
                android.widget.Toast.LENGTH_LONG
            ).show()
        } catch (_: Exception) {
            android.widget.Toast.makeText(context, "Folder: ${folder.absolutePath}", android.widget.Toast.LENGTH_LONG).show()
        }
    }

    // Alias for backward compatibility
    fun getMinecraftDir(context: Context): File {
        return getNuxDir(context)
    }

    private fun getInstancesFile(context: Context): File {
        return File(getNuxDir(context), "instances.json")
    }

    private fun migrateOldStorage(context: Context) {
        try {
            val targetDir = getNuxDir(context)

            // Previous possible locations
            val oldLocations = listOf(
                File(context.dataDir ?: context.filesDir, "nuxlauncher"),
                File(context.filesDir, "nuxlauncher"),
                File(context.filesDir, ".minecraft")
            )

            for (oldLoc in oldLocations) {
                if (!oldLoc.exists()) continue
                if (oldLoc.canonicalPath == targetDir.canonicalPath) continue

                // 1. Assets
                val oldAssets = File(oldLoc, "assets")
                val newAssets = File(targetDir, "assets")
                if (oldAssets.exists()) {
                    if (!newAssets.exists() || newAssets.list().isNullOrEmpty()) {
                        oldAssets.copyRecursively(newAssets, overwrite = true)
                    }
                }

                // 2. Libraries
                val oldLibs = File(oldLoc, "libraries")
                val newLibs = File(targetDir, "libraries")
                if (oldLibs.exists()) {
                    if (!newLibs.exists() || newLibs.list().isNullOrEmpty()) {
                        oldLibs.copyRecursively(newLibs, overwrite = true)
                    }
                }

                // 3. Versions
                val oldVersions = File(oldLoc, "versions")
                val newVersions = File(targetDir, "versions")
                if (oldVersions.exists()) {
                    if (!newVersions.exists() || newVersions.list().isNullOrEmpty()) {
                        oldVersions.copyRecursively(newVersions, overwrite = true)
                    }
                }

                // 4. Instances
                val oldInstances = File(oldLoc, "instances")
                if (oldInstances.exists()) {
                    val newInstances = File(targetDir, "instances")
                    if (!newInstances.exists()) newInstances.mkdirs()
                    oldInstances.listFiles()?.forEach { instFolder ->
                        if (instFolder.isDirectory) {
                            val targetInstDir = File(newInstances, instFolder.name)
                            if (!targetInstDir.exists() || targetInstDir.list().isNullOrEmpty()) {
                                instFolder.copyRecursively(targetInstDir, overwrite = true)
                            }
                        }
                    }
                }

                // 5. instances.json
                val oldInstJson = File(oldLoc, "instances.json")
                val newInstJson = File(targetDir, "instances.json")
                if (oldInstJson.exists() && (!newInstJson.exists() || newInstJson.length() == 0L)) {
                    oldInstJson.copyTo(newInstJson, overwrite = true)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private const val PREFS_NAME = "nux_launcher_prefs"
    private const val KEY_LAST_SELECTED_INSTANCE = "last_selected_instance_id"
    private var appContext: Context? = null

    private fun saveSelectedInstanceId(context: Context, id: String) {
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putString(KEY_LAST_SELECTED_INSTANCE, id).apply()
            File(context.filesDir, "selected_instance_id.txt").writeText(id)
        } catch (_: Exception) {}
    }

    private fun loadSelectedInstanceId(context: Context): String? {
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val id = prefs.getString(KEY_LAST_SELECTED_INSTANCE, null)
            if (!id.isNullOrBlank()) return id
            val file = File(context.filesDir, "selected_instance_id.txt")
            if (file.exists()) {
                val text = file.readText().trim()
                if (text.isNotEmpty()) return text
            }
        } catch (_: Exception) {}
        return null
    }

    fun init(context: Context) {
        appContext = context.applicationContext
        migrateOldStorage(context)

        val file = getInstancesFile(context)
        var loadedList = mutableListOf<Instance>()
        if (file.exists()) {
            try {
                val json = file.readText()
                val type = object : TypeToken<List<Instance>>() {}.type
                val list: List<Instance>? = gson.fromJson(json, type)
                if (list != null) {
                    loadedList.addAll(list.filterNot { it.id == "instance_default" || it.name == "Minecraft 1.20.4" })
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // Scan instances directory for isolated instance.json (like Windows version)
        val instancesDir = getInstancesDir(context)
        instancesDir.listFiles()?.forEach { instFolder ->
            if (instFolder.isDirectory) {
                val configFile = File(instFolder, "instance.json")
                if (configFile.exists() && loadedList.none { it.id == instFolder.name }) {
                    try {
                        val inst = gson.fromJson(configFile.readText(), Instance::class.java)
                        if (inst != null) {
                            loadedList.add(inst)
                        }
                    } catch (_: Exception) {}
                }
            }
        }

        // Check if downloaded client JAR and loader exist for each instance
        val verifiedList = loadedList.map { inst ->
            inst.copy(isDownloaded = isInstanceDownloaded(context, inst))
        }

        _instances.value = verifiedList

        // Preserve and restore the user's last selected instance
        val savedId = loadSelectedInstanceId(context)
        val currentSelectedId = _selectedInstance.value?.id ?: savedId

        val targetInstance = verifiedList.find { it.id == currentSelectedId }
            ?: (if (savedId != null) verifiedList.find { it.id == savedId } else null)
            ?: verifiedList.firstOrNull()

        _selectedInstance.value = targetInstance
        targetInstance?.let { saveSelectedInstanceId(context, it.id) }
        save(context)
    }

    fun isInstanceDownloaded(context: Context, inst: Instance): Boolean {
        val versionsDir = getVersionsDir(context)
        val vDir = File(versionsDir, inst.mcVersion)
        val jarFile = File(vDir, "${inst.mcVersion}.jar")
        val jsonFile = File(vDir, "${inst.mcVersion}.json")
        if (!jarFile.exists() || jarFile.length() == 0L || !jsonFile.exists() || jsonFile.length() == 0L) {
            return false
        }
        if (inst.loader.equals("fabric", ignoreCase = true)) {
            val fabricJson = if (inst.loaderVersion.isNotBlank()) {
                File(vDir, "fabric-${inst.loaderVersion}.json")
            } else {
                vDir.listFiles { f -> f.name.startsWith("fabric-") && f.name.endsWith(".json") }?.firstOrNull()
            }
            if (fabricJson == null || !fabricJson.exists() || fabricJson.length() == 0L) {
                return false
            }
            val libDir = getLibrariesDir(context)
            val fabricLoaderDir = File(libDir, "net/fabricmc/fabric-loader")
            if (!fabricLoaderDir.exists() || fabricLoaderDir.listFiles().isNullOrEmpty()) {
                return false
            }
        }
        return true
    }


    fun createInstance(context: Context, instance: Instance) {
        // Create isolated instance folder structure
        val instDir = getInstanceDir(context, instance)
        val gameDir = getInstanceGameDir(context, instance)
        instDir.mkdirs()
        gameDir.mkdirs()

        // Write isolated instance.json
        try {
            File(instDir, "instance.json").writeText(gson.toJson(instance))
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val list = _instances.value.toMutableList()
        list.add(0, instance)
        _instances.value = list
        _selectedInstance.value = instance
        saveSelectedInstanceId(context, instance.id)
        save(context)
    }

    fun selectInstance(instance: Instance) {
        _selectedInstance.value = instance
        appContext?.let { saveSelectedInstanceId(it, instance.id) }
    }

    fun updateInstance(context: Context, updated: Instance) {
        val instDir = getInstanceDir(context, updated)
        try {
            File(instDir, "instance.json").writeText(gson.toJson(updated))
        } catch (_: Exception) {}

        val list = _instances.value.map { if (it.id == updated.id) updated else it }
        _instances.value = list
        if (_selectedInstance.value?.id == updated.id) {
            _selectedInstance.value = updated
        }
        save(context)
    }

    fun deleteInstance(context: Context, instanceId: String) {
        val list = _instances.value.filterNot { it.id == instanceId }
        _instances.value = list
        if (_selectedInstance.value?.id == instanceId) {
            val nextInst = list.firstOrNull()
            _selectedInstance.value = nextInst
            nextInst?.let { saveSelectedInstanceId(context, it.id) }
        }
        save(context)

        // Delete isolated instance directory and all contents from storage
        try {
            val instDir = getInstanceDirById(context, instanceId)
            if (instDir.exists()) {
                instDir.deleteRecursively()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun save(context: Context) {
        try {
            val file = getInstancesFile(context)
            file.writeText(gson.toJson(_instances.value))
            // Also sync each instance.json in its isolated directory
            _instances.value.forEach { inst ->
                try {
                    val instDir = getInstanceDir(context, inst)
                    File(instDir, "instance.json").writeText(gson.toJson(inst))
                } catch (_: Exception) {}
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
