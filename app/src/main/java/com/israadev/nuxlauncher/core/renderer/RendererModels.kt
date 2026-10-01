package com.israadev.nuxlauncher.core.renderer

/**
 * Metadata and launch configuration for a graphics renderer.
 */
data class NuxRendererInfo(
    val id: String,
    val displayName: String,
    val badge: String,
    val summary: String,
    val compatibility: String,
    val rendererId: String,
    val libraryName: String,
    val eglName: String? = null,
    val envVariables: Map<String, String> = emptyMap(),
    val requiresVulkan: Boolean = false,
    val isAdrenoRecommended: Boolean = false,
    val isMaliRecommended: Boolean = false,
    val isPlugin: Boolean = false,
    val pluginPackageName: String? = null,
    val pluginNativePath: String? = null,
    val dlopenLibs: List<String> = emptyList(),
    val minMCVersion: String? = null,
    val maxMCVersion: String? = null
) {
    val isConfigurable: Boolean
        get() {
            // MobileGlues (baik bawaan maupun plugin) bukan renderer yang memiliki panel setting gear di Zalith
            if (id == "mobileglues" || 
                id.contains("mobileglue", ignoreCase = true) || 
                pluginPackageName == "com.fcl.plugin.mobileglues" || 
                displayName.contains("MobileGlues", ignoreCase = true)) {
                return false
            }

            // MobileGL (top.mobilegl.plugin atau nama MobileGL) adalah configurable persis Zalith
            if (pluginPackageName == "top.mobilegl.plugin" || 
                id == "mobilegl" ||
                id == "plugin_top.mobilegl.plugin" ||
                displayName.equals("MobileGL", ignoreCase = true) ||
                (displayName.contains("MobileGL", ignoreCase = true) && !displayName.contains("MobileGlues", ignoreCase = true))) {
                return true
            }

            // Periksa apakah renderer terdaftar di NuxRendererV2Manager dan memiliki unit konfigurasi aktif
            return com.israadev.nuxlauncher.core.renderer.v2.NuxRendererV2Manager.isConfigurableRenderer(this)
        }
}
