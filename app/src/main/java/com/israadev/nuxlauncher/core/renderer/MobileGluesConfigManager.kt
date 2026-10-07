package com.israadev.nuxlauncher.core.renderer

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Environment
import android.util.Log
import org.json.JSONObject
import java.io.File

data class MobileGluesConfig(
    var enableANGLE: Boolean = false,
    var enableNoError: Boolean = true,
    var angleDepthClearFixMode: Boolean = false,
    var enableExtTimerQuery: Boolean = false,
    var enableExtComputeShader: Boolean = true,
    var enableExtDirectStateAccess: Boolean = true,
    var fsr1Setting: Int = 0,
    var maxGlslCacheSize: Int = 16,
    var customGLVersion: String = "4.0.0",
    var multidrawOrderArrays: String = "unroll",
    var multidrawOrderElements: String = "unroll,indirect",
    var multidrawOrderElementsBaseVertex: String = "basevertex,indirect,compute,unroll",
    var multidrawOrderArraysIndirect: String = "indirect",
    var multidrawOrderElementsIndirect: String = "indirect"
)

object MobileGluesConfigManager {
    private const val TAG = "MobileGluesConfig"
    private const val MG_DIR = "MG"
    private const val CONFIG_FILE = "config.json"

    private fun getConfigFile(): File {
        val sdcard = Environment.getExternalStorageDirectory()
        val dir = File(sdcard, MG_DIR)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return File(dir, CONFIG_FILE)
    }

    fun loadConfig(): MobileGluesConfig {
        val config = MobileGluesConfig()
        val file = getConfigFile()
        if (!file.exists() || !file.canRead()) {
            return config
        }

        try {
            val jsonText = file.readText()
            val obj = JSONObject(jsonText)

            if (obj.has("enableANGLE")) {
                config.enableANGLE = obj.optInt("enableANGLE", 1) != 0
            } else if (obj.has("enableAngle")) {
                config.enableANGLE = obj.optInt("enableAngle", 1) != 0
            }
            if (obj.has("enableNoError")) {
                config.enableNoError = obj.optInt("enableNoError", 3) != 0
            } else if (obj.has("ignoreError")) {
                config.enableNoError = obj.optInt("ignoreError", 1) != 0
            }
            if (obj.has("angleDepthClearFixMode")) {
                config.angleDepthClearFixMode = obj.optInt("angleDepthClearFixMode", 0) != 0
            }
            if (obj.has("enableExtTimerQuery")) {
                config.enableExtTimerQuery = obj.optInt("enableExtTimerQuery", 0) != 0
            }
            if (obj.has("enableExtComputeShader")) {
                config.enableExtComputeShader = obj.optInt("enableExtComputeShader", 1) != 0
            }
            if (obj.has("enableExtDirectStateAccess")) {
                config.enableExtDirectStateAccess = obj.optInt("enableExtDirectStateAccess", 1) != 0
            }
            if (obj.has("fsr1Setting")) {
                config.fsr1Setting = obj.optInt("fsr1Setting", 0)
            }
            if (obj.has("maxGlslCacheSize")) {
                config.maxGlslCacheSize = obj.optInt("maxGlslCacheSize", 121)
            }
            if (obj.has("customGLVersion")) {
                val rawVer = obj.optString("customGLVersion", "4.5")
                config.customGLVersion = if (rawVer == "0" || rawVer.isBlank()) "4.5" else rawVer
            }
            if (obj.has("multidrawOrderArrays")) {
                config.multidrawOrderArrays = obj.optString("multidrawOrderArrays", config.multidrawOrderArrays)
            }
            if (obj.has("multidrawOrderElements")) {
                config.multidrawOrderElements = obj.optString("multidrawOrderElements", config.multidrawOrderElements)
            }
            if (obj.has("multidrawOrderElementsBaseVertex")) {
                config.multidrawOrderElementsBaseVertex = obj.optString("multidrawOrderElementsBaseVertex", config.multidrawOrderElementsBaseVertex)
            }
            if (obj.has("multidrawOrderArraysIndirect")) {
                config.multidrawOrderArraysIndirect = obj.optString("multidrawOrderArraysIndirect", config.multidrawOrderArraysIndirect)
            }
            if (obj.has("multidrawOrderElementsIndirect")) {
                config.multidrawOrderElementsIndirect = obj.optString("multidrawOrderElementsIndirect", config.multidrawOrderElementsIndirect)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error reading /sdcard/MG/config.json", e)
        }

        return config
    }

    fun saveConfig(config: MobileGluesConfig): Boolean {
        return try {
            val file = getConfigFile()
            val obj = JSONObject()

            obj.put("enableANGLE", if (config.enableANGLE) 1 else 0)
            obj.put("enableAngle", if (config.enableANGLE) 1 else 0)
            obj.put("enableNoError", if (config.enableNoError) 3 else 0)
            obj.put("ignoreError", if (config.enableNoError) 1 else 0)
            obj.put("angleDepthClearFixMode", if (config.angleDepthClearFixMode) 1 else 0)
            obj.put("enableExtTimerQuery", if (config.enableExtTimerQuery) 1 else 0)
            obj.put("enableExtComputeShader", if (config.enableExtComputeShader) 1 else 0)
            obj.put("enableExtDirectStateAccess", if (config.enableExtDirectStateAccess) 1 else 0)
            obj.put("fsr1Setting", config.fsr1Setting)
            obj.put("maxGlslCacheSize", config.maxGlslCacheSize)
            obj.put("customGLVersion", config.customGLVersion)
            obj.put("multidrawOrderArrays", config.multidrawOrderArrays)
            obj.put("multidrawOrderElements", config.multidrawOrderElements)
            obj.put("multidrawOrderElementsBaseVertex", config.multidrawOrderElementsBaseVertex)
            obj.put("multidrawOrderArraysIndirect", config.multidrawOrderArraysIndirect)
            obj.put("multidrawOrderElementsIndirect", config.multidrawOrderElementsIndirect)

            file.writeText(obj.toString())
            Log.i(TAG, "Saved MobileGlues config to ${file.absolutePath}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to write /sdcard/MG/config.json", e)
            false
        }
    }

    fun isMobileGluesInstalled(context: Context): Boolean {
        return try {
            context.packageManager.getPackageInfo("com.fcl.plugin.mobileglues", 0)
            true
        } catch (_: Exception) {
            false
        }
    }

    fun openMobileGluesApp(context: Context): Boolean {
        return try {
            val pm = context.packageManager
            val intent = pm.getLaunchIntentForPackage("com.fcl.plugin.mobileglues")
                ?: Intent().apply {
                    component = ComponentName("com.fcl.plugin.mobileglues", "com.fcl.plugin.mobileglues.MainActivity")
                }
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch MobileGlues activity", e)
            false
        }
    }

    fun ensureConfigReady() {
        try {
            val file = getConfigFile()
            if (!file.exists()) {
                val defaultCfg = MobileGluesConfig()
                saveConfig(defaultCfg)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to ensure MobileGlues config ready", e)
        }
    }
}

