package com.israadev.nuxlauncher.core.renderer.v2

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import com.israadev.nuxlauncher.core.renderer.NuxRendererInfo
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

object NuxRendererV2Manager {
    private const val TAG = "NuxRendererV2Manager"
    private val v2Plugins = mutableListOf<RendererV2Data>()

    fun getAllV2Plugins(): List<RendererV2Data> = synchronized(v2Plugins) {
        v2Plugins.toList()
    }

    fun getPluginData(packageNameOrId: String): RendererV2Data? = synchronized(v2Plugins) {
        if (packageNameOrId.isBlank()) return null
        v2Plugins.firstOrNull { 
            it.packageName.equals(packageNameOrId, ignoreCase = true) || 
            it.config.rendererId.equals(packageNameOrId, ignoreCase = true) ||
            it.config.displayName.equals(packageNameOrId, ignoreCase = true) ||
            "plugin_${it.packageName}".equals(packageNameOrId, ignoreCase = true) ||
            (packageNameOrId.contains("mobilegl", ignoreCase = true) && !packageNameOrId.contains("mobileglue", ignoreCase = true) && it.packageName == "top.mobilegl.plugin")
        }
    }

    /**
     * Memeriksa apakah renderer memiliki unit pengaturan konfigurasi V2 aktif.
     * Khusus MobileGlues bawaan/plugin lama selalu return false (sesuai Zalith).
     */
    fun isConfigurableRenderer(renderer: NuxRendererInfo): Boolean {
        if (renderer.id == "mobileglues" || 
            renderer.id.contains("mobileglue", ignoreCase = true) || 
            renderer.pluginPackageName == "com.fcl.plugin.mobileglues" || 
            renderer.displayName.contains("MobileGlues", ignoreCase = true)) {
            return false
        }
        val targetData = getPluginData(renderer.pluginPackageName ?: "") 
            ?: getPluginData(renderer.id) 
            ?: getPluginData(renderer.displayName)
        return targetData != null && targetData.units.isNotEmpty()
    }

    /**
     * Memindai plugin renderer V2 dari seluruh aplikasi yang terpasang di Android (fclPlugin_V2 & MobileGL)
     */
    fun scanV2Plugins(context: Context): List<RendererV2Data> {
        val result = mutableListOf<RendererV2Data>()
        val pm = context.packageManager
        val installedApps = try {
            pm.getInstalledApplications(PackageManager.GET_META_DATA)
        } catch (e: Exception) {
            Log.e(TAG, "Gagal mengambil daftar aplikasi terpasang", e)
            emptyList()
        }

        for (info in installedApps) {
            val pkg = info.packageName
            if (pkg == context.packageName) continue

            val metaData = info.metaData ?: continue
            val nativeLibDir = info.nativeLibraryDir
            val appLabel = runCatching { info.loadLabel(pm).toString() }.getOrDefault(pkg)

            // 1. Coba baca fclPlugin_V2 jika tersedia (MobileGL, NGG V2, dsb)
            val configResId = runCatching { metaData.getInt("fclPlugin_V2", -1).takeIf { it > 0 } }.getOrNull()
            if (configResId != null) {
                try {
                    val res = pm.getResourcesForApplication(info)
                    val jsonString = res.getString(configResId)
                    val parsedConfig = parseRendererConfigJson(jsonString, nativeLibDir, info, pm)
                    if (parsedConfig != null) {
                        val data = RendererV2Data(
                            packageName = pkg,
                            nativePath = nativeLibDir,
                            summary = "Renderer Plugin V2: $appLabel",
                            config = parsedConfig,
                            context = context
                        )
                        result.add(data)
                        Log.i(TAG, "Berhasil memuat Renderer Plugin V2 dari $pkg ($appLabel)")
                        continue
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Gagal mengurai fclPlugin_V2 dari $pkg", e)
                }
            }

            // 2. Fallback khusus untuk MobileGL (top.mobilegl.plugin) jika manifest resource gagal diurai
            val isMobileGLPkg = pkg == "top.mobilegl.plugin" || 
                    (pkg.contains("mobilegl", ignoreCase = true) && !pkg.contains("mobileglue", ignoreCase = true))

            if (isMobileGLPkg) {
                val mgConfig = createDefaultMobileGLV2Config(nativeLibDir)
                val data = RendererV2Data(
                    packageName = pkg,
                    nativePath = nativeLibDir,
                    summary = "MobileGL Graphics Engine ($appLabel)",
                    config = mgConfig,
                    context = context
                )
                result.add(data)
                Log.i(TAG, "Menerapkan profil konfigurasi V2 bawaan untuk MobileGL ($pkg)")
            }
        }

        // Jika MobileGL terdeteksi tetapi belum ada konfigurasi, atau fallback default untuk MobileGL
        if (result.none { it.packageName == "top.mobilegl.plugin" }) {
            val defaultMobileGLData = RendererV2Data(
                packageName = "top.mobilegl.plugin",
                nativePath = "",
                summary = "MobileGL Engine (Standard V2 Profile)",
                config = createDefaultMobileGLV2Config(""),
                context = context
            )
            result.add(defaultMobileGLData)
        }

        synchronized(v2Plugins) {
            v2Plugins.clear()
            v2Plugins.addAll(result)
        }

        return result
    }

    /**
     * Mengurai string JSON fclPlugin_V2 menjadi objek RendererV2Config
     */
    private fun parseRendererConfigJson(
        jsonString: String, 
        nativeLibDir: String,
        info: ApplicationInfo? = null,
        pm: PackageManager? = null
    ): RendererV2Config? {
        return try {
            val root = JSONObject(jsonString)
            val displayName = root.optString("displayName", "MobileGL")
            val rendererId = root.optString("rendererId", "opengles3")
            val rendererGLPath = resolveNativePath(root.optString("rendererGLPath", ""), nativeLibDir)
            val rendererEGLPath = resolveNativePath(root.optString("rendererEGLPath", ""), nativeLibDir)

            val dlopenLibPaths = mutableListOf<String>()
            val dlArray = root.optJSONArray("dlopenLibPaths")
            if (dlArray != null) {
                for (i in 0 until dlArray.length()) {
                    val p = dlArray.getString(i)
                    if (p.isNotBlank()) dlopenLibPaths.add(resolveNativePath(p, nativeLibDir))
                }
            }

            val envList = mutableListOf<EnvConfig>()
            val envArray = root.optJSONArray("env")
            if (envArray != null) {
                for (i in 0 until envArray.length()) {
                    val envObj = envArray.getJSONObject(i)
                    val envType = envObj.optString("type", envObj.optString("@type", ""))
                    val key = envObj.optString("key", "")
                    if (key.isBlank()) continue

                    val rawTitleKey = envObj.optJSONObject("title")?.optString("key") ?: envObj.optString("title", null)
                    val resolvedTitle = resolveEnvTitle(rawTitleKey, info, pm)

                    when {
                        envType.contains("Selectable") || envObj.has("items") -> {
                            val itemsObj = envObj.optJSONObject("items")
                            val defVal = itemsObj?.optString("defaultValue", "") ?: ""
                            val vals = mutableListOf<String>()
                            val valArr = itemsObj?.optJSONArray("values")
                            if (valArr != null) {
                                for (j in 0 until valArr.length()) vals.add(valArr.getString(j))
                            }
                            val check = if (envObj.has("check")) envObj.optBoolean("check", true) else null
                            envList.add(EnvConfig.SelectableEnv(key, resolvedTitle, check, defVal, vals))
                        }
                        envType.contains("Toggleable") || envObj.has("toggle") -> {
                            val value = resolveNativePath(envObj.optString("value", "1"), nativeLibDir)
                            val toggle = envObj.optBoolean("toggle", false)
                            envList.add(EnvConfig.ToggleableEnv(key, value, resolvedTitle, toggle))
                        }
                        envType.contains("Customizable") || envObj.has("defaultValue") -> {
                            val defVal = envObj.optString("defaultValue", null)
                            envList.add(EnvConfig.CustomizableEnv(key, resolvedTitle, defVal))
                        }
                        else -> {
                            val value = resolveNativePath(envObj.optString("value", ""), nativeLibDir)
                            envList.add(EnvConfig.NormalEnv(key, value))
                        }
                    }
                }
            }

            RendererV2Config(
                displayName = displayName,
                rendererId = rendererId,
                rendererGLPath = rendererGLPath,
                rendererEGLPath = rendererEGLPath,
                dlopenLibPaths = dlopenLibPaths,
                env = envList,
                minMCVer = root.optString("minMCVer", null),
                maxMCVer = root.optString("maxMCVer", null)
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing renderer config JSON", e)
            null
        }
    }

    private fun resolveEnvTitle(titleKey: String?, info: ApplicationInfo?, pm: PackageManager?): String? {
        if (titleKey == null || titleKey.isBlank()) return null
        if (info != null && pm != null) {
            runCatching {
                val metaData = info.metaData
                if (metaData != null) {
                    val resId = metaData.getInt(titleKey, -1).takeIf { it > 0 }
                    if (resId != null) {
                        val res = pm.getResourcesForApplication(info)
                        val str = res.getString(resId)
                        if (str.isNotBlank()) return str
                    }
                }
            }
        }
        // Human-friendly dictionary fallback for known MobileGL keys
        return when (titleKey) {
            "mobilegl_backend_type_title" -> "Rendering backend (DirectGLES / DirectVulkan)"
            "mobilegl_use_angle_title" -> "Use ANGLE GLES libraries"
            "mobilegl_disable_subgroup_title" -> "Disable Vulkan shader subgroups"
            "mobilegl_disable_timerquery_title" -> "Disable GPU timer queries"
            "mobilegl_magma_r11g11b10f_fallback_title" -> "Use Magma R11G11B10F fallback"
            "mobilegl_magma_frames_inflight_title" -> "Magma frames in flight (1-64)"
            "mobilegl_avoid_sampler_mipmap_min_filter_title" -> "Avoid sampler mipmap min filter"
            "mobilegl_coherent_as_flush_title" -> "Coherent as flush"
            "mobilegl_relaxed_semantics_title" -> "Relaxed semantics"
            else -> titleKey
        }
    }

    private fun resolveNativePath(path: String, nativeLibDir: String): String {
        if (!path.startsWith("**|") || nativeLibDir.isBlank()) return path
        val rel = path.removePrefix("**|")
        return File(nativeLibDir, rel).absolutePath
    }

    /**
     * Profil V2 bawaan lengkap untuk MobileGL persis seperti di Zalith Launcher
     * (MOBILEGL_BACKEND_TYPE, MOBILEGL_DISABLE_TIMERQUERY, MOBILEGL_MAGMA_DISABLE_SUBGROUP, dll)
     */
    fun createDefaultMobileGLV2Config(nativeLibDir: String): RendererV2Config {
        val glPath = if (nativeLibDir.isNotBlank()) "$nativeLibDir/libMobileGL.so" else "libMobileGL.so"
        val envList = listOf(
            EnvConfig.SelectableEnv(
                key = "MOBILEGL_BACKEND_TYPE",
                title = "Rendering backend (DirectGLES / DirectVulkan)",
                check = true,
                defaultValue = "DirectGLES",
                values = listOf("DirectGLES", "DirectVulkan")
            ),
            EnvConfig.ToggleableEnv(
                key = "MOBILEGL_DISABLE_TIMERQUERY",
                value = "1",
                title = "Disable GPU timer queries",
                toggle = false
            ),
            EnvConfig.ToggleableEnv(
                key = "MOBILEGL_MAGMA_DISABLE_SUBGROUP",
                value = "1",
                title = "Disable Vulkan shader subgroups",
                toggle = false
            ),
            EnvConfig.ToggleableEnv(
                key = "MOBILEGL_MAGMA_R11G11B10F_FALLBACK",
                value = "1",
                title = "Use Magma R11G11B10F fallback",
                toggle = false
            ),
            EnvConfig.CustomizableEnv(
                key = "MOBILEGL_MAGMA_FRAMESINFLIGHT",
                title = "Magma frames in flight (1-64)",
                defaultValue = "3"
            ),
            EnvConfig.ToggleableEnv(
                key = "MOBILEGL_ESPRYT_AVOID_SAMPLER_MIPMAP_MIN_FILTER",
                value = "1",
                title = "Avoid sampler mipmap min filter",
                toggle = false
            ),
            EnvConfig.ToggleableEnv(
                key = "MOBILEGL_COHERENT_AS_FLUSH",
                value = "1",
                title = "Coherent as flush",
                toggle = false
            ),
            EnvConfig.ToggleableEnv(
                key = "MOBILEGL_RELAXED_SEMANTICS",
                value = "1",
                title = "Relaxed semantics",
                toggle = false
            ),
            EnvConfig.ToggleableEnv(
                key = "MOBILEGL_ESPRYT_USE_ANGLE",
                value = "1",
                title = "Use ANGLE GLES libraries",
                toggle = false
            ),
            EnvConfig.ToggleableEnv(
                key = "allow_glsl_extension_directive_midshader",
                value = "true",
                title = "Allow GLSL extension directive midshader",
                toggle = true
            ),
            EnvConfig.ToggleableEnv(
                key = "allow_higher_compat_version",
                value = "true",
                title = "Allow higher OpenGL compatibility version",
                toggle = true
            )
        )

        return RendererV2Config(
            displayName = "MobileGL",
            rendererId = "opengles3",
            rendererGLPath = glPath,
            rendererEGLPath = glPath,
            dlopenLibPaths = emptyList(),
            env = envList
        )
    }

    /**
     * Mengambil map environment variable efektif untuk disuntikkan ke proses game saat diluncurkan
     */
    fun getEffectiveEnv(targetRenderer: NuxRendererInfo): Map<String, String> {
        val pluginPkg = targetRenderer.pluginPackageName ?: ""
        val targetData = getPluginData(pluginPkg) 
            ?: getPluginData(targetRenderer.id) 
            ?: getPluginData(targetRenderer.displayName)
            ?: if (targetRenderer.displayName.equals("MobileGL", ignoreCase = true)) getPluginData("top.mobilegl.plugin") else null

        val result = mutableMapOf<String, String>()
        if (targetData != null) {
            result.putAll(targetData.getEffectiveEnv())
        }

        // Pastikan flag GLSL & Extension compatibility selalu aktif untuk MobileGL & Custom Renderer
        // (Standar kompatibilitas Zalith Launcher 2 untuk mencegah crash parsing shader Mojang & Iris)
        if (targetRenderer.id != "gl4es" && targetRenderer.rendererId != "opengles2") {
            result["allow_glsl_extension_directive_midshader"] = "true"
            result["allow_higher_compat_version"] = "true"
            result["force_glsl_extensions_warn"] = "true"
        }

        return result
    }
}
