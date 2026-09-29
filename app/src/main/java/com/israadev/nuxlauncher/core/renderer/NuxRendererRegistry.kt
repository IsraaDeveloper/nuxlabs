package com.israadev.nuxlauncher.core.renderer

import com.israadev.nuxlauncher.core.utils.NuxVersionUtils
import java.io.File

object NuxRendererRegistry {

    val RENDERER_AUTO = NuxRendererInfo(
        id = "auto",
        displayName = "Auto (Rekomendasi)",
        badge = "Otomatis Cerdas",
        summary = "Otomatis memilih renderer paling optimal & stabil berdasarkan versi Minecraft yang dijalankan.",
        compatibility = "Semua Versi Minecraft",
        rendererId = "auto",
        libraryName = "auto"
    )

    val RENDERER_KRYPTON = NuxRendererInfo(
        id = "krypton",
        displayName = "Krypton (Next-Gen GL4ES)",
        badge = "Rekomendasi Utama (Mali & Adreno)",
        summary = "Wrapper modern OpenGL ES 3.x langsung di atas driver resmi sistem. Paling stabil dan FPS maksimal untuk GPU Mali & Adreno di MC modern (1.17 – 26.3).",
        compatibility = "Minecraft 1.17 – 26.3-snapshot-4",
        rendererId = "opengles3",
        libraryName = "libng_gl4es.so",
        isMaliRecommended = true,
        isAdrenoRecommended = true,
        minMCVersion = "1.17",
        maxMCVersion = "26.3-snapshot-4",
        envVariables = mapOf(
            "LIBGL_USE_MC_COLOR" to "1",
            "LIBGL_GL" to "31",
            "LIBGL_ES" to "3",
            "LIBGL_NORMALIZE" to "1",
            "LIBGL_NOERROR" to "1"
        )
    )

    val RENDERER_MOBILEGLUES = NuxRendererInfo(
        id = "mobileglues",
        displayName = "MobileGlues (OpenGL 4.0)",
        badge = "FCL Compatibility Layer",
        summary = "Pustaka grafis OpenGL 4.0 MobileGlues untuk Minecraft 1.17 – 1.21.1. Kompatibilitas tinggi dengan berbagai GPU mobile termasuk Mali.",
        compatibility = "Minecraft 1.17 – 1.21.1",
        rendererId = "opengles3",
        libraryName = "libmobileglues.so",
        eglName = "libmobileglues.so",
        isMaliRecommended = true,
        minMCVersion = "1.17",
        maxMCVersion = "1.21.1",
        envVariables = mapOf(
            "LIBGL_ES" to "3",
            "POJAVEXEC_EGL" to "libmobileglues.so",
            "LIBGL_EGL" to "libmobileglues.so",
            "MG_COUNT_LAUNCH" to "1",
            "SDL_OPENGL_FORCE_SRGB_FRAMEBUFFER" to "0"
        )
    )

    val RENDERER_GL4ES = NuxRendererInfo(
        id = "gl4es",
        displayName = "GL4ES 1.1.4",
        badge = "Paling Stabil untuk MC Lama",
        summary = "Wrapper OpenGL ES 2.0 klasik. Kompatibel untuk Minecraft 1.7.10 – 1.16.5 dan perangkat spek rendah.",
        compatibility = "Minecraft 1.7.10 – 1.16.5",
        rendererId = "opengles2",
        libraryName = "libgl4es_114.so",
        minMCVersion = "1.7.2",
        maxMCVersion = "1.16.5",
        envVariables = mapOf(
            "LIBGL_ES" to "2",
            "LIBGL_MIPMAP" to "3",
            "LIBGL_NOERROR" to "1",
            "LIBGL_NOINTOVLHACK" to "1",
            "LIBGL_NORMALIZE" to "1"
        )
    )

    val RENDERER_ZINK = NuxRendererInfo(
        id = "zink",
        displayName = "Kopper Zink (Desktop GL 4.6)",
        badge = "Hardware Vulkan",
        summary = "Menerjemahkan OpenGL desktop 4.6 ke Vulkan. Mendukung mod shader modern (Iris/Oculus) & mod tingkat lanjut.",
        compatibility = "Minecraft 1.14 – 26.3",
        rendererId = "opengles3_desktopgl_zink_kopper",
        libraryName = "libglxshim.so",
        eglName = "libEGL_mesa.so",
        requiresVulkan = true,
        minMCVersion = "1.14",
        maxMCVersion = "26.3",
        envVariables = mapOf(
            "LIBGL_ES" to "3",
            "MESA_LOADER_DRIVER_OVERRIDE" to "zink",
            "MESA_GL_VERSION_OVERRIDE" to "4.6",
            "MESA_GLSL_VERSION_OVERRIDE" to "460",
            "force_glsl_extensions_warn" to "true",
            "allow_higher_compat_version" to "true",
            "allow_glsl_extension_directive_midshader" to "true",
            "LIB_MESA_NAME" to "libglxshim.so"
        )
    )

    val RENDERER_FREEDRENO = NuxRendererInfo(
        id = "freedreno",
        displayName = "Freedreno (Snapdragon Turnip)",
        badge = "Khusus GPU Adreno",
        summary = "Driver Mesa Gallium Freedreno open-source dioptimalkan khusus untuk chipset Snapdragon / GPU Adreno.",
        compatibility = "Minecraft 1.7.10 – 26.3",
        rendererId = "gallium_freedreno",
        libraryName = "libOSMesa_8.so",
        isAdrenoRecommended = true,
        minMCVersion = "1.7.2",
        maxMCVersion = "26.3",
        envVariables = mapOf(
            "MESA_LOADER_DRIVER_OVERRIDE" to "freedreno",
            "MESA_GL_VERSION_OVERRIDE" to "4.6",
            "MESA_GLSL_VERSION_OVERRIDE" to "460",
            "LIB_MESA_NAME" to "libOSMesa_8.so"
        )
    )

    val RENDERER_VIRGL = NuxRendererInfo(
        id = "virgl",
        displayName = "VirGLRenderer",
        badge = "Virtual Server",
        summary = "Menerjemahkan panggilan grafis melalui IPC virtual test server untuk rendering terisolasi.",
        compatibility = "Minecraft 1.7.10 – 26.3",
        rendererId = "gallium_virgl",
        libraryName = "libOSMesa_2121.so",
        minMCVersion = "1.7.2",
        maxMCVersion = "26.3",
        envVariables = mapOf(
            "MESA_LOADER_DRIVER_OVERRIDE" to "virgl",
            "MESA_GL_VERSION_OVERRIDE" to "4.6",
            "MESA_GLSL_VERSION_OVERRIDE" to "460",
            "LIB_MESA_NAME" to "libOSMesa_2121.so"
        )
    )

    val RENDERER_PANFROST = NuxRendererInfo(
        id = "panfrost",
        displayName = "Panfrost (Mali GPU)",
        badge = "Eksperimental Mali",
        summary = "Driver Mesa Gallium Panfrost. Perhatian: Sebagian besar kernel Android vendor (seperti MediaTek) menolak alokasi JIT ioctl driver ini dan menyebabkan force-close. Jika mental, gunakan Krypton atau MobileGlues!",
        compatibility = "Minecraft 1.7.10 – 1.16.5 (Eksperimental)",
        rendererId = "gallium_panfrost",
        libraryName = "libOSMesa_2300d.so",
        isMaliRecommended = false,
        minMCVersion = "1.7.2",
        maxMCVersion = "1.16.5",
        envVariables = mapOf(
            "MESA_LOADER_DRIVER_OVERRIDE" to "panfrost",
            "MESA_GL_VERSION_OVERRIDE" to "3.3",
            "MESA_GLSL_VERSION_OVERRIDE" to "330",
            "LIB_MESA_NAME" to "libOSMesa_2300d.so"
        )
    )

    private val baseRenderers: List<NuxRendererInfo> = listOf(
        RENDERER_AUTO,
        RENDERER_KRYPTON,
        RENDERER_MOBILEGLUES,
        RENDERER_GL4ES,
        RENDERER_ZINK,
        RENDERER_FREEDRENO,
        RENDERER_VIRGL,
        RENDERER_PANFROST
    )

    private val pluginRenderers = mutableListOf<NuxRendererInfo>()

    fun setPluginRenderers(plugins: List<NuxRendererInfo>) {
        synchronized(pluginRenderers) {
            pluginRenderers.clear()
            pluginRenderers.addAll(plugins)
        }
    }

    /**
     * Seluruh daftar renderer yang dapat dipilih oleh user di Settings (bawaan + plugin eksternal).
     */
    val availableRenderers: List<NuxRendererInfo>
        get() = synchronized(pluginRenderers) {
            baseRenderers + pluginRenderers
        }

    /**
     * Menemukan renderer berdasarkan ID
     */
    fun findRendererById(id: String): NuxRendererInfo {
        return availableRenderers.find { 
            it.id.equals(id, ignoreCase = true) || 
            (it.isPlugin && it.pluginPackageName.equals(id.removePrefix("plugin_"), ignoreCase = true))
        } ?: RENDERER_AUTO
    }

    /**
     * Memeriksa apakah renderer yang dipilih kompatibel dengan versi Minecraft yang akan dimainkan.
     */
    fun isSupportedForVersion(renderer: NuxRendererInfo, mcVersion: String): Boolean {
        return NuxVersionUtils.isRendererSupported(renderer, mcVersion)
    }

    /**
     * Menyelesaikan konfigurasi renderer aktual yang akan dijalankan di game engine.
     * Jika "auto" dipilih, akan memilih Krypton untuk MC 1.17+ atau GL4ES untuk versi di bawahnya.
     */
    fun resolveRenderer(
        selectedId: String,
        mcVersion: String,
        nativeLibDir: File,
        context: android.content.Context? = null
    ): NuxRendererInfo {
        if (context != null && pluginRenderers.isEmpty()) {
            NuxRendererPluginManager.scanPlugins(context)
        }

        val hasNgGl4es = File(nativeLibDir, "libng_gl4es.so").exists()
        val hasMobileGlues = File(nativeLibDir, "libmobileglues.so").exists()
        val hasGl4es = File(nativeLibDir, "libgl4es_114.so").exists()

        // 1. Direct match in availableRenderers (built-in or scanned plugins)
        val requested = findRendererById(selectedId)
        if (requested.isPlugin) {
            return requested
        }

        // 2. If user selected by plugin ID pattern or package name
        if (selectedId.startsWith("plugin_") || selectedId.contains("com.fcl.plugin") || selectedId.contains("plugin")) {
            val pkg = selectedId.removePrefix("plugin_")
            val matchedPlugin = pluginRenderers.find {
                it.id.equals(selectedId, ignoreCase = true) ||
                it.pluginPackageName.equals(pkg, ignoreCase = true) ||
                it.pluginPackageName?.contains(pkg, ignoreCase = true) == true
            }
            if (matchedPlugin != null) {
                return matchedPlugin
            }
        }

        // 3. If selectedId is "mobileglues" or mentions mobileglue
        if (selectedId.contains("mobileglue", ignoreCase = true)) {
            if (hasMobileGlues) {
                return RENDERER_MOBILEGLUES
            }
            // Auto-fallback to external MobileGlues plugin if installed
            val pluginMg = pluginRenderers.find {
                it.id.contains("mobileglue", ignoreCase = true) ||
                it.displayName.contains("mobileglue", ignoreCase = true) ||
                it.libraryName.contains("mobileglue", ignoreCase = true) ||
                it.pluginPackageName?.contains("mobileglue", ignoreCase = true) == true
            }
            if (pluginMg != null) {
                return pluginMg
            }
        }

        // 4. Auto mode
        if (selectedId.equals("auto", ignoreCase = true)) {
            val isModernMc = isModernMinecraft(mcVersion)
            return if (isModernMc && hasNgGl4es) {
                RENDERER_KRYPTON
            } else if (isModernMc && (hasMobileGlues || pluginRenderers.any { it.libraryName.contains("mobileglue", ignoreCase = true) })) {
                pluginRenderers.find { it.libraryName.contains("mobileglue", ignoreCase = true) } ?: RENDERER_MOBILEGLUES
            } else if (hasGl4es) {
                RENDERER_GL4ES
            } else if (hasNgGl4es) {
                RENDERER_KRYPTON
            } else {
                RENDERER_GL4ES
            }
        }

        // 5. If requested target .so exists in nativeLibDir
        val targetSo = File(nativeLibDir, requested.libraryName)
        if (targetSo.exists()) {
            return requested
        }

        // 6. Fallback aman jika library target tidak ada di nativeLibDir
        return if (hasNgGl4es) RENDERER_KRYPTON else if (hasMobileGlues) RENDERER_MOBILEGLUES else RENDERER_GL4ES
    }

    /**
     * Deteksi apakah versi Minecraft adalah 1.17 ke atas
     */
    fun isModernMinecraft(version: String): Boolean {
        if (version.isBlank()) return true
        val clean = version.trim().lowercase()
        // Cek snapshot modern (24w..., 25w..., 26w...)
        if (clean.matches(Regex("""^\d{2}w\d{2}.*"""))) return true

        // Ekstrak angka versi mayor/minor dari 1.X.Y
        val match = Regex("""^1\.(\d+)""").find(clean)
        if (match != null) {
            val minor = match.groupValues[1].toIntOrNull() ?: 17
            return minor >= 17
        }
        return true
    }
}
