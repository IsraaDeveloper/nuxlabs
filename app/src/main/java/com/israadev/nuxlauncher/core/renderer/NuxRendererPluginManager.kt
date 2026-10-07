package com.israadev.nuxlauncher.core.renderer

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.util.Log
import java.io.File

object NuxRendererPluginManager {
    private const val TAG = "NuxRendererPlugin"
    private val pluginRenderers = mutableListOf<NuxRendererInfo>()

    fun getPluginRenderers(): List<NuxRendererInfo> = synchronized(pluginRenderers) {
        pluginRenderers.toList()
    }

    fun scanPlugins(context: Context): List<NuxRendererInfo> {
        val detected = mutableListOf<NuxRendererInfo>()
        val pm = context.packageManager
        val processedPackages = mutableSetOf<String>()

        fun processAppInfo(info: ApplicationInfo) {
            val pkg = info.packageName
            if (pkg == context.packageName || !processedPackages.add(pkg)) return

            try {
                val metaData = info.metaData ?: run {
                    try {
                        pm.getApplicationInfo(pkg, PackageManager.GET_META_DATA).metaData
                    } catch (_: Exception) {
                        null
                    }
                }

                val nativeLibDir = info.nativeLibraryDir
                val appLabel = runCatching { info.loadLabel(pm).toString() }.getOrDefault(pkg)

                // 1. Check FCL / Zalith Plugin metadata
                if (metaData != null && (
                    metaData.getBoolean("fclPlugin", false) ||
                    metaData.getBoolean("zalithRendererPlugin", false) ||
                    metaData.containsKey("renderer")
                )) {
                    val rendererString = metaData.getString("renderer") ?: ""
                    val des = metaData.getString("des") ?: appLabel
                    val pojavEnvString = metaData.getString("pojavEnv") ?: ""

                    val parts = rendererString.split(":")
                    var rendererId = if (parts.isNotEmpty() && parts[0].isNotBlank()) parts[0] else "opengles3"
                    val glName = if (parts.size > 1 && parts[1].isNotBlank()) parts[1] else "libgl4es.so"
                    val rawEglName = if (parts.size > 2 && parts[2].isNotBlank()) parts[2] else null
                    val eglName = if (rawEglName != null && rawEglName.startsWith("/")) "$nativeLibDir$rawEglName" else rawEglName

                    val envMap = mutableMapOf<String, String>()
                    val dlopenList = mutableListOf<String>()

                    if (pojavEnvString.isNotBlank()) {
                        pojavEnvString.split(":").forEach { envPair ->
                            if (envPair.contains("=")) {
                                val split = envPair.split("=", limit = 2)
                                val k = split[0].trim()
                                val v = split[1].trim()
                                when (k) {
                                    "POJAV_RENDERER" -> {
                                        rendererId = v
                                    }
                                    "DLOPEN" -> {
                                        v.split(",").forEach { dl ->
                                            if (dl.isNotBlank()) dlopenList.add(dl.trim())
                                        }
                                    }
                                    "LIB_MESA_NAME", "MESA_LIBRARY" -> {
                                        envMap[k] = "$nativeLibDir/$v"
                                    }
                                    else -> envMap[k] = v
                                }
                            }
                        }
                    }


                    val minVer = metaData.getString("minMCVer")
                    val maxVer = metaData.getString("maxMCVer")

                    val pluginInfo = NuxRendererInfo(
                        id = "plugin_$pkg",
                        displayName = des,
                        badge = "Plugin: $appLabel",
                        summary = if (pkg == "top.mobilegl.plugin" || des.equals("MobileGL", ignoreCase = true)) 
                            "Pustaka grafis MobileGL (Vulkan & GLES backend)" 
                        else "Renderer eksternal dari APK: $appLabel ($pkg)",
                        compatibility = "Minecraft (APK Plugin)",
                        rendererId = rendererId,
                        libraryName = glName,
                        eglName = eglName,
                        envVariables = envMap,
                        isPlugin = true,
                        pluginPackageName = pkg,
                        pluginNativePath = nativeLibDir,
                        dlopenLibs = dlopenList,
                        minMCVersion = minVer,
                        maxMCVersion = maxVer
                    )
                    detected.add(pluginInfo)
                    Log.i(TAG, "Found renderer plugin (metadata): ${pluginInfo.displayName} ($pkg)")
                    return
                }

                // 2. Heuristic detection: check for native libraries in package dir (MobileGL, MobileGlues, LTW, Holy GL4ES, etc.)
                val nativeDirFile = File(nativeLibDir)
                if (nativeDirFile.exists() && nativeDirFile.isDirectory) {
                    val soFiles = nativeDirFile.listFiles { f -> f.extension == "so" } ?: emptyArray()
                    val hasMobileGL = soFiles.any { it.name.contains("mobilegl", ignoreCase = true) }
                    val hasMobileGlues = soFiles.any { it.name.contains("mobileglue", ignoreCase = true) }
                    val hasGl4es = soFiles.any { it.name.contains("gl4es", ignoreCase = true) }
                    val hasLtw = soFiles.any { it.name.contains("ltw", ignoreCase = true) || it.name.contains("turnip", ignoreCase = true) }
                    val isKnownPkg = pkg.contains("mobilegl", ignoreCase = true) ||
                            pkg.contains("gl4es", ignoreCase = true) ||
                            pkg.contains("renderer", ignoreCase = true) ||
                            pkg.contains("ltw", ignoreCase = true) ||
                            pkg.contains("ngg", ignoreCase = true)

                    if (hasMobileGL || hasMobileGlues || hasGl4es || hasLtw || isKnownPkg) {
                        val glName = soFiles.firstOrNull { it.name.startsWith("libmobilegl", ignoreCase = true) || it.name.startsWith("libgl") }?.name ?: "libgl4es.so"
                        val eglName = soFiles.firstOrNull { it.name.contains("egl", ignoreCase = true) }?.name
                        val dlopenList = soFiles.map { it.name }

                        val pluginInfo = NuxRendererInfo(
                            id = "plugin_$pkg",
                            displayName = appLabel,
                            badge = "Plugin Eksternal",
                            summary = "Renderer APK: $appLabel ($pkg)",
                            compatibility = "Minecraft (APK Plugin)",
                            rendererId = when {
                                hasMobileGlues -> "mobileglues"
                                hasMobileGL -> "mobilegl"
                                hasGl4es -> "opengles2"
                                else -> "opengles3"
                            },
                            libraryName = glName,
                            eglName = eglName,
                            envVariables = mapOf(
                                "POJAVEXEC_EGL" to (eglName ?: glName),
                                "LIBGL_EGL" to (eglName ?: glName)
                            ),
                            isPlugin = true,
                            pluginPackageName = pkg,
                            pluginNativePath = nativeLibDir,
                            dlopenLibs = dlopenList
                        )
                        detected.add(pluginInfo)
                        Log.i(TAG, "Found renderer plugin (heuristic): ${pluginInfo.displayName} ($pkg)")
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error checking package $pkg for renderer plugin", e)
            }
        }

        // Method 1: queryIntentActivities
        try {
            val intent = Intent(Intent.ACTION_MAIN)
            val activities = pm.queryIntentActivities(intent, PackageManager.GET_META_DATA)
            for (resolve in activities) {
                val appInfo = resolve.activityInfo?.applicationInfo ?: continue
                processAppInfo(appInfo)
            }
        } catch (e: Exception) {
            Log.w(TAG, "queryIntentActivities failed", e)
        }

        // Method 2: getInstalledApplications
        try {
            val installed = pm.getInstalledApplications(PackageManager.GET_META_DATA)
            for (appInfo in installed) {
                processAppInfo(appInfo)
            }
        } catch (e: Exception) {
            Log.w(TAG, "getInstalledApplications failed", e)
        }

        synchronized(pluginRenderers) {
            pluginRenderers.clear()
            pluginRenderers.addAll(detected)
        }
        NuxRendererRegistry.setPluginRenderers(detected)
        try {
            com.israadev.nuxlauncher.core.renderer.v2.NuxRendererV2Manager.scanV2Plugins(context)
        } catch (e: Exception) {
            Log.w(TAG, "scanV2Plugins failed", e)
        }
        return detected
    }
}
