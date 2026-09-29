package com.israadev.nuxlauncher.core.settings

import android.app.ActivityManager
import android.content.Context
import com.google.gson.Gson
import com.israadev.nuxlauncher.core.models.LauncherSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

object SettingsManager {
    private val gson = Gson()
    private val _settings = MutableStateFlow(LauncherSettings())
    val settings: StateFlow<LauncherSettings> = _settings.asStateFlow()

    private fun getSettingsFile(context: Context): File {
        return File(context.filesDir, "settings.json")
    }

    fun init(context: Context) {
        val file = getSettingsFile(context)
        if (file.exists()) {
            try {
                val json = file.readText()
                val loaded = gson.fromJson(json, LauncherSettings::class.java)
                if (loaded != null) {
                    _settings.value = loaded
                    return
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // Recommend default RAM based on device's capacity (safe default ~ 1/3 to 1/2 of RAM)
        val totalRam = getTotalDeviceMemoryMb(context)
        val defaultRam = when {
            totalRam >= 12000 -> 4096
            totalRam >= 8000 -> 3072
            totalRam >= 6000 -> 2560
            totalRam >= 4000 -> 2048
            else -> 1280
        }
        val initialSettings = LauncherSettings(ramMb = defaultRam)
        _settings.value = initialSettings
        save(context)
    }

    fun updateSettings(context: Context, newSettings: LauncherSettings) {
        _settings.value = newSettings
        save(context)
    }

    fun resetToDefaults(context: Context) {
        val totalRam = getTotalDeviceMemoryMb(context)
        val defaultRam = when {
            totalRam >= 8000 -> 3072
            totalRam >= 4000 -> 2048
            else -> 1280
        }
        val defaultSettings = LauncherSettings(ramMb = defaultRam)
        _settings.value = defaultSettings
        save(context)
    }

    private fun save(context: Context) {
        try {
            val file = getSettingsFile(context)
            file.writeText(gson.toJson(_settings.value))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getTotalDeviceMemoryMb(context: Context): Int {
        return try {
            val actManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager ?: return 4096
            val memInfo = ActivityManager.MemoryInfo()
            actManager.getMemoryInfo(memInfo)
            (memInfo.totalMem / (1024 * 1024)).toInt()
        } catch (_: Throwable) {
            4096
        }
    }

    fun getAvailableDeviceMemoryMb(context: Context): Int {
        return try {
            val actManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager ?: return 2048
            val memInfo = ActivityManager.MemoryInfo()
            actManager.getMemoryInfo(memInfo)
            (memInfo.availMem / (1024 * 1024)).toInt()
        } catch (_: Throwable) {
            2048
        }
    }

    fun getCpuCoreCount(): Int {
        return Runtime.getRuntime().availableProcessors()
    }
}
