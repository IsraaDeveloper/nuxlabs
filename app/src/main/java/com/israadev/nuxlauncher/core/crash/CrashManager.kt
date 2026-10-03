package com.israadev.nuxlauncher.core.crash

import android.content.Context
import com.google.gson.Gson
import com.israadev.nuxlauncher.core.instance.InstanceManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

/**
 * Data model representing a Minecraft / JVM game crash session or Launcher crash
 */
data class GameCrashInfo(
    val instanceName: String,
    val mcVersion: String,
    val exitCode: Int,
    val isSignal: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
    val logSnippet: String = "",
    val fullLogPath: String = "",
    val crashReportPath: String? = null,
    val crashType: String = CRASH_TYPE_GAME,
    val loader: String = "vanilla",
    val loaderVersion: String? = null,
    val installedMods: List<String> = emptyList()
) {
    companion object {
        const val CRASH_TYPE_GAME = "GAME_CRASH"
        const val CRASH_TYPE_LAUNCHER = "LAUNCHER_CRASH"
    }

    val isLauncherCrash: Boolean
        get() = crashType == CRASH_TYPE_LAUNCHER

    fun getDisplayTitle(): String {
        return if (isLauncherCrash) {
            "NUX LAUNCHER CRASH"
        } else {
            "MINECRAFT MENGALAMI CRASH"
        }
    }

    fun getStatusBadgeText(): String {
        return when {
            isLauncherCrash -> "LAUNCHER EXCEPTION"
            isSignal -> CrashUtils.getSignalName(exitCode)
            else -> "EXIT CODE $exitCode"
        }
    }

    fun getMainMessage(): String {
        return when {
            isLauncherCrash -> "Peluncur mengalami kesalahan tak terduga (Uncaught Exception)."
            isSignal -> "JVM dihentikan karena sinyal fatal $exitCode (${CrashUtils.getSignalName(exitCode)})."
            else -> "JVM keluar dengan kode $exitCode (${CrashUtils.getExitCodeDescription(exitCode)})."
        }
    }
}

object CrashManager {
    private const val CRASH_FILE_NAME = "last_game_crash.json"
    private const val SESSION_FILE_NAME = "active_game_session.json"
    private val gson = Gson()

    data class ActiveGameSession(
        val instanceName: String,
        val mcVersion: String,
        val rendererId: String,
        val startTime: Long = System.currentTimeMillis(),
        val gameDirPath: String? = null,
        val loader: String? = null,
        val loaderVersion: String? = null,
        val installedMods: List<String> = emptyList()
    )

    fun getInstalledMods(gameDirPath: String?): List<String> {
        if (gameDirPath.isNullOrBlank()) return emptyList()
        return try {
            val modsFolder = File(gameDirPath, "mods")
            if (!modsFolder.exists() || !modsFolder.isDirectory) return emptyList()
            modsFolder.listFiles { f ->
                f.isFile && (f.name.endsWith(".jar", ignoreCase = true) || f.name.endsWith(".disabled", ignoreCase = true))
            }?.map { it.name }?.sorted() ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun onGameSessionStarted(
        context: Context,
        instanceName: String,
        mcVersion: String,
        rendererId: String,
        gameDirPath: String? = null,
        loader: String? = null,
        loaderVersion: String? = null,
        installedMods: List<String> = emptyList()
    ) {
        try {
            val resolvedMods = if (installedMods.isNotEmpty()) installedMods else getInstalledMods(gameDirPath)
            val session = ActiveGameSession(
                instanceName = instanceName,
                mcVersion = mcVersion,
                rendererId = rendererId,
                startTime = System.currentTimeMillis(),
                gameDirPath = gameDirPath,
                loader = loader,
                loaderVersion = loaderVersion,
                installedMods = resolvedMods
            )
            val file = File(context.filesDir, SESSION_FILE_NAME)
            file.writeText(gson.toJson(session))
        } catch (_: Exception) {}
    }

    fun onGameSessionEnded(context: Context) {
        try {
            val file = File(context.filesDir, SESSION_FILE_NAME)
            if (file.exists()) {
                file.delete()
            }
        } catch (_: Exception) {}
    }

    private val _activeCrash = MutableStateFlow<GameCrashInfo?>(null)
    val activeCrash: StateFlow<GameCrashInfo?> = _activeCrash.asStateFlow()

    /**
     * Merekam crash (Game JVM ataupun Launcher)
     */
    fun recordCrash(
        context: Context,
        instanceName: String,
        mcVersion: String,
        exitCode: Int,
        isSignal: Boolean,
        gameDirPath: String,
        liveLogs: List<String> = emptyList(),
        exceptionDetail: String? = null,
        crashType: String = GameCrashInfo.CRASH_TYPE_GAME,
        loader: String = "vanilla",
        loaderVersion: String? = null,
        installedMods: List<String> = emptyList()
    ) {
        try {
            val filesDir = context.filesDir

            // 1. Cari crash report resmi dari folder crash-reports di gameDir
            val crashReportsDir = File(gameDirPath, "crash-reports")
            val latestCrashReport = if (crashReportsDir.exists() && crashReportsDir.isDirectory) {
                crashReportsDir.listFiles { f -> f.isFile && f.name.startsWith("crash-") && f.name.endsWith(".txt") }
                    ?.maxByOrNull { it.lastModified() }
            } else null

            // Cek apakah crash report dibuat baru-baru ini (kurang dari 15 menit lalu)
            val isRecentReport = latestCrashReport != null && (System.currentTimeMillis() - latestCrashReport.lastModified() < 900_000L)
            val logFile = File(filesDir, "latestlog.txt")

            val snippet = when {
                isRecentReport && latestCrashReport != null -> {
                    latestCrashReport.readLines().take(150).joinToString("\n")
                }
                exceptionDetail != null -> {
                    exceptionDetail
                }
                logFile.exists() -> {
                    val lines = logFile.readLines()
                    lines.takeLast(100).joinToString("\n")
                }
                liveLogs.isNotEmpty() -> {
                    liveLogs.takeLast(80).joinToString("\n")
                }
                else -> {
                    "JVM keluar dengan kode $exitCode (Signal: $isSignal).\nPeriksa file log untuk rincian lebih lanjut."
                }
            }

            val targetLogPath = when {
                isRecentReport && latestCrashReport != null -> latestCrashReport.absolutePath
                logFile.exists() -> logFile.absolutePath
                else -> ""
            }

            val resolvedMods = if (installedMods.isNotEmpty()) installedMods else getInstalledMods(gameDirPath)

            val crashInfo = GameCrashInfo(
                instanceName = instanceName,
                mcVersion = mcVersion,
                exitCode = exitCode,
                isSignal = isSignal,
                timestamp = System.currentTimeMillis(),
                logSnippet = snippet,
                fullLogPath = targetLogPath,
                crashReportPath = if (isRecentReport && latestCrashReport != null) latestCrashReport.absolutePath else null,
                crashType = crashType,
                loader = loader,
                loaderVersion = loaderVersion,
                installedMods = resolvedMods
            )

            val crashFile = File(filesDir, CRASH_FILE_NAME)
            crashFile.writeText(gson.toJson(crashInfo))
            _activeCrash.value = crashInfo
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Merekam crash yang terjadi pada aplikasi Launcher itu sendiri (Uncaught Exception)
     */
    fun recordLauncherCrash(context: Context, throwable: Throwable) {
        try {
            val filesDir = context.filesDir
            val launcherCrashFile = File(filesDir, "launcher-crash.txt")
            val stackTrace = android.util.Log.getStackTraceString(throwable)
            launcherCrashFile.writeText(
                "=== NUX Launcher Crash Report ===\n" +
                "Time: ${java.text.DateFormat.getDateTimeInstance().format(java.util.Date())}\n" +
                "Device: ${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL} (Android ${android.os.Build.VERSION.RELEASE})\n\n" +
                stackTrace
            )

            val crashInfo = GameCrashInfo(
                instanceName = "NUX Launcher",
                mcVersion = "App",
                exitCode = -1,
                isSignal = false,
                timestamp = System.currentTimeMillis(),
                logSnippet = stackTrace,
                fullLogPath = launcherCrashFile.absolutePath,
                crashReportPath = launcherCrashFile.absolutePath,
                crashType = GameCrashInfo.CRASH_TYPE_LAUNCHER
            )

            val crashFile = File(filesDir, CRASH_FILE_NAME)
            crashFile.writeText(gson.toJson(crashInfo))
            _activeCrash.value = crashInfo
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Memeriksa apakah ada file status crash saat MainActivity resume
     */
    fun checkAndNotify(context: Context) {
        try {
            val crashFile = File(context.filesDir, CRASH_FILE_NAME)
            if (crashFile.exists() && crashFile.length() > 0) {
                val json = crashFile.readText()
                val info = gson.fromJson(json, GameCrashInfo::class.java)
                if (info != null) {
                    _activeCrash.value = info
                    return
                }
            }

            // Deteksi jika sesi game mati mendadak (Native Abort / SIGABRT / SIGSEGV / OOM Killer)
            val sessionFile = File(context.filesDir, SESSION_FILE_NAME)
            if (sessionFile.exists()) {
                try {
                    val sessionJson = sessionFile.readText()
                    val session = gson.fromJson(sessionJson, ActiveGameSession::class.java)
                    sessionFile.delete() // hapus agar tidak muncul berulang

                    val logFile = File(context.filesDir, "latestlog.txt")
                    val logLines = if (logFile.exists()) logFile.readLines() else emptyList()
                    val tail = logLines.takeLast(100).joinToString("\n")

                    // 1. Cek apakah ada file crash-report resmi di folder gameDir instance yang aktif
                    val targetGameDir = session?.gameDirPath?.let { File(it) }
                    val crashReportsDir = if (targetGameDir != null) File(targetGameDir, "crash-reports") else null
                    val recentCrashFile = if (crashReportsDir != null && crashReportsDir.exists()) {
                        crashReportsDir.listFiles { f -> f.isFile && f.name.startsWith("crash-") && f.name.endsWith(".txt") }
                            ?.filter { it.lastModified() >= (session.startTime - 15000L) }
                            ?.maxByOrNull { it.lastModified() }
                    } else null

                    // 2. Cek apakah ada hs_err_pid JVM fatal error log di folder gameDir instance yang aktif
                    val recentHsErrFile = if (targetGameDir != null && targetGameDir.exists()) {
                        targetGameDir.listFiles { f -> f.isFile && f.name.startsWith("hs_err_pid") && f.name.endsWith(".log") }
                            ?.filter { it.lastModified() >= (session.startTime - 15000L) }
                            ?.maxByOrNull { it.lastModified() }
                    } else null

                    val snippet = when {
                        recentCrashFile != null -> {
                            recentCrashFile.readLines().take(120).joinToString("\n")
                        }
                        recentHsErrFile != null -> {
                            recentHsErrFile.readLines().take(80).joinToString("\n")
                        }
                        tail.isNotBlank() -> tail
                        else -> "Proses game terhenti mendadak (Kemungkinan dihentikan sistem karena kehabisan RAM atau crash driver native)."
                    }

                    var exitCode = when {
                        recentHsErrFile != null -> {
                            val firstLines = recentHsErrFile.readLines().take(10).joinToString(" ")
                            if (firstLines.contains("SIGSEGV", ignoreCase = true)) 139
                            else if (firstLines.contains("SIGABRT", ignoreCase = true)) 134
                            else 139
                        }
                        tail.contains("fatal signal 11", ignoreCase = true) || tail.contains("SIGSEGV", ignoreCase = true) -> 139
                        tail.contains("fatal signal 6", ignoreCase = true) || tail.contains("SIGABRT", ignoreCase = true) -> 134
                        else -> 137 // SIGKILL / OOM default jika sesi terputus tanpa exit code
                    }

                    val fullLogPath = when {
                        recentCrashFile != null -> recentCrashFile.absolutePath
                        recentHsErrFile != null -> recentHsErrFile.absolutePath
                        logFile.exists() -> logFile.absolutePath
                        else -> ""
                    }

                    val resolvedSessionMods = if (!session?.installedMods.isNullOrEmpty()) {
                        session?.installedMods ?: emptyList()
                    } else {
                        getInstalledMods(session?.gameDirPath)
                    }

                    val crashInfo = GameCrashInfo(
                        instanceName = session?.instanceName ?: "Minecraft",
                        mcVersion = session?.mcVersion ?: "Unknown",
                        exitCode = exitCode,
                        isSignal = true,
                        timestamp = System.currentTimeMillis(),
                        logSnippet = snippet,
                        fullLogPath = fullLogPath,
                        crashReportPath = recentCrashFile?.absolutePath ?: recentHsErrFile?.absolutePath,
                        crashType = GameCrashInfo.CRASH_TYPE_GAME,
                        loader = session?.loader ?: "vanilla",
                        loaderVersion = session?.loaderVersion,
                        installedMods = resolvedSessionMods
                    )

                    val cf = File(context.filesDir, CRASH_FILE_NAME)
                    cf.writeText(gson.toJson(crashInfo))
                    _activeCrash.value = crashInfo
                    return
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            // Fallback: periksa file crash report terbaru di folder instance yang sedang dipilih (bukan seluruh instance)
            val inst = InstanceManager.selectedInstance.value
            if (inst != null) {
                val instDir = File(InstanceManager.getInstancesDir(context), inst.id)
                val instGameDir = File(instDir, "minecraft")
                val crashReportsDir = File(instGameDir, "crash-reports")
                if (crashReportsDir.exists()) {
                    val recentCrash = crashReportsDir.listFiles { f -> f.isFile && f.name.startsWith("crash-") && f.name.endsWith(".txt") }
                        ?.maxByOrNull { it.lastModified() }

                    if (recentCrash != null && (System.currentTimeMillis() - recentCrash.lastModified() < 900_000L)) {
                        val marker = File(context.filesDir, "seen_${recentCrash.name}")
                        if (!marker.exists()) {
                            val lines = recentCrash.readLines()
                            val snippet = lines.take(150).joinToString("\n")
                            val fallbackMods = getInstalledMods(instGameDir.absolutePath)
                            val info = GameCrashInfo(
                                instanceName = inst.name,
                                mcVersion = inst.mcVersion,
                                exitCode = 1,
                                isSignal = false,
                                timestamp = recentCrash.lastModified(),
                                logSnippet = snippet,
                                fullLogPath = recentCrash.absolutePath,
                                crashReportPath = recentCrash.absolutePath,
                                crashType = GameCrashInfo.CRASH_TYPE_GAME,
                                loader = inst.loader,
                                loaderVersion = inst.loaderVersion,
                                installedMods = fallbackMods
                            )
                            _activeCrash.value = info
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Menghapus status crash setelah ditutup oleh user
     */
    fun dismissCrash(context: Context) {
        val current = _activeCrash.value
        _activeCrash.value = null
        try {
            val crashFile = File(context.filesDir, CRASH_FILE_NAME)
            if (crashFile.exists()) {
                crashFile.delete()
            }
            if (current?.crashReportPath != null) {
                val reportFile = File(current.crashReportPath)
                val marker = File(context.filesDir, "seen_${reportFile.name}")
                marker.createNewFile()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
