package com.israadev.nuxlauncher.core.mods

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipInputStream

data class PendingAddonImport(
    val uri: Uri,
    val fileName: String,
    val suggestedType: String, // "mods" | "modpacks" | "resourcepacks" | "shaderpacks" | "unknown"
    val timestamp: Long = System.currentTimeMillis()
)

object NuxAddonImportManager {
    private val _pendingImport = MutableStateFlow<PendingAddonImport?>(null)
    val pendingImport = _pendingImport.asStateFlow()

    fun setPendingImport(import: PendingAddonImport) {
        _pendingImport.value = import
    }

    fun clearPendingImport() {
        _pendingImport.value = null
    }

    fun getFileName(context: Context, uri: Uri): String {
        var name = "addon_file"
        try {
            if (uri.scheme == "content") {
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIdx != -1 && cursor.moveToFirst()) {
                        name = cursor.getString(nameIdx)
                    }
                }
            } else if (uri.scheme == "file") {
                name = uri.lastPathSegment ?: "addon_file"
            }
        } catch (_: Exception) {}
        return name
    }

    fun resolveTypeFromAliasOrFile(context: Context, uri: Uri, aliasClass: String?): String {
        if (!aliasClass.isNullOrBlank()) {
            when {
                aliasClass.endsWith("ImportModActivity") -> return "mods"
                aliasClass.endsWith("ImportModpackActivity") -> return "modpacks"
                aliasClass.endsWith("ImportResourcepackActivity") -> return "resourcepacks"
                aliasClass.endsWith("ImportShaderpackActivity") -> return "shaderpacks"
            }
        }

        val fileName = getFileName(context, uri).lowercase()
        return when {
            fileName.endsWith(".jar") -> "mods"
            fileName.endsWith(".mrpack") -> "modpacks"
            fileName.endsWith(".zip") -> inspectZipFromUri(context, uri)
            else -> "unknown"
        }
    }

    private fun inspectZipFromUri(context: Context, uri: Uri): String {
        try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                ZipInputStream(stream).use { zis ->
                    var entry = zis.nextEntry
                    var hasShaders = false
                    var hasPackMcmeta = false
                    var hasAssets = false
                    var hasModpackIndex = false
                    var hasCurseforgeManifest = false
                    var hasModsFolder = false
                    var hasJar = false
                    var count = 0

                    while (entry != null && count < 60) {
                        count++
                        val name = entry.name.lowercase()
                        if (name.startsWith("shaders/") || name.contains("/shaders/")) hasShaders = true
                        if (name == "pack.mcmeta" || name.endsWith("/pack.mcmeta")) hasPackMcmeta = true
                        if (name.startsWith("assets/") || name.contains("/assets/")) hasAssets = true
                        if (name.endsWith("modrinth.index.json")) hasModpackIndex = true
                        if (name.endsWith("manifest.json")) hasCurseforgeManifest = true
                        if (name.startsWith("mods/") || name.contains("/mods/")) hasModsFolder = true
                        if (name.endsWith(".jar")) hasJar = true
                        zis.closeEntry()
                        entry = zis.nextEntry
                    }

                    return when {
                        hasModpackIndex || hasCurseforgeManifest -> "modpacks"
                        hasShaders -> "shaderpacks"
                        hasPackMcmeta && hasAssets -> "resourcepacks"
                        hasModsFolder || hasJar -> "modpacks"
                        hasPackMcmeta -> "resourcepacks"
                        else -> "modpacks"
                    }
                }
            }
        } catch (_: Exception) {}
        return "modpacks"
    }
}
