package com.israadev.nuxlauncher.core.runtime

import android.content.Context
import android.os.Build
import android.system.Os
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.compressors.xz.XZCompressorInputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

object JavaRuntimeManager {

    fun getDeviceArch(): String {
        val abi = Build.SUPPORTED_ABIS.firstOrNull() ?: "arm64-v8a"
        return when {
            abi.contains("arm64") -> "arm64"
            abi.contains("armeabi") || abi.contains("arm") -> "arm"
            abi.contains("x86_64") -> "x86_64"
            abi.contains("x86") -> "x86"
            else -> "arm64"
        }
    }

    fun getRuntimesDir(context: Context): File {
        val dir = File(context.filesDir, "runtimes")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun getRuntimeHome(context: Context, runtimeName: String): File {
        return File(getRuntimesDir(context), runtimeName)
    }

    fun isRuntimeInstalled(context: Context, runtimeName: String): Boolean {
        val home = getRuntimeHome(context, runtimeName)
        val javaBin = File(home, "bin/java")
        val releaseFile = File(home, "release")
        return home.exists() && (javaBin.exists() || releaseFile.exists())
    }

    /**
     * Resolves recommended OpenJDK runtime based on Mojang manifest metadata or Minecraft version prefix.
     * Matches NUX Launcher Windows logic identically.
     */
    fun getRecommendedRuntime(mcVersion: String, javaMajorVersion: Int? = null): String {
        if (javaMajorVersion != null) {
            return when {
                javaMajorVersion >= 25 -> "jre-25"
                javaMajorVersion >= 21 -> "jre-21"
                javaMajorVersion >= 17 -> "jre-17"
                else -> "jre-8"
            }
        }

        return when {
            mcVersion.startsWith("26.") -> "jre-25"
            mcVersion.startsWith("1.21") || mcVersion.startsWith("1.20.5") || mcVersion.startsWith("1.20.6") -> "jre-21"
            mcVersion.startsWith("1.20") || mcVersion.startsWith("1.19") || mcVersion.startsWith("1.18") || mcVersion.startsWith("1.17") -> "jre-17"
            else -> "jre-8"
        }
    }

    fun getRuntimeDisplayName(runtimeName: String): String {
        return when (runtimeName) {
            "jre-8" -> "Java 8 (Auto)"
            "jre-17" -> "Java 17 (Auto)"
            "jre-21" -> "Java 21 (Auto)"
            "jre-25" -> "Java 25 (Auto)"
            else -> runtimeName.uppercase()
        }
    }

    fun getJavaExecutable(context: Context, runtimeName: String): File {
        return File(getRuntimeHome(context, runtimeName), "bin/java")
    }

    suspend fun extractRuntime(
        context: Context,
        runtimeName: String,
        onProgress: (String) -> Unit = {}
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            val destDir = getRuntimeHome(context, runtimeName)
            if (isRuntimeInstalled(context, runtimeName)) {
                return@withContext Result.success(destDir)
            }

            destDir.mkdirs()
            val arch = getDeviceArch()
            val assetPath = "runtimes/$runtimeName"

            // 1. Unpack universal.tar.xz
            onProgress("Mengekstrak Java Runtime ($runtimeName universal)...")
            val universalName = "$assetPath/universal.tar.xz"
            try {
                context.assets.open(universalName).use { input ->
                    unpackTarXz(input, destDir)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            // 2. Unpack bin-$arch.tar.xz
            onProgress("Mengekstrak Java Runtime ($runtimeName bin-$arch)...")
            val binName = "$assetPath/bin-$arch.tar.xz"
            try {
                context.assets.open(binName).use { input ->
                    unpackTarXz(input, destDir)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            // 3. Mark executables
            val javaBin = File(destDir, "bin/java")
            if (javaBin.exists()) {
                javaBin.setExecutable(true, false)
                javaBin.setReadable(true, false)
            }

            File(destDir, "bin").listFiles()?.forEach { bin ->
                bin.setExecutable(true, false)
            }

            Result.success(destDir)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun unpackTarXz(inputStream: InputStream, destDir: File) {
        TarArchiveInputStream(XZCompressorInputStream(inputStream)).use { tarIn ->
            val buffer = ByteArray(8192)
            var entry = tarIn.nextEntry
            while (entry != null) {
                val targetFile = File(destDir, entry.name)

                if (entry.isSymbolicLink) {
                    try {
                        if (targetFile.exists()) targetFile.delete()
                        Os.symlink(entry.linkName, targetFile.absolutePath)
                    } catch (_: Throwable) {}
                } else if (entry.isDirectory) {
                    targetFile.mkdirs()
                } else {
                    targetFile.parentFile?.mkdirs()
                    FileOutputStream(targetFile).use { out ->
                        var len: Int
                        while (tarIn.read(buffer).also { len = it } != -1) {
                            out.write(buffer, 0, len)
                        }
                    }
                }
                entry = tarIn.nextEntry
            }
        }
    }
}
