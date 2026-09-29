package com.israadev.nuxlauncher.core.game

import android.content.Context
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.ConcurrentHashMap

private const val TAG = "MCOptions"

object MCOptions {
    private val lock = Any()
    private val parameterMap = ConcurrentHashMap<String, String>()
    private var optionsFile: File? = null

    fun setup(context: Context, gameDir: File) {
        synchronized(lock) {
            parameterMap.clear()
            val target = File(gameDir, "options.txt")
            optionsFile = target

            target.parentFile?.takeIf { !it.exists() }?.mkdirs()
            if (!target.exists()) {
                createWithDefaults(context, target)
            }

            loadInternal(target)
        }
    }

    private fun createWithDefaults(context: Context, target: File) {
        runCatching {
            context.assets.open("game/options.txt").use { input ->
                FileOutputStream(target).use { output ->
                    input.copyTo(output)
                }
            }
            Log.d(TAG, "Successfully extracted default options.txt to ${target.absolutePath}")
        }.onFailure {
            Log.w(TAG, "Failed to unpack options.txt from assets: ${it.message}")
        }
    }

    private fun loadInternal(file: File) {
        if (!file.exists()) return
        runCatching {
            val newMap = file.readLines()
                .mapNotNull { line ->
                    val trimmed = line.trim()
                    val idx = trimmed.indexOf(':')
                    if (idx > 0) {
                        trimmed.substring(0, idx).trim() to trimmed.substring(idx + 1).trim()
                    } else null
                }.toMap()

            parameterMap.clear()
            parameterMap.putAll(newMap)
            Log.d(TAG, "Loaded ${parameterMap.size} options from ${file.name}")
        }.onFailure {
            Log.w(TAG, "Failed to parse options.txt: ${it.message}")
        }
    }

    fun get(key: String): String? = parameterMap[key]

    fun set(key: String, value: String) {
        parameterMap[key] = value
    }

    fun containsKey(key: String): Boolean = parameterMap.containsKey(key)

    fun save() {
        val file = optionsFile ?: return
        synchronized(lock) {
            val tempFile = File(file.parentFile, "${file.name}.tmp")
            runCatching {
                tempFile.bufferedWriter().use { writer ->
                    parameterMap.forEach { (k, v) ->
                        writer.write("$k:$v")
                        writer.newLine()
                    }
                }
                if (file.exists()) file.delete()
                tempFile.renameTo(file)
                Log.d(TAG, "Saved options.txt successfully (${parameterMap.size} keys)")
            }.onFailure {
                Log.e(TAG, "Failed to save options.txt: ${it.message}")
                tempFile.delete()
            }
        }
    }
}
