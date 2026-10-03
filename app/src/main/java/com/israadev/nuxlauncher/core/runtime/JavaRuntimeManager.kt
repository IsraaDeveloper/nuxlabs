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

    fun ensureExecutablePermissions(home: File) {
        if (!home.exists() || !home.isDirectory) return

        val javaBin = File(home, "bin/java")
        if (javaBin.exists()) {
            try {
                Os.chmod(javaBin.absolutePath, 493) // 0755: rwxr-xr-x
            } catch (_: Throwable) {}
            javaBin.setExecutable(true, false)
            javaBin.setReadable(true, false)
        }

        File(home, "bin").walkTopDown().forEach { file ->
            if (file.isFile) {
                try {
                    Os.chmod(file.absolutePath, 493) // 0755
                } catch (_: Throwable) {}
                file.setExecutable(true, false)
                file.setReadable(true, false)
            }
        }

        File(home, "lib").walkTopDown().forEach { file ->
            if (file.isFile && (file.extension == "so" || file.name.contains(".so"))) {
                try {
                    Os.chmod(file.absolutePath, 493) // 0755
                } catch (_: Throwable) {}
                file.setExecutable(true, false)
                file.setReadable(true, false)
            }
        }

        try {
            Runtime.getRuntime().exec(arrayOf("chmod", "-R", "755", home.absolutePath)).waitFor()
        } catch (_: Throwable) {}
    }

    fun isRuntimeInstalled(context: Context, runtimeName: String): Boolean {
        val home = getRuntimeHome(context, runtimeName)
        if (!home.exists() || !home.isDirectory) return false

        val permMarker = File(home, ".nux_perm_v2")
        if (!permMarker.exists()) return false

        val javaBin = File(home, "bin/java")
        if (!javaBin.exists() || javaBin.length() == 0L) return false
        ensureExecutablePermissions(home)

        // Verify libjli.so exists
        val hasJli = File(home, "lib/jli/libjli.so").exists() ||
                File(home, "lib/libjli.so").exists() ||
                home.walkTopDown().any { it.name == "libjli.so" }
        if (!hasJli) return false

        // Verify libjvm.so exists
        val hasJvm = File(home, "lib/server/libjvm.so").exists() ||
                File(home, "lib/client/libjvm.so").exists() ||
                home.walkTopDown().any { it.name == "libjvm.so" }
        if (!hasJvm) return false

        // Verify modules (Java 9+) or rt.jar (Java 8)
        val hasModulesOrRt = File(home, "lib/modules").exists() ||
                File(home, "lib/rt.jar").exists() ||
                home.walkTopDown().any { it.name == "rt.jar" }
        if (!hasModulesOrRt) return false

        return true
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
        onProgressString: (String) -> Unit
    ): Result<File> = extractRuntime(context, runtimeName) { _, msg -> onProgressString(msg) }

    suspend fun extractRuntime(
        context: Context,
        runtimeName: String,
        onProgress: (Float, String) -> Unit = { _, _ -> }
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            val destDir = getRuntimeHome(context, runtimeName)
            if (isRuntimeInstalled(context, runtimeName)) {
                ensureExecutablePermissions(destDir)
                return@withContext Result.success(destDir)
            }

            // Bersihkan direktori runtime lama yang mungkin rusak / salah permissions
            if (destDir.exists()) {
                destDir.deleteRecursively()
            }
            destDir.mkdirs()
            val arch = getDeviceArch()
            val assetPath = "runtimes/$runtimeName"

            // 1. Unpack universal.tar.xz (libraries, modules, config) - 0% to 55%
            onProgress(0.05f, "Mengekstrak Java Runtime ($runtimeName universal)...")
            val universalName = "$assetPath/universal.tar.xz"
            try {
                context.assets.open(universalName).use { input ->
                    unpackTarXz(
                        inputStream = input,
                        destDir = destDir,
                        baseProgress = 0.05f,
                        targetProgress = 0.55f,
                        estimatedTotal = 600,
                        label = "Mengekstrak runtime universal ($runtimeName)",
                        onProgress = onProgress
                    )
                }
            } catch (e: Exception) {
                android.util.Log.e("JavaRuntimeManager", "Gagal unpack universal.tar.xz: ${e.message}", e)
                throw e
            }

            // 2. Unpack bin-$arch.tar.xz (binaries, libjli, libjvm for arch) - 55% to 75%
            onProgress(0.55f, "Mengekstrak Java Runtime ($runtimeName bin-$arch)...")
            val binName = "$assetPath/bin-$arch.tar.xz"
            try {
                context.assets.open(binName).use { input ->
                    unpackTarXz(
                        inputStream = input,
                        destDir = destDir,
                        baseProgress = 0.55f,
                        targetProgress = 0.75f,
                        estimatedTotal = 80,
                        label = "Mengekstrak native binary ($runtimeName $arch)",
                        onProgress = onProgress
                    )
                }
            } catch (e: Exception) {
                android.util.Log.e("JavaRuntimeManager", "Gagal unpack bin-$arch.tar.xz: ${e.message}", e)
                throw e
            }

            // 3. Unpack file Pack200 (.pack) jika ada (khusus Java 8) - 75% to 92%
            unpackPack200Files(
                context = context,
                runtimeDir = destDir,
                baseProgress = 0.75f,
                targetProgress = 0.92f,
                onProgress = onProgress
            )

            // 4. Normalisasi tata letak arsitektur Java 8 (lib/aarch64 -> lib/)
            normalizeJava8ArchLayout(destDir)

            // 5. Mark executables & permissions across bin and lib folders
            onProgress(0.96f, "Memverifikasi izin sistem OpenJDK...")
            ensureExecutablePermissions(destDir)
            runCatching {
                File(destDir, ".nux_perm_v2").writeText("1.0.9")
            }

            if (!isRuntimeInstalled(context, runtimeName)) {
                return@withContext Result.failure(Exception("Verifikasi OpenJDK $runtimeName tidak lengkap setelah ekstraksi."))
            }

            onProgress(1.0f, "Ekstraksi OpenJDK $runtimeName selesai!")
            Result.success(destDir)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Membongkar seluruh file .pack (Pack200) menjadi file .jar (diperlukan untuk Java 8).
     */
    private suspend fun unpackPack200Files(
        context: Context,
        runtimeDir: File,
        baseProgress: Float,
        targetProgress: Float,
        onProgress: (Float, String) -> Unit
    ) = withContext(Dispatchers.IO) {
        val packFiles = runtimeDir.walkTopDown()
            .filter { it.isFile && it.name.endsWith(".pack") }
            .toList()

        if (packFiles.isEmpty()) return@withContext

        // Temukan biner unpack200 dari nativeLibraryDir aplikasi atau fallback bin/unpack200
        val nativeUnpack = File(context.applicationInfo.nativeLibraryDir, "libunpack200.so")
        val internalUnpack = File(runtimeDir, "bin/unpack200")
        if (internalUnpack.exists()) {
            ensureExecutablePermissions(runtimeDir)
        }

        val unpackCmd = when {
            nativeUnpack.exists() && nativeUnpack.canExecute() -> nativeUnpack.absolutePath
            internalUnpack.exists() -> internalUnpack.absolutePath
            nativeUnpack.exists() -> nativeUnpack.absolutePath
            else -> "libunpack200.so"
        }

        val total = packFiles.size
        packFiles.forEachIndexed { index, packFile ->
            val jarFile = File(packFile.parentFile, packFile.name.removeSuffix(".pack"))
            val p = baseProgress + (index.toFloat() / total) * (targetProgress - baseProgress)
            onProgress(p, "Membongkar bytecode Java 8: ${jarFile.name} (${index + 1}/$total)...")

            try {
                val pb = ProcessBuilder(unpackCmd, "-r", packFile.absolutePath, jarFile.absolutePath)
                pb.directory(runtimeDir)
                pb.redirectErrorStream(true)
                val process = pb.start()
                val exitCode = process.waitFor()
                if (exitCode != 0) {
                    android.util.Log.w("JavaRuntimeManager", "unpack200 exit code $exitCode for ${packFile.name}")
                }
            } catch (e: Exception) {
                android.util.Log.e("JavaRuntimeManager", "Gagal unpack200 ${packFile.name}: ${e.message}", e)
            }
        }
    }

    /**
     * Menghubungkan subdirektori arsitektur pada Java 8 (seperti lib/aarch64/)
     * ke direktori lib/ utama agar kompatibel dengan pemanggilan libjvm.so dan libjli.so standar.
     */
    private fun normalizeJava8ArchLayout(runtimeDir: File) {
        val libDir = File(runtimeDir, "lib")
        if (!libDir.exists() || !libDir.isDirectory) return

        val archNames = listOf("aarch64", "aarch32", "arm", "i386", "amd64", "x86_64")
        val archFolder = archNames.map { File(libDir, it) }.firstOrNull { it.exists() && it.isDirectory }
            ?: return

        // 1. Hubungkan server / client JVM folder
        val serverFolder = File(archFolder, "server")
        val clientFolder = File(archFolder, "client")
        val targetServer = File(libDir, "server")
        val targetClient = File(libDir, "client")

        if (serverFolder.exists() && !targetServer.exists()) {
            try {
                Os.symlink(serverFolder.absolutePath, targetServer.absolutePath)
            } catch (_: Throwable) {
                try { serverFolder.copyRecursively(targetServer, overwrite = true) } catch (_: Throwable) {}
            }
        }
        if (clientFolder.exists() && !targetClient.exists()) {
            try {
                Os.symlink(clientFolder.absolutePath, targetClient.absolutePath)
            } catch (_: Throwable) {
                try { clientFolder.copyRecursively(targetClient, overwrite = true) } catch (_: Throwable) {}
            }
        }

        // 2. Hubungkan jli folder
        val jliFolder = File(archFolder, "jli")
        val targetJli = File(libDir, "jli")
        if (jliFolder.exists() && !targetJli.exists()) {
            try {
                Os.symlink(jliFolder.absolutePath, targetJli.absolutePath)
            } catch (_: Throwable) {
                try { jliFolder.copyRecursively(targetJli, overwrite = true) } catch (_: Throwable) {}
            }
        }

        // 3. Hubungkan setiap shared library (.so) yang ada di archFolder ke libDir
        archFolder.listFiles { f -> f.isFile && f.extension == "so" }?.forEach { soFile ->
            val targetSo = File(libDir, soFile.name)
            if (!targetSo.exists()) {
                try {
                    Os.symlink(soFile.absolutePath, targetSo.absolutePath)
                } catch (_: Throwable) {
                    try { soFile.copyTo(targetSo, overwrite = true) } catch (_: Throwable) {}
                }
            }
        }
    }

    private fun unpackTarXz(
        inputStream: InputStream,
        destDir: File,
        baseProgress: Float = 0f,
        targetProgress: Float = 1f,
        estimatedTotal: Int = 100,
        label: String = "Mengekstrak",
        onProgress: (Float, String) -> Unit = { _, _ -> }
    ) {
        TarArchiveInputStream(XZCompressorInputStream(inputStream)).use { tarIn ->
            val buffer = ByteArray(32768)
            var entry = tarIn.nextEntry
            var fileCount = 0
            while (entry != null) {
                val cleanName = entry.name.removePrefix("./").removePrefix("/")
                if (cleanName.isNotEmpty() && cleanName != ".") {
                    val targetFile = File(destDir, cleanName)

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
                        if (cleanName.startsWith("bin/") || cleanName.endsWith(".so") || cleanName.contains(".so.")) {
                            try {
                                Os.chmod(targetFile.absolutePath, 493) // 0755
                            } catch (_: Throwable) {}
                            targetFile.setExecutable(true, false)
                            targetFile.setReadable(true, false)
                        }
                    }
                    fileCount++
                    val ratio = (fileCount.toFloat() / estimatedTotal).coerceIn(0f, 0.98f)
                    val p = baseProgress + ratio * (targetProgress - baseProgress)
                    val fileName = cleanName.substringAfterLast('/')
                    onProgress(p, "$label: $fileName ($fileCount file)")
                }
                entry = tarIn.nextEntry
            }
        }
    }
}
