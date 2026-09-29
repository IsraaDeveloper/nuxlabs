package com.israadev.nuxlauncher.core.launch

import android.content.Context
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

object LwjglManager {

    fun getLwjglVersion(mcVersion: String, versionJsonFile: File? = null): String {
        if (versionJsonFile != null && versionJsonFile.exists()) {
            try {
                val content = versionJsonFile.readText()
                if (content.contains("org.lwjgl:lwjgl:3.4.") || content.contains("org.lwjgl.lwjgl:lwjgl:3.4.") || content.contains("3.4.1")) {
                    return "3.4.1"
                }
            } catch (_: Exception) {}
        }
        if (mcVersion.startsWith("26.") || mcVersion.startsWith("25w") || mcVersion.startsWith("24w") || mcVersion.contains("snapshot")) {
            return "3.4.1"
        }
        return "3.3.3"
    }

    fun prepareLauncherComponents(context: Context): File? {
        val launcherDir = File(context.filesDir, "components/launcher")
        launcherDir.mkdirs()
        val patcher = File(launcherDir, "MioLibPatcher.jar")
        try {
            context.assets.open("components/launcher/MioLibPatcher.jar").use { input ->
                val assetLen = input.available().toLong()
                if (!patcher.exists() || patcher.length() != assetLen) {
                    FileOutputStream(patcher).use { output ->
                        input.copyTo(output)
                    }
                }
            }
        } catch (_: Exception) {}
        prepareLaunchWrapper(context)
        return patcher
    }

    fun prepareLaunchWrapper(context: Context): File? {
        val launcherDir = File(context.filesDir, "components/launcher")
        launcherDir.mkdirs()
        val wrapper = File(launcherDir, "MioLaunchWrapper.jar")
        try {
            context.assets.open("components/launcher/MioLaunchWrapper.jar").use { input ->
                val assetLen = input.available().toLong()
                if (!wrapper.exists() || wrapper.length() != assetLen) {
                    FileOutputStream(wrapper).use { output ->
                        input.copyTo(output)
                    }
                }
            }
        } catch (_: Exception) {
            return null
        }
        return wrapper
    }

    fun prepareAuthLibInjector(context: Context): File? {
        val authDir = File(context.filesDir, "components/auth_libs")
        authDir.mkdirs()
        val jar = File(authDir, "authlib-injector.jar")
        if (!jar.exists() || jar.length() == 0L) {
            try {
                context.assets.open("components/auth_libs/authlib-injector.jar").use { input ->
                    FileOutputStream(jar).use { output ->
                        input.copyTo(output)
                    }
                }
            } catch (_: Exception) {
                return null
            }
        }
        return jar
    }

    fun prepareLwjgl(context: Context, lwjglVersion: String = "3.3.3"): File {
        val targetDir = File(context.filesDir, "components/lwjgl/$lwjglVersion")
        val nativesDir = File(targetDir, "natives")

        val marker = File(targetDir, ".extracted")
        if (marker.exists()) {
            ensureSpirvCross(context, nativesDir)
            return nativesDir
        }

        targetDir.mkdirs()
        nativesDir.mkdirs()

        // Extract jars and natives from assets/app_runtime/lwjgl/$lwjglVersion
        val assetBasePath = "app_runtime/lwjgl/$lwjglVersion"
        copyAssetFolder(context, assetBasePath, targetDir)

        ensureSpirvCross(context, nativesDir)

        marker.createNewFile()
        return nativesDir
    }

    fun getLwjglJars(context: Context, lwjglVersion: String = "3.3.3"): List<File> {
        val targetDir = File(context.filesDir, "components/lwjgl/$lwjglVersion")
        if (!targetDir.exists()) {
            prepareLwjgl(context, lwjglVersion)
        }
        val isLwjgl2 = lwjglVersion.startsWith("2.")
        val jars = targetDir.listFiles { file ->
            file.isFile && file.name.endsWith(".jar") && (isLwjgl2 || file.name != "lwjgl-lwjglx.jar")
        } ?: emptyArray()
        return jars.sortedWith(Comparator { a, b ->
            when {
                a.name == "lwjgl.jar" -> -1
                b.name == "lwjgl.jar" -> 1
                a.name.contains("merged") -> -1
                b.name.contains("merged") -> 1
                else -> a.name.compareTo(b.name)
            }
        })
    }

    fun getNativesDir(context: Context, lwjglVersion: String = "3.3.3"): File {
        val dir = File(context.filesDir, "components/lwjgl/$lwjglVersion/natives")
        if (!dir.exists() || dir.listFiles()?.isEmpty() == true) {
            prepareLwjgl(context, lwjglVersion)
        } else {
            ensureSpirvCross(context, dir)
        }
        return dir
    }

    fun ensureSpirvCross(context: Context, nativesDir: File) {
        try {
            val nativeLibDir = File(context.applicationInfo.nativeLibraryDir)
            val spirvSrc = File(nativeLibDir, "libspirv-cross-c-shared.so")
            if (spirvSrc.exists()) {
                val targets = listOf("libspirv-cross.so", "libspirv-cross-c-shared.so")
                for (name in targets) {
                    val dest = File(nativesDir, name)
                    if (!dest.exists() || dest.length() == 0L) {
                        try {
                            android.system.Os.symlink(spirvSrc.absolutePath, dest.absolutePath)
                        } catch (_: Throwable) {
                            try {
                                spirvSrc.copyTo(dest, overwrite = true)
                            } catch (_: Throwable) {}
                        }
                    }
                }
            }
        } catch (_: Throwable) {}
    }

    private fun copyAssetFolder(context: Context, assetFolder: String, targetFolder: File) {
        val assets = context.assets.list(assetFolder) ?: return
        for (item in assets) {
            val subAssetPath = "$assetFolder/$item"
            val subAssets = context.assets.list(subAssetPath)
            if (subAssets != null && subAssets.isNotEmpty()) {
                val subTarget = File(targetFolder, item)
                subTarget.mkdirs()
                copyAssetFolder(context, subAssetPath, subTarget)
            } else {
                val destFile = File(targetFolder, item)
                try {
                    context.assets.open(subAssetPath).use { input ->
                        FileOutputStream(destFile).use { output ->
                            input.copyTo(output)
                        }
                    }
                } catch (e: Exception) {
                    // Try directory if open failed
                    val subTarget = File(targetFolder, item)
                    subTarget.mkdirs()
                    copyAssetFolder(context, subAssetPath, subTarget)
                }
            }
        }
    }
}
