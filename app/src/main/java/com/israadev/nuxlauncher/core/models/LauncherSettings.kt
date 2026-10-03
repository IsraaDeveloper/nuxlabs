package com.israadev.nuxlauncher.core.models

/**
 * Global Configuration for NUX Launcher (Java, RAM, Controls, Graphics)
 */
data class LauncherSettings(
    // JVM & RAM Memory
    val ramMb: Int = 1536,
    val initialHeapMb: Int = 256,
    val customJvmArgs: String = "",
    val defaultJavaRuntime: String = "auto",

    // In-game Mouse & Touch Controls (Moved from in-game HUD to Settings)
    val mouseControlMode: String = "SLIDE", // "SLIDE" (Trackpad) or "CLICK" (Direct Touch)
    val cursorSensitivity: Int = 100, // 50% - 300%
    val captureSensitivity: Int = 125, // 50% - 300% (Camera look speed)
    val mouseSizeDp: Int = 24, // 16 - 48 dp
    val hideMouseInClickMode: Boolean = true,
    val physicalMouseMode: Boolean = true, // Auto-hide virtual cursor & enable system cursor when physical mouse is used

    // Graphics & Performance
    val resolutionRatio: Int = 100, // 50% - 125%
    val autoOptimizeMinecraft: Boolean = true,
    val sustainedPerformanceMode: Boolean = false,
    val selectedRenderer: String = "auto", // "auto", "krypton", "mobileglues", "gl4es", "zink", "freedreno", "virgl", "panfrost"
    val vulkanDriver: String = "auto", // "auto", "system", "turnip", etc.
    val graphicsApi: String = "DEFAULT", // "DEFAULT", "OPENGL", "VULKAN"
    val zinkPreferSystemDriver: Boolean = false,
    val vsyncInZink: Boolean = false,

    // Hero Banner Animation (Video MP4)
    val heroAnimationEnabled: Boolean = false,
    val heroAnimationVideoPath: String = "",
    val heroAnimationRotation: Int = 0, // 0, 90, 180, 270

    // AI Crash Analytics (OpenRouter AI)
    val aiAutoAnalyze: Boolean = true,
    val aiApiKey: String = "",
    val aiModel: String = ""
)
