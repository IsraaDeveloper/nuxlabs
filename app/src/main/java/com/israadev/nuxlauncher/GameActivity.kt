package com.israadev.nuxlauncher

import android.content.Context
import com.israadev.nuxlauncher.core.crash.CrashManager
import com.israadev.nuxlauncher.core.renderer.NuxRendererRegistry
import com.israadev.nuxlauncher.ui.activities.ErrorActivity
import com.israadev.nuxlauncher.ui.components.GameLoadingOverlay
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.system.Os
import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.israadev.nuxlauncher.core.controls.ControlLayoutManager
import com.israadev.nuxlauncher.core.controls.models.CustomControlButton
import com.israadev.nuxlauncher.core.game.MCOptions
import com.israadev.nuxlauncher.core.settings.SettingsManager
import com.israadev.nuxlauncher.core.game.input.EfficientAndroidLWJGLKeycode
import com.israadev.nuxlauncher.core.game.input.HidableInputLayout
import com.israadev.nuxlauncher.core.game.input.TouchCharInput
import com.israadev.nuxlauncher.ui.components.NuxBadge
import com.israadev.nuxlauncher.ui.components.NuxButton
import com.israadev.nuxlauncher.ui.components.NuxCard
import com.israadev.nuxlauncher.ui.control.MouseControlMode
import com.israadev.nuxlauncher.ui.control.SwitchableMouseLayout
import com.israadev.nuxlauncher.ui.theme.NuxColors
import com.israadev.nuxlauncher.ui.theme.NuxSizes
import com.movtery.inputmap.keycodes.LwjglGlfwKeycode
import com.movtery.zalithlauncher.bridge.CURSOR_DISABLED
import com.movtery.zalithlauncher.bridge.CURSOR_ENABLED
import com.movtery.zalithlauncher.bridge.LoggerBridge
import com.movtery.zalithlauncher.bridge.ZLBridge
import com.movtery.zalithlauncher.bridge.ZLBridgeStates
import com.movtery.zalithlauncher.bridge.ZLNativeInvoker
import com.oracle.dalvik.VMLauncher
import com.movtery.zalithlauncher.game.sdl.SdlBridge
import org.libsdl.app.SDLActivity
import org.libsdl.app.SDLSurface
import org.lwjgl.glfw.CallbackBridge
import java.io.File
import kotlin.concurrent.thread
import kotlin.math.roundToInt

class GameActivity : ComponentActivity(), SurfaceHolder.Callback {

    companion object {
        const val EXTRA_INSTANCE_NAME = "extra_instance_name"
        const val EXTRA_MC_VERSION = "extra_mc_version"
        const val EXTRA_USERNAME = "extra_username"
        const val EXTRA_UUID = "extra_uuid"
        const val EXTRA_GAME_DIR = "extra_game_dir"
        const val EXTRA_ASSETS_DIR = "extra_assets_dir"
        const val EXTRA_ASSET_INDEX = "extra_asset_index"
        const val EXTRA_MAIN_CLASS = "extra_main_class"
        const val EXTRA_CLASSPATH = "extra_classpath"
        const val EXTRA_RUNTIME_NAME = "extra_runtime_name"
        const val EXTRA_RUNTIME_HOME = "extra_runtime_home"
        const val EXTRA_LWJGL_NATIVES_DIR = "extra_lwjgl_natives_dir"
        const val EXTRA_ACCESS_TOKEN = "extra_access_token"
        const val EXTRA_USER_TYPE = "extra_user_type"
        const val EXTRA_AUTHLIB_INJECTOR_PATH = "extra_authlib_injector_path"
        const val EXTRA_AUTHLIB_URL = "extra_authlib_url"
        const val EXTRA_USE_WRAPPER = "extra_use_wrapper"
    }

    private val liveLogs = mutableStateListOf<String>()
    private var isGameStarted = false
    private var surfaceHolderRef: SurfaceHolder? = null
    private val isControlVisibleState = mutableStateOf(true)
    private val isGameRenderingState = mutableStateOf(false)

    private var isManualExit = false
    private var sessionStartTime = System.currentTimeMillis()

    private fun handleGameExit(exitCode: Int, isSignal: Boolean, errorDetail: String? = null) {
        // Cek apakah game keluar bersih secara normal atas instruksi user (klik quit game / pause exit)
        val isNormalExit = isManualExit || (exitCode == 0 && !isSignal)

        if (!isNormalExit) {
            // Ini adalah CRASH (JVM Exit code != 0, fatal signal, OOM, atau exception)
            val crashReportsDir = File(gameDirPath, "crash-reports")
            val latestCrashReport = if (crashReportsDir.exists() && crashReportsDir.isDirectory) {
                crashReportsDir.listFiles { f -> f.isFile && f.name.startsWith("crash-") && f.name.endsWith(".txt") }
                    ?.maxByOrNull { it.lastModified() }
            } else null

            val hasRecentCrashReport = latestCrashReport != null && (latestCrashReport.lastModified() >= sessionStartTime - 10000L)
            val logFile = File(filesDir, "latestlog.txt")
            val effectiveLogPath = if (hasRecentCrashReport && latestCrashReport != null) latestCrashReport.absolutePath else if (logFile.exists()) logFile.absolutePath else ""

            val finalExitCode = if (exitCode != 0) exitCode else 1

            // 1. Rekam di CrashManager agar MainActivity juga tahu jika dibuka kembali
            CrashManager.recordCrash(
                context = this,
                instanceName = instanceName,
                mcVersion = mcVersion,
                exitCode = finalExitCode,
                isSignal = isSignal,
                gameDirPath = gameDirPath,
                liveLogs = liveLogs.toList(),
                exceptionDetail = errorDetail
            )

            // 2. Langsung luncurkan ErrorActivity mandiri (persis seperti Zalith Launcher)
            try {
                ErrorActivity.showGameCrash(
                    context = this,
                    exitCode = finalExitCode,
                    isSignal = isSignal,
                    logPath = effectiveLogPath,
                    instanceName = instanceName,
                    mcVersion = mcVersion
                )
            } catch (e: Throwable) {
                try {
                    val launcherIntent = Intent(this, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    }
                    startActivity(launcherIntent)
                } catch (_: Throwable) {}
            }
        } else {
            // Normal Exit
            CrashManager.onGameSessionEnded(this)
            try {
                val launcherIntent = Intent(this, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                startActivity(launcherIntent)
            } catch (_: Throwable) {}
        }

        terminateGameProcess()
    }

    private fun terminateGameProcess() {
        try {
            isManualExit = true
            CrashManager.onGameSessionEnded(this)
            ZLBridge.releaseBridgeWindow()
            com.israadev.nuxlauncher.core.skin.OfflineSkinServerManager.stopServer()
        } catch (_: Throwable) {}
        finish()
        android.os.Process.killProcess(android.os.Process.myPid())
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            isManualExit = true
            CrashManager.onGameSessionEnded(this)
            ZLBridge.releaseBridgeWindow()
            com.israadev.nuxlauncher.core.skin.OfflineSkinServerManager.stopServer()
        } catch (_: Throwable) {}
        android.os.Process.killProcess(android.os.Process.myPid())
    }

    private var instanceName = "Minecraft"
    private var mcVersion = "Unknown"
    private var username = "Player"
    private var uuid = "00000000-0000-0000-0000-000000000000"
    private var gameDirPath = ""
    private var assetsDirPath = ""
    private var assetIndexId = ""
    private var mainClass = "net.minecraft.client.main.Main"
    private var classpath = ""
    private var runtimeName = "jre-17"
    private var runtimeHomePath = ""
    private var lwjglNativesDirPath = ""
    private var accessToken = "0"
    private var userType = "mojang"
    private var authlibInjectorPath: String? = null
    private var authlibUrl: String? = null
    private var useWrapper = false

    private fun getScaledDisplayDimensions(): Pair<Int, Int> {
        val settings = SettingsManager.settings.value
        val ratio = (settings.resolutionRatio / 100f).coerceIn(0.5f, 1.5f)
        val rawW = resources.displayMetrics.widthPixels
        val rawH = resources.displayMetrics.heightPixels
        // GameActivity adalah sensorLandscape, pastikan lebar horizontal selalu lebih besar dari tinggi vertikal
        val screenW = maxOf(rawW, rawH)
        val screenH = minOf(rawW, rawH)
        val width = (screenW * ratio).roundToInt()
        val height = (screenH * ratio).roundToInt()
        return Pair(width, height)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        SettingsManager.init(this)
        ControlLayoutManager.init(this)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && SettingsManager.settings.value.sustainedPerformanceMode) {
            window.setSustainedPerformanceMode(true)
        }
        CallbackBridge.sContext = this

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val params = window.attributes
            params.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            window.clearFlags(WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN)
            window.addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN)
            window.attributes = params
        }

        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            or View.SYSTEM_UI_FLAG_FULLSCREEN
        )

        instanceName = intent.getStringExtra(EXTRA_INSTANCE_NAME) ?: "Minecraft"
        mcVersion = intent.getStringExtra(EXTRA_MC_VERSION) ?: "Unknown"
        username = intent.getStringExtra(EXTRA_USERNAME) ?: "Player"
        uuid = intent.getStringExtra(EXTRA_UUID) ?: "00000000-0000-0000-0000-000000000000"
        gameDirPath = intent.getStringExtra(EXTRA_GAME_DIR) ?: filesDir.absolutePath
        assetsDirPath = intent.getStringExtra(EXTRA_ASSETS_DIR) ?: File(filesDir, "minecraft/assets").absolutePath
        assetIndexId = intent.getStringExtra(EXTRA_ASSET_INDEX) ?: mcVersion
        mainClass = intent.getStringExtra(EXTRA_MAIN_CLASS) ?: "net.minecraft.client.main.Main"
        classpath = intent.getStringExtra(EXTRA_CLASSPATH) ?: ""
        runtimeName = intent.getStringExtra(EXTRA_RUNTIME_NAME) ?: "jre-17"
        runtimeHomePath = intent.getStringExtra(EXTRA_RUNTIME_HOME) ?: ""
        lwjglNativesDirPath = intent.getStringExtra(EXTRA_LWJGL_NATIVES_DIR) ?: ""
        accessToken = intent.getStringExtra(EXTRA_ACCESS_TOKEN) ?: "0"
        userType = intent.getStringExtra(EXTRA_USER_TYPE) ?: "mojang"
        authlibInjectorPath = intent.getStringExtra(EXTRA_AUTHLIB_INJECTOR_PATH)
        authlibUrl = intent.getStringExtra(EXTRA_AUTHLIB_URL)
        useWrapper = intent.getBooleanExtra(EXTRA_USE_WRAPPER, false)

        // Setup real-time native LoggerBridge
        setupLogger()

        // Setup exit callback
        ZLNativeInvoker.onExitCallback = { exitCode, isSignal ->
            runOnUiThread {
                liveLogs.add("[NUX Engine] Game process ended (code: $exitCode, signal: $isSignal)")
                handleGameExit(exitCode, isSignal)
            }
        }

        // Setup Graphic Output Listener (Mendeteksi Logo Mojang mulai dirender)
        CallbackBridge.setGraphicOutputListener {
            runOnUiThread {
                isGameRenderingState.value = true
                liveLogs.add("🎨 [Render Engine] First graphical frame rendered on Surface!")
                val midX = (CallbackBridge.windowWidth.takeIf { it > 0 } ?: 1280) / 2f
                val midY = (CallbackBridge.windowHeight.takeIf { it > 0 } ?: 720) / 2f
                CallbackBridge.sendCursorPos(midX, midY)
            }
        }

        val composeView = ComposeView(this).apply {
            setContent {
                GameScreen(
                    instanceName = instanceName,
                    mcVersion = mcVersion,
                    username = username,
                    mainClass = mainClass,
                    runtimeName = runtimeName,
                    liveLogs = liveLogs,
                    isControlVisibleState = isControlVisibleState,
                    isGameRenderingState = isGameRenderingState,
                    onSurfaceReady = { surfaceHolder ->
                        surfaceHolderRef = surfaceHolder
                        val (targetWidth, targetHeight) = getScaledDisplayDimensions()
                        surfaceHolder.setFixedSize(targetWidth, targetHeight)
                        surfaceHolder.addCallback(this@GameActivity)
                    },
                    onExit = {
                        isManualExit = true
                        terminateGameProcess()
                    }
                )
            }
        }

        setContentView(composeView)
    }

    private fun setupLogger() {
        val logFile = File(filesDir, "latestlog.txt")
        if (logFile.exists()) logFile.delete()
        logFile.createNewFile()

        LoggerBridge.setListener { text ->
            runOnUiThread {
                liveLogs.add(text)
                if (liveLogs.size > 200) {
                    liveLogs.removeAt(0)
                }
                // Deteksi sekunder jika frame grafik atau sistem audio game mulai aktif
                if (!isGameRenderingState.value) {
                    if (text.contains("OpenAL initialized") || 
                        text.contains("Reloading ResourceManager") || 
                        text.contains("Sound engine started") ||
                        text.contains("Setting user: ")) {
                        isGameRenderingState.value = true
                    }
                }
            }
        }

        try {
            LoggerBridge.start(logFile.absolutePath)
            LoggerBridge.appendTitle("NUX Launcher Game Session")
            LoggerBridge.append("▷ Starting $instanceName ($mcVersion) on $runtimeName")
        } catch (e: Throwable) {
            liveLogs.add("[LoggerBridge Init] ${e.message}")
        }
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        val (targetWidth, targetHeight) = getScaledDisplayDimensions()
        holder.setFixedSize(targetWidth, targetHeight)
        val surface = holder.surface
        val rootLayout = window.decorView as? ViewGroup
        SdlBridge.prepareSurface(this, surface, rootLayout, holder)
        try {
            ZLBridge.setupBridgeWindow(surface)
            liveLogs.add("[Render Bridge] Native window bound to SurfaceView (${targetWidth}x${targetHeight}).")
        } catch (e: Throwable) {
            liveLogs.add("[Render Bridge Error] ${e.message}")
        }

        if (!isGameStarted) {
            isGameStarted = true
            startGameJVM()
        }
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        val landscapeW = maxOf(width, height)
        val landscapeH = minOf(width, height)
        CallbackBridge.sendUpdateWindowSize(landscapeW, landscapeH)
        LoggerBridge.append("▷ [SurfaceChanged] Native surface buffer: ${landscapeW}x${landscapeH} (Scale: ${SettingsManager.settings.value.resolutionRatio}%)")
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        try {
            val nativeSurface = SDLSurface.getNativeSurface()
            if (SdlBridge.beginSurfaceDestroy(holder, nativeSurface)) {
                if (SdlBridge.sdlEnabled) {
                    SDLActivity.getSDLSurface()?.surfaceDestroyed()
                }
                SdlBridge.unregisterSurface(nativeSurface)
            }
            ZLBridge.releaseBridgeWindow()
        } catch (_: Throwable) {}
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        // If soft keyboard IME input view is active, allow standard handling
        if (TouchCharInput.isActive()) {
            return super.dispatchKeyEvent(event)
        }

        // Intercept right click from mouse being treated as KEYCODE_BACK
        val source = event.source
        if (source and InputDevice.SOURCE_MOUSE == InputDevice.SOURCE_MOUSE ||
            source and InputDevice.SOURCE_MOUSE_RELATIVE == InputDevice.SOURCE_MOUSE_RELATIVE) {
            if (event.keyCode == KeyEvent.KEYCODE_BACK) {
                val isDown = event.action == KeyEvent.ACTION_DOWN
                CallbackBridge.sendMouseButton(LwjglGlfwKeycode.GLFW_MOUSE_BUTTON_RIGHT, isDown)
                return true
            }
        }

        // Physical keyboard key pressed -> automatically hide touch controls
        if (event.action == KeyEvent.ACTION_DOWN) {
            isControlVisibleState.value = false
        }

        val index = EfficientAndroidLWJGLKeycode.getIndexByKey(event.keyCode)
        if (index >= 0) {
            EfficientAndroidLWJGLKeycode.execKey(event, index)
            return true
        }

        return super.dispatchKeyEvent(event)
    }

    override fun dispatchGenericMotionEvent(event: MotionEvent): Boolean {
        if (event.isFromSource(InputDevice.SOURCE_MOUSE) || event.isFromSource(InputDevice.SOURCE_MOUSE_RELATIVE)) {
            // Physical mouse action detected -> hide touch controls
            isControlVisibleState.value = false
        }
        return super.dispatchGenericMotionEvent(event)
    }

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        if (event.isFromSource(InputDevice.SOURCE_MOUSE) || event.isFromSource(InputDevice.SOURCE_MOUSE_RELATIVE)) {
            isControlVisibleState.value = false
        }
        return super.dispatchTouchEvent(event)
    }

    private fun startGameJVM() {
        thread(name = "NUX-JVM-Thread") {
            try {
                val nativeLibDir = applicationInfo.nativeLibraryDir
                val runtimeHome = File(runtimeHomePath)
                val gameDir = File(gameDirPath)
                val assetsDir = File(assetsDirPath)

                liveLogs.add("[NUX Launcher] Menyiapkan environment JVM...")

                val activeSettings = SettingsManager.settings.value
                val (targetWidth, targetHeight) = getScaledDisplayDimensions()

                // Setup & Optimize Minecraft options.txt (Zalith pure behavior)
                MCOptions.setup(this@GameActivity, gameDir)
                MCOptions.apply {
                    set("fullscreen", "false")
                    set("touchscreen", "false")
                    set("options.narrator", "0")
                    set("narrator", "0")

                    // Ensure default low-end mobile performance options if enabled in settings
                    if (activeSettings.autoOptimizeMinecraft) {
                        if (!containsKey("clouds")) set("clouds", "false")
                        if (!containsKey("renderClouds")) set("renderClouds", "false")
                        if (!containsKey("entityShadows")) set("entityShadows", "false")
                        if (!containsKey("renderDistance")) set("renderDistance", "2")
                        if (!containsKey("simulationDistance")) set("simulationDistance", "5")
                    }

                    set("overrideWidth", "$targetWidth")
                    set("overrideHeight", "$targetHeight")
                    save()
                }
                LoggerBridge.append("▷ [MCOptions] Minecraft options verified (res=${targetWidth}x${targetHeight}, autoOptimize=${activeSettings.autoOptimizeMinecraft})")

                // Verify & Ensure OpenJDK Runtime is fully installed
                if (!com.israadev.nuxlauncher.core.runtime.JavaRuntimeManager.isRuntimeInstalled(this@GameActivity, runtimeName)) {
                    liveLogs.add("[NUX Runtime] Mempersiapkan OpenJDK ($runtimeName)...")
                    LoggerBridge.append("▷ [NUX Runtime] Runtime $runtimeName missing or incomplete, extracting from assets...")
                    val extRes = kotlinx.coroutines.runBlocking {
                        com.israadev.nuxlauncher.core.runtime.JavaRuntimeManager.extractRuntime(this@GameActivity, runtimeName) { msg ->
                            liveLogs.add("[NUX Runtime] $msg")
                            LoggerBridge.append("▷ [NUX Runtime] $msg")
                        }
                    }
                    if (extRes.isFailure) {
                        val errMsg = extRes.exceptionOrNull()?.message ?: "Extraction failed"
                        liveLogs.add("[ERROR] Gagal mengekstrak OpenJDK $runtimeName: $errMsg")
                        LoggerBridge.append("[ERROR] Gagal mengekstrak OpenJDK: $errMsg")
                        throw Exception("Gagal menyiapkan OpenJDK ($runtimeName): $errMsg")
                    }
                    liveLogs.add("[NUX Runtime] OpenJDK ($runtimeName) siap digunakan!")
                }

                // Combined Library Path (Include Java runtime libraries for pojavexec dlopen)
                val javaLibDir = File(runtimeHome, "lib")
                val javaServerDir = File(runtimeHome, "lib/server")
                val javaJliDir = File(runtimeHome, "lib/jli")

                val javaLibPaths = listOf(javaLibDir, javaServerDir, javaJliDir)
                    .filter { it.exists() }
                    .map { it.absolutePath }

                // Ensure native libraries have executable permissions
                try {
                    javaLibDir.walkTopDown().filter { it.extension == "so" }.forEach {
                        it.setReadable(true, false)
                        it.setExecutable(true, false)
                    }
                    File(runtimeHome, "bin").walkTopDown().forEach {
                        it.setReadable(true, false)
                        it.setExecutable(true, false)
                    }
                } catch (_: Throwable) {}

                // Resolve Selected Graphics Renderer
                try {
                    com.israadev.nuxlauncher.core.renderer.NuxRendererPluginManager.scanPlugins(this)
                } catch (e: Throwable) {
                    LoggerBridge.append("▷ [Renderer] Plugin scan error: ${e.message}")
                }

                val targetRenderer = NuxRendererRegistry.resolveRenderer(
                    selectedId = activeSettings.selectedRenderer,
                    mcVersion = mcVersion,
                    nativeLibDir = File(nativeLibDir),
                    context = this
                )
                val rendererId = targetRenderer.rendererId
                val rendererSoName = targetRenderer.libraryName

                LoggerBridge.append("▷ [Renderer] Active Backend: ${targetRenderer.displayName} ($rendererId)")
                LoggerBridge.append("▷ [Renderer] Library: $rendererSoName, Selection: ${activeSettings.selectedRenderer}, IsPlugin: ${targetRenderer.isPlugin}")

                val runtimeHomeStr = runtimeHome.absolutePath
                val libDirName = if (Build.SUPPORTED_ABIS.any { it.contains("64") }) "lib64" else "lib"

                // 1. Runtime linker library path (Dipakai oleh ZLBridge.setLdLibraryPath untuk memperbarui Android Bionic Linker)
                val runtimeLdPaths = mutableListOf<String>()
                if (targetRenderer.isPlugin && !targetRenderer.pluginNativePath.isNullOrBlank()) {
                    runtimeLdPaths.add(targetRenderer.pluginNativePath!!)
                }
                runtimeLdPaths.add("$runtimeHomeStr/lib/jli")
                if (File("$runtimeHomeStr/jre").exists()) {
                    runtimeLdPaths.add("$runtimeHomeStr/jre/lib/server")
                    runtimeLdPaths.add("$runtimeHomeStr/jre/lib")
                } else {
                    runtimeLdPaths.add("$runtimeHomeStr/lib/server")
                    runtimeLdPaths.add("$runtimeHomeStr/lib")
                }
                if (lwjglNativesDirPath.isNotBlank()) runtimeLdPaths.add(lwjglNativesDirPath)
                runtimeLdPaths.add(nativeLibDir)
                runtimeLdPaths.add("/system/$libDirName")
                runtimeLdPaths.add("/vendor/$libDirName")
                runtimeLdPaths.add("/vendor/$libDirName/hw")
                runtimeLdPaths.add("/system_ext/$libDirName")

                val runtimeLdLibraryPath = runtimeLdPaths.distinct().joinToString(":")
                ZLBridge.setLdLibraryPath(runtimeLdLibraryPath)

                // 2. Game LD_LIBRARY_PATH (Identik dengan ZalithLauncher2 getLibraryPath() - JANGAN SAMPAI ADA FOLDER JRE lib/server / lib/jli!)
                // Jika LD_LIBRARY_PATH mengandung 'lib/server' atau 'lib/client', OpenJDK libjli.so akan mengira perlu re-exec
                // dan memanggil execve(bin/java) yang otomatis ditolak Android kernel (Permission denied)!
                val gameLdPaths = mutableListOf<String>()
                if (lwjglNativesDirPath.isNotBlank()) gameLdPaths.add(lwjglNativesDirPath)
                gameLdPaths.add("/system/$libDirName")
                gameLdPaths.add("/vendor/$libDirName")
                gameLdPaths.add("/vendor/$libDirName/hw")
                gameLdPaths.add("/system_ext/$libDirName")
                if (targetRenderer.isPlugin && !targetRenderer.pluginNativePath.isNullOrBlank()) {
                    gameLdPaths.add(targetRenderer.pluginNativePath!!)
                }
                gameLdPaths.add(nativeLibDir)
                val gameLdLibraryPath = gameLdPaths.distinct().joinToString(":")

                val glLibPath = if (targetRenderer.isPlugin && !targetRenderer.pluginNativePath.isNullOrBlank()) {
                    if (rendererSoName.startsWith("/")) rendererSoName else "${targetRenderer.pluginNativePath}/$rendererSoName"
                } else {
                    "$nativeLibDir/$rendererSoName"
                }

                val actualPojavRenderer = when {
                    rendererId.startsWith("opengles") -> rendererId
                    rendererId.startsWith("vulkan") -> rendererId
                    rendererId.startsWith("gallium") -> rendererId
                    rendererId == "custom_gallium" -> rendererId
                    else -> "opengles3"
                }
                Os.setenv("POJAV_NATIVEDIR", nativeLibDir, true)
                Os.setenv("POJAV_RENDERER", actualPojavRenderer, true)
                Os.setenv("POJAV_SDL_REUSE_WINDOW", "1", true)
                Os.setenv("SDL_OPENGL_LIBRARY", glLibPath, true)

                // Apply renderer specific environment variables
                targetRenderer.envVariables.forEach { (k, v) ->
                    Os.setenv(k, v, true)
                }

                // Apply Renderer V2 Environment Variables (persis Zalith fclPlugin_V2 & MobileGlues)
                try {
                    val v2Envs = com.israadev.nuxlauncher.core.renderer.v2.NuxRendererV2Manager.getEffectiveEnv(targetRenderer)
                    v2Envs.forEach { (k, v) ->
                        Os.setenv(k, v, true)
                        LoggerBridge.append("▷ [Renderer V2 Env] $k = $v")
                    }
                } catch (e: Exception) {
                    LoggerBridge.append("▷ [Renderer V2 Env Warning] Gagal menginjeksi V2 env: ${e.message}")
                }

                // Apply EGL specific library path if specified (full absolute path for plugins)
                val eglPath = if (!targetRenderer.eglName.isNullOrBlank()) {
                    val rawEgl = targetRenderer.eglName!!
                    if (rawEgl.startsWith("/")) rawEgl
                    else if (targetRenderer.isPlugin && !targetRenderer.pluginNativePath.isNullOrBlank()) "${targetRenderer.pluginNativePath}/$rawEgl"
                    else "$nativeLibDir/$rawEgl"
                } else null

                if (!eglPath.isNullOrBlank()) {
                    Os.setenv("POJAVEXEC_EGL", eglPath, true)
                    Os.setenv("SDL_EGL_LIBRARY", eglPath, true)
                }

                // Apply Vulkan / Zink / Graphics API settings
                if (activeSettings.zinkPreferSystemDriver) {
                    Os.setenv("POJAV_ZINK_PREFER_SYSTEM_DRIVER", "1", true)
                    LoggerBridge.append("▷ [Renderer Config] POJAV_ZINK_PREFER_SYSTEM_DRIVER=1")
                }
                if (activeSettings.vsyncInZink) {
                    Os.setenv("POJAV_VSYNC_IN_ZINK", "1", true)
                    LoggerBridge.append("▷ [Renderer Config] POJAV_VSYNC_IN_ZINK=1")
                }
                if (activeSettings.graphicsApi != "DEFAULT") {
                    Os.setenv("POJAV_GRAPHICS_API", activeSettings.graphicsApi, true)
                    LoggerBridge.append("▷ [Renderer Config] POJAV_GRAPHICS_API=${activeSettings.graphicsApi}")
                }
                if (activeSettings.vulkanDriver != "auto" && activeSettings.vulkanDriver.isNotBlank()) {
                    Os.setenv("POJAV_VULKAN_DRIVER", activeSettings.vulkanDriver, true)
                    LoggerBridge.append("▷ [Renderer Config] POJAV_VULKAN_DRIVER=${activeSettings.vulkanDriver}")
                }

                // Pre-dlopen plugin libraries from the plugin's own directory ONLY
                if (targetRenderer.isPlugin && !targetRenderer.pluginNativePath.isNullOrBlank()) {
                    val pluginDir = File(targetRenderer.pluginNativePath)
                    targetRenderer.dlopenLibs.forEach { dlName ->
                        val dlFile = File(pluginDir, dlName)
                        if (dlFile.exists()) {
                            try {
                                ZLBridge.dlopen(dlFile.absolutePath)
                                LoggerBridge.append("▷ [Plugin DLOPEN] $dlName")
                            } catch (e: Throwable) {
                                LoggerBridge.append("▷ [Plugin DLOPEN Warning] $dlName: ${e.message}")
                            }
                        }
                    }
                    val targetSo = File(glLibPath)
                    if (targetSo.exists()) {
                        try {
                            ZLBridge.dlopen(targetSo.absolutePath)
                            LoggerBridge.append("▷ [Plugin GL Loaded] ${targetSo.absolutePath}")
                        } catch (e: Throwable) {
                            LoggerBridge.append("▷ [Plugin GL Warning] ${targetRenderer.libraryName}: ${e.message}")
                        }
                    }
                    if (!eglPath.isNullOrBlank() && eglPath != glLibPath) {
                        val eglFile = File(eglPath)
                        if (eglFile.exists()) {
                            try {
                                ZLBridge.dlopen(eglFile.absolutePath)
                                LoggerBridge.append("▷ [Plugin EGL Loaded] ${eglFile.absolutePath}")
                            } catch (e: Throwable) {
                                LoggerBridge.append("▷ [Plugin EGL Warning]: ${e.message}")
                            }
                        }
                    }
                }

                // VirGL socket path
                if (rendererId == "gallium_virgl") {
                    Os.setenv("VTEST_SOCKET_NAME", File(cacheDir, ".virgl_test").absolutePath, true)
                }

                // Mesa GLSL Cache Directory
                if (rendererId.startsWith("gallium") || rendererId.contains("zink")) {
                    Os.setenv("MESA_GLSL_CACHE_DIR", cacheDir.absolutePath, true)
                }
                Os.setenv("JAVA_HOME", runtimeHome.absolutePath, true)
                Os.setenv("HOME", gameDir.absolutePath, true)
                Os.setenv("TMPDIR", cacheDir.absolutePath, true)
                Os.setenv("PATH", "${runtimeHome.absolutePath}/bin:" + (Os.getenv("PATH") ?: "/system/bin"), true)
                Os.setenv("LD_LIBRARY_PATH", gameLdLibraryPath, true)
                Os.setenv("AWTSTUB_WIDTH", "$targetWidth", true)
                Os.setenv("AWTSTUB_HEIGHT", "$targetHeight", true)
                Os.setenv("ALSOFT_DRIVERS", "opensl", true)

                // Android DNS Resolver Setup (Matches Zalith & Pojav for SRV record resolution)
                val resolvConf = File(filesDir, "resolv.conf")
                val dnsServers = buildSet {
                    try {
                        val cm = getSystemService(android.net.ConnectivityManager::class.java)
                        val activeNet = cm?.activeNetwork
                        if (activeNet != null) {
                            val lp = cm.getLinkProperties(activeNet)
                            lp?.dnsServers
                                ?.mapNotNull { it.hostAddress?.takeIf(String::isNotEmpty) }
                                ?.filterNot { it.contains(':') }
                                ?.let { addAll(it) }
                        }
                    } catch (_: Throwable) {}
                    add("1.1.1.1")
                    add("1.0.0.1")
                    add("8.8.8.8")
                    add("8.8.4.4")
                }
                val configText = dnsServers.joinToString(separator = "\n") { "nameserver $it" }
                runCatching {
                    resolvConf.writeText(configText)
                    File(gameDir, "resolv.conf").writeText(configText)
                }
                try {
                    Os.setenv("RESOLV_CONF", resolvConf.absolutePath, true)
                    Os.setenv("RES_OPTIONS", "retrans:1 retry:1 timeout:2", true)
                } catch (_: Throwable) {}

                // Pre-dlopen Java Runtime core libraries (Identical to Zalith Launcher 2 dlopenJavaRuntime)
                val isJava8 = File(runtimeHome, "jre").exists()
                val rtLibDir = if (isJava8) File(runtimeHome, "jre/lib") else File(runtimeHome, "lib")
                val rtJliDir = if (File(rtLibDir, "jli/libjli.so").exists()) File(rtLibDir, "jli") else rtLibDir
                val rtJvmDir = if (File(rtLibDir, "server/libjvm.so").exists()) File(rtLibDir, "server") else File(rtLibDir, "client")

                val essentialLibs = listOf(
                    File(rtJliDir, "libjli.so"),
                    File(rtJvmDir, "libjvm.so"),
                    File(rtLibDir, "libfreetype.so"),
                    File(rtLibDir, "libverify.so"),
                    File(rtLibDir, "libjava.so"),
                    File(rtLibDir, "libnet.so"),
                    File(rtLibDir, "libnio.so"),
                    File(rtLibDir, "libawt.so"),
                    File(rtLibDir, "libawt_headless.so"),
                    File(rtLibDir, "libfontmanager.so")
                )
                essentialLibs.forEach { so ->
                    if (so.exists()) {
                        try {
                            ZLBridge.dlopen(so.absolutePath)
                            LoggerBridge.append("▷ [Pre-dlopen] Loaded ${so.name}")
                        } catch (e: Throwable) {
                            LoggerBridge.append("▷ [Pre-dlopen Warning] ${so.name}: ${e.message}")
                        }
                    }
                }

                // Pre-dlopen all remaining runtime shared libraries (.so) as Zalith does
                runtimeHome.walkTopDown().filter { it.isFile && it.name.endsWith(".so") }.forEach { so ->
                    try {
                        ZLBridge.dlopen(so.absolutePath)
                    } catch (_: Throwable) {}
                }

                // Pre-dlopen OpenAL engine (Matches Zalith dlopenEngine)
                val openalFile = File(nativeLibDir, "libopenal.so")
                if (openalFile.exists()) {
                    try {
                        ZLBridge.dlopen(openalFile.absolutePath)
                        LoggerBridge.append("▷ [Pre-dlopen] Loaded libopenal.so")
                    } catch (_: Throwable) {}
                }

                // Pre-dlopen internal renderer ONLY if NOT a plugin (prevents overriding plugin libraries with internal ones)
                if (!targetRenderer.isPlugin) {
                    try {
                        val rendererFile = File(nativeLibDir, rendererSoName)
                        if (rendererFile.exists()) {
                            ZLBridge.dlopen(rendererFile.absolutePath)
                            LoggerBridge.append("▷ [Pre-dlopen] Loaded internal renderer: $rendererSoName")
                        } else {
                            LoggerBridge.append("▷ [Pre-dlopen Warning] Renderer file not found: $rendererSoName")
                        }
                    } catch (e: Throwable) {
                        LoggerBridge.append("▷ [Pre-dlopen Warning] Renderer $rendererSoName: ${e.message}")
                    }

                    if (!eglPath.isNullOrBlank() && eglPath != glLibPath) {
                        val eglFile = File(eglPath)
                        if (eglFile.exists()) {
                            try {
                                ZLBridge.dlopen(eglFile.absolutePath)
                                LoggerBridge.append("▷ [Pre-dlopen] Loaded internal EGL: ${eglFile.name}")
                            } catch (_: Throwable) {}
                        }
                    }
                }

                // Pre-dlopen SPIRV-Cross if available
                val spirvLibFile = File(nativeLibDir, "libspirv-cross-c-shared.so")
                if (spirvLibFile.exists()) {
                    try {
                        ZLBridge.dlopen(spirvLibFile.absolutePath)
                        LoggerBridge.append("▷ [Pre-dlopen] Loaded libspirv-cross-c-shared.so")
                    } catch (e: Throwable) {
                        LoggerBridge.append("▷ [Pre-dlopen Warning] spirv: ${e.message}")
                    }
                }

                // Pre-dlopen LWJGL natives
                if (lwjglNativesDirPath.isNotBlank()) {
                    val lwjglDir = File(lwjglNativesDirPath)
                    listOf("liblwjgl.so", "liblwjgl_opengl.so", "liblwjgl_stb.so", "liblwjgl_tinyfd.so", "libfreetype.so", "libshaderc.so", "liblwjgl_vma.so", "libspirv-cross.so").forEach { libName ->
                        val so = File(lwjglDir, libName)
                        if (so.exists()) {
                            try {
                                ZLBridge.dlopen(so.absolutePath)
                                LoggerBridge.append("▷ [Pre-dlopen] Loaded ${so.name}")
                            } catch (e: Throwable) {
                                LoggerBridge.append("▷ [Pre-dlopen Warning] ${so.name}: ${e.message}")
                            }
                        }
                    }
                }

                ZLBridge.setupExitMethod(this@GameActivity)
                ZLBridge.initializeGameExitHook()
                ZLBridge.chdir(gameDir.absolutePath)

                // Build JVM Launch Arguments
                val effectiveLwjglDir = if (lwjglNativesDirPath.isNotBlank()) lwjglNativesDirPath else nativeLibDir
                val freetypeLib = if (lwjglNativesDirPath.isNotBlank() && File(lwjglNativesDirPath, "libfreetype.so").exists()) {
                    File(lwjglNativesDirPath, "libfreetype.so").absolutePath
                } else {
                    "$nativeLibDir/libfreetype.so"
                }

                val spirvLibPath = if (lwjglNativesDirPath.isNotBlank() && File(lwjglNativesDirPath, "libspirv-cross.so").exists()) {
                    File(lwjglNativesDirPath, "libspirv-cross.so").absolutePath
                } else if (File(nativeLibDir, "libspirv-cross-c-shared.so").exists()) {
                    File(nativeLibDir, "libspirv-cross-c-shared.so").absolutePath
                } else {
                    "libspirv-cross-c-shared.so"
                }

                // Pastikan permission executable (0755) pada bin/java dan runtime native libraries
                com.israadev.nuxlauncher.core.runtime.JavaRuntimeManager.ensureExecutablePermissions(runtimeHome)

                val jvmArgs = mutableListOf<String>()
                jvmArgs.add("${runtimeHome.absolutePath}/bin/java")
                jvmArgs.add("-Djava.home=${runtimeHome.absolutePath}")
                jvmArgs.add("-Djava.io.tmpdir=${cacheDir.absolutePath}")
                jvmArgs.add("-Djava.library.path=$gameLdLibraryPath")
                jvmArgs.add("-Dorg.lwjgl.librarypath=$effectiveLwjglDir")
                jvmArgs.add("-Dorg.lwjgl.opengl.libname=$glLibPath")
                jvmArgs.add("-Dorg.lwjgl.openal.libname=$nativeLibDir/libopenal.so")
                jvmArgs.add("-Dorg.lwjgl.vulkan.libname=libvulkan.so")
                jvmArgs.add("-Dorg.lwjgl.freetype.libname=$freetypeLib")
                jvmArgs.add("-Dorg.lwjgl.spvc.libname=$spirvLibPath")
                jvmArgs.add("-Dorg.lwjgl.spvc.defaultname=spirv-cross-c-shared")
                jvmArgs.add("-Dorg.lwjgl.system.allocator=system")
                jvmArgs.add("-Djna.boot.library.path=$nativeLibDir")
                jvmArgs.add("-Dglfwstub.windowWidth=$targetWidth")
                jvmArgs.add("-Dglfwstub.windowHeight=$targetHeight")
                jvmArgs.add("-Dglfwstub.initEgl=false")
                jvmArgs.add("-Dos.name=Linux")
                jvmArgs.add("-Dos.version=Android-${Build.VERSION.RELEASE}")
                jvmArgs.add("-Duser.home=${gameDir.parentFile?.absolutePath ?: gameDir.absolutePath}")
                jvmArgs.add("-Dnet.minecraft.clientmodname=NUX-Launcher")
                jvmArgs.add("-Dlog4j2.formatMsgNoLookups=true")
                jvmArgs.add("-Djava.rmi.server.useCodebaseOnly=true")
                jvmArgs.add("-Dcom.sun.jndi.rmi.object.trustURLCodebase=false")
                jvmArgs.add("-Dcom.sun.jndi.cosnaming.object.trustURLCodebase=false")
                jvmArgs.add("-Dfml.earlyprogresswindow=false")
                jvmArgs.add("-Dfml.ignoreInvalidMinecraftCertificates=true")
                jvmArgs.add("-Dfml.ignorePatchDiscrepancies=true")
                jvmArgs.add("-Dloader.disable_forked_guis=true")
                jvmArgs.add("-Djdk.lang.Process.launchMechanism=FORK")
                jvmArgs.add("-Dsodium.checks.issue2561=false")
                jvmArgs.add("-Dfile.encoding=UTF-8")
                jvmArgs.add("-Dsun.stdout.encoding=UTF-8")
                jvmArgs.add("-Dsun.stderr.encoding=UTF-8")

                // Robust Android Socket & Networking Stabilization (Direct match with Zalith / Pojav)
                jvmArgs.add("-Dext.net.resolvPath=${resolvConf.absolutePath}")
                jvmArgs.add("-Djava.net.preferIPv4Stack=true")
                jvmArgs.add("-Djava.net.preferIPv6Addresses=false")
                jvmArgs.add("-Dsun.net.dns.nameservers=${dnsServers.joinToString(",")}")
                jvmArgs.add("-Ddns.server=${dnsServers.first()}")
                jvmArgs.add("-Dsun.net.spi.nameservice.nameservers=${dnsServers.joinToString(",")}")
                jvmArgs.add("-Dsun.net.spi.nameservice.provider.1=dns,sun")
                jvmArgs.add("-Dio.netty.tryReflectionSetAccessible=true")
                jvmArgs.add("-Dnetworkaddress.cache.ttl=30")
                jvmArgs.add("-Dnetworkaddress.cache.negative.ttl=10")

                jvmArgs.add("-XX:ActiveProcessorCount=${Runtime.getRuntime().availableProcessors()}")
                jvmArgs.add("-Xms${activeSettings.initialHeapMb}M")
                jvmArgs.add("-Xmx${activeSettings.ramMb}M")
                if (activeSettings.customJvmArgs.isNotBlank()) {
                    activeSettings.customJvmArgs.split(" ")
                        .map { it.trim() }
                        .filter { it.isNotEmpty() }
                        .forEach { jvmArgs.add(it) }
                }

                val nuxPatcher = File(com.israadev.nuxlauncher.core.instance.InstanceManager.getNuxDir(this), "components/launcher/MioLibPatcher.jar")
                val patcherJar = if (nuxPatcher.exists()) nuxPatcher else File(filesDir, "components/launcher/MioLibPatcher.jar")
                if (patcherJar.exists()) {
                    jvmArgs.add("-javaagent:${patcherJar.absolutePath}")
                    LoggerBridge.append("▷ Enabled MioLibPatcher bytecode agent")
                }

                // Enable authlib-injector if using Ely.by or custom auth server
                if (!authlibInjectorPath.isNullOrBlank() && !authlibUrl.isNullOrBlank()) {
                    val authlibJar = File(authlibInjectorPath!!)
                    if (authlibJar.exists()) {
                        jvmArgs.add("-javaagent:${authlibJar.absolutePath}=$authlibUrl")
                        jvmArgs.add("-Dauthlibinjector.side=client")
                        LoggerBridge.append("▷ Enabled authlib-injector for external auth ($authlibUrl)")
                    }
                }

                if (runtimeName.contains("21") || runtimeName.contains("25") || runtimeName.contains("17")) {
                    jvmArgs.add("--add-opens=java.base/java.lang=ALL-UNNAMED")
                    jvmArgs.add("--add-opens=java.base/java.lang.reflect=ALL-UNNAMED")
                    jvmArgs.add("--add-opens=java.base/java.util=ALL-UNNAMED")
                    jvmArgs.add("--add-opens=java.base/java.text=ALL-UNNAMED")
                    jvmArgs.add("--add-opens=java.base/java.net=ALL-UNNAMED")
                    jvmArgs.add("--add-opens=jdk.naming.dns/com.sun.jndi.dns=ALL-UNNAMED")
                    jvmArgs.add("--add-exports=jdk.naming.dns/com.sun.jndi.dns=ALL-UNNAMED")
                    jvmArgs.add("--add-opens=java.desktop/sun.awt=ALL-UNNAMED")
                    jvmArgs.add("--add-opens=java.desktop/java.awt=ALL-UNNAMED")
                    jvmArgs.add("--add-opens=java.desktop/sun.font=ALL-UNNAMED")
                    jvmArgs.add("--add-opens=java.desktop/sun.java2d=ALL-UNNAMED")

                    if (mainClass.contains(".")) {
                        val pkg = mainClass.substring(0, mainClass.lastIndexOf("."))
                        jvmArgs.add("--add-exports")
                        jvmArgs.add("$pkg/$pkg=ALL-UNNAMED")
                    }
                }

                // Sanitasi classpath: buang duplikat dan pastikan tidak ada bentrok versi ASM jika menjalankan Fabric/Knot
                val effectiveClasspath = if (mainClass.contains("knot") || mainClass.contains("fabric")) {
                    val entries = classpath.split(File.pathSeparator).filter { it.isNotBlank() }
                    val hasModernAsm = entries.any { it.contains("org/ow2/asm") && !it.contains("/9.6/") }
                    if (hasModernAsm) {
                        entries.filterNot { it.contains("org/ow2/asm") && it.contains("/9.6/") }
                    } else {
                        entries
                    }.distinct().joinToString(File.pathSeparator)
                } else {
                    classpath.split(File.pathSeparator).filter { it.isNotBlank() }.distinct().joinToString(File.pathSeparator)
                }

                jvmArgs.add("-cp")
                jvmArgs.add(effectiveClasspath)
                if (useWrapper) {
                    jvmArgs.add("mio.Wrapper")
                }
                jvmArgs.add(mainClass)
                jvmArgs.add("--username")
                jvmArgs.add(username)
                jvmArgs.add("--version")
                jvmArgs.add(mcVersion)
                jvmArgs.add("--gameDir")
                jvmArgs.add(gameDir.absolutePath)
                jvmArgs.add("--assetsDir")
                jvmArgs.add(assetsDir.absolutePath)
                jvmArgs.add("--assetIndex")
                jvmArgs.add(assetIndexId)
                jvmArgs.add("--uuid")
                jvmArgs.add(uuid)
                jvmArgs.add("--accessToken")
                jvmArgs.add(accessToken)
                jvmArgs.add("--userType")
                jvmArgs.add(userType)
                jvmArgs.add("--versionType")
                jvmArgs.add("release")

                LoggerBridge.appendTitle("JVM Launch Command")
                jvmArgs.forEach { LoggerBridge.append("▷ $it") }

                CrashManager.onGameSessionStarted(this, instanceName, mcVersion, activeSettings.selectedRenderer, gameDir.absolutePath)
                liveLogs.add("[NUX Engine] Memulai eksekusi VMLauncher.launchJVM()...")
                val exitCode = VMLauncher.launchJVM(jvmArgs.toTypedArray())
                liveLogs.add("[NUX Engine] JVM selesai dengan kode keluar: $exitCode")
                runOnUiThread {
                    handleGameExit(exitCode, false)
                }
            } catch (e: Throwable) {
                LoggerBridge.append("[ERROR JVM Launch] ${e.message}")
                e.printStackTrace()
                runOnUiThread {
                    handleGameExit(-1, false, e.stackTraceToString())
                }
            }
        }
    }
}

enum class FpsMode {
    NORMAL,    // Siklus 3 / Awal: Mengikuti visibilitas GUI (isControlVisible), log tertutup
    PINNED,    // Siklus 1: FPS di-pin, tetap tampil meski GUI di-hide, log tertutup
    SHOW_LOG   // Siklus 2: Memunculkan log in-game (isConsoleVisible = true)
}

@Composable
fun GameScreen(
    instanceName: String,
    mcVersion: String,
    username: String,
    mainClass: String,
    runtimeName: String,
    liveLogs: List<String>,
    isControlVisibleState: MutableState<Boolean>,
    isGameRenderingState: MutableState<Boolean>,
    onSurfaceReady: (SurfaceHolder) -> Unit,
    onExit: () -> Unit
) {
    var isControlVisible by isControlVisibleState
    var isGameRendering by isGameRenderingState
    var isManualLoadingDismissed by remember { mutableStateOf(false) }
    val showLoadingOverlay = !isGameRendering && !isManualLoadingDismissed

    var isKeyboardRequested by remember { mutableStateOf(false) }
    var isConsoleVisible by remember { mutableStateOf(false) }
    var isConsoleExpanded by remember { mutableStateOf(false) }
    var showExitConfirmDialog by remember { mutableStateOf(false) }
    var currentFps by remember { mutableIntStateOf(0) }
    var fpsMode by remember { mutableStateOf(FpsMode.NORMAL) }
    val listState = rememberLazyListState()
    val context = LocalContext.current

    // Handler siklus 3-klik tombol FPS
    val cycleFpsMode: () -> Unit = {
        when (fpsMode) {
            FpsMode.NORMAL -> {
                fpsMode = FpsMode.PINNED
                isConsoleVisible = false
                Toast.makeText(context, "📌 FPS Di-Pin (Tetap tampil saat GUI disembunyikan)", Toast.LENGTH_SHORT).show()
            }
            FpsMode.PINNED -> {
                fpsMode = FpsMode.SHOW_LOG
                isConsoleVisible = true
                Toast.makeText(context, "📜 Menampilkan Live Log Minecraft", Toast.LENGTH_SHORT).show()
            }
            FpsMode.SHOW_LOG -> {
                fpsMode = FpsMode.NORMAL
                isConsoleVisible = false
                Toast.makeText(context, "FPS Mode Normal (Mengikuti visibilitas GUI)", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Periodically update FPS counter from native engine
    LaunchedEffect(Unit) {
        while (isActive) {
            try {
                currentFps = CallbackBridge.getCurrentFps()
            } catch (_: Throwable) {
            }
            delay(500)
        }
    }

    val gameCursorMode by ZLBridgeStates.cursorMode.collectAsState()
    val launcherSettings by SettingsManager.settings.collectAsState()
    val customButtons by ControlLayoutManager.buttons.collectAsState()
    val mouseControlMode = if (launcherSettings.mouseControlMode == "CLICK") MouseControlMode.CLICK else MouseControlMode.SLIDE

    LaunchedEffect(liveLogs.size) {
        if (liveLogs.isNotEmpty()) {
            listState.animateScrollToItem(liveLogs.size - 1)
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        val screenWidth = constraints.maxWidth.toFloat()
        val screenHeight = constraints.maxHeight.toFloat()

        var cursorX by remember { mutableFloatStateOf(0f) }
        var cursorY by remember { mutableFloatStateOf(0f) }
        var isMousePressed by remember { mutableStateOf(false) }

        LaunchedEffect(screenWidth, screenHeight) {
            if (cursorX == 0f && screenWidth > 0f) {
                cursorX = screenWidth / 2f
                cursorY = screenHeight / 2f
                val winW = CallbackBridge.windowWidth
                val winH = CallbackBridge.windowHeight
                val targetX = if (screenWidth > 0 && winW > 0) cursorX * (winW.toFloat() / screenWidth) else cursorX
                val targetY = if (screenHeight > 0 && winH > 0) cursorY * (winH.toFloat() / screenHeight) else cursorY
                CallbackBridge.sendCursorPos(targetX, targetY)
            }
        }

        // 1. OpenGL/Vulkan Surface View (Pure rendering surface)
        AndroidView(
            factory = { ctx ->
                SurfaceView(ctx).apply {
                    onSurfaceReady(holder)
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // 2. Zalith-Style Touchpad & Input Controller Layer
        SwitchableMouseLayout(
            modifier = Modifier.fillMaxSize(),
            screenWidth = screenWidth,
            screenHeight = screenHeight,
            cursorMode = gameCursorMode,
            controlMode = mouseControlMode,
            cursorPosition = Offset(cursorX, cursorY),
            cursorSensitivity = launcherSettings.cursorSensitivity / 100f,
            onCursorPositionChange = { newPos ->
                cursorX = newPos.x
                cursorY = newPos.y
            },
            onMouse = {
                isControlVisible = false
            },
            onTouch = {
                // Do not auto-show; GUI visibility is explicitly controlled by the user via the HIDE/SHOW GUI button
            },
            onTap = { pos ->
                val winW = CallbackBridge.windowWidth
                val winH = CallbackBridge.windowHeight
                val targetX = if (screenWidth > 0 && winW > 0) pos.x * (winW.toFloat() / screenWidth) else pos.x
                val targetY = if (screenHeight > 0 && winH > 0) pos.y * (winH.toFloat() / screenHeight) else pos.y
                CallbackBridge.putMouseEventWithCoords(
                    LwjglGlfwKeycode.GLFW_MOUSE_BUTTON_LEFT,
                    targetX,
                    targetY
                )
            },
            onLongPress = {
                isMousePressed = true
                CallbackBridge.putMouseEvent(LwjglGlfwKeycode.GLFW_MOUSE_BUTTON_LEFT, true)
            },
            onLongPressEnd = {
                isMousePressed = false
                CallbackBridge.putMouseEvent(LwjglGlfwKeycode.GLFW_MOUSE_BUTTON_LEFT, false)
            },
            onCapturedMove = { delta ->
                val sens = launcherSettings.captureSensitivity / 100f
                CallbackBridge.sendCursorDelta(delta.x * sens, delta.y * sens)
            }
        )

        // 2b. Game Loading Screen Overlay with Launcher Tips (Muncul sebelum logo Mojang)
        GameLoadingOverlay(
            visible = showLoadingOverlay,
            instanceName = instanceName,
            mcVersion = mcVersion,
            latestLog = liveLogs.lastOrNull() ?: "",
            onClose = { isManualLoadingDismissed = true },
            onViewLog = {
                fpsMode = FpsMode.SHOW_LOG
                isConsoleVisible = true
            },
            modifier = Modifier.fillMaxSize()
        )

        // Hidden IME soft keyboard layer
        if (isKeyboardRequested) {
            HidableInputLayout(
                onClose = {
                    isKeyboardRequested = false
                }
            )
        }

        // 3. Visible Desktop Virtual Cursor Pointer (Zalith Style - automatically visible ONLY in menus)
        val shouldShowPointer = if (mouseControlMode == MouseControlMode.CLICK && launcherSettings.hideMouseInClickMode) {
            false
        } else {
            gameCursorMode == CURSOR_ENABLED && cursorX > 0f && cursorY > 0f
        }

        if (shouldShowPointer) {
            VirtualCursorPointer(
                x = cursorX,
                y = cursorY,
                isPressed = isMousePressed,
                cursorSizeDp = launcherSettings.mouseSizeDp
            )
        }

        // 3. HUD / Live Debug Console Layer (Obsidian Cyber-Glass)
        if (isConsoleVisible) {
            Column(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(16.dp)
                    .width(if (isConsoleExpanded) 420.dp else 300.dp)
                    .background(Color(0xF2090D14), RoundedCornerShape(12.dp))
                    .border(1.5.dp, Color(0x3834D399), RoundedCornerShape(12.dp))
                    .padding(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Clickable FPS badge in console (Klik ke-3 untuk kembali normal)
                    Box(
                        modifier = Modifier.clickable { cycleFpsMode() }
                    ) {
                        NuxBadge(
                            text = if (currentFps > 0) "$currentFps FPS" else "FPS: --",
                            backgroundColor = when {
                                currentFps >= 50 -> Color(0xFF10B981)
                                currentFps >= 25 -> Color(0xFFF59E0B)
                                currentFps > 0 -> Color(0xFFEF4444)
                                else -> Color(0xFF4B5563)
                            },
                            textColor = Color.White
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .clickable { isConsoleExpanded = !isConsoleExpanded }
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = if (isConsoleExpanded) "▲ KECILKAN" else "▼ LOG LENGKAP",
                                color = Color(0xFF34D399),
                                fontWeight = FontWeight.Bold,
                                fontSize = 9.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clickable {
                                    isConsoleVisible = false
                                    fpsMode = FpsMode.NORMAL
                                }
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "✕",
                                color = Color(0xFFA1A1AA),
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                if (isConsoleExpanded) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                    ) {
                        items(liveLogs) { log ->
                            Text(
                                text = log,
                                color = Color(0xFFA7F3D0),
                                fontFamily = FontFamily.Monospace,
                                fontSize = 9.sp,
                                lineHeight = 12.sp
                            )
                        }
                    }
                } else {
                    liveLogs.takeLast(4).forEach { log ->
                        Text(
                            text = log,
                            color = Color(0xFFA7F3D0),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 9.sp,
                            maxLines = 1,
                            lineHeight = 12.sp
                        )
                    }
                }
            }
        }

        // 4. Virtual Controls & Customizable System Controls (FPS, Keyboard, Hide/Show GUI, Close)
        val density = LocalDensity.current
        customButtons.forEach { btn ->
            val btnWidthPx = with(density) { btn.widthDp.dp.toPx() }
            val btnHeightPx = with(density) { btn.heightDp.dp.toPx() }

            val centerX = screenWidth * (btn.xPercent / 100f)
            val centerY = screenHeight * (btn.yPercent / 100f)

            val leftPx = (centerX - btnWidthPx / 2f).coerceIn(0f, (screenWidth - btnWidthPx).coerceAtLeast(0f))
            val topPx = (centerY - btnHeightPx / 2f).coerceIn(0f, (screenHeight - btnHeightPx).coerceAtLeast(0f))

            val leftDp = with(density) { leftPx.toDp() }
            val topDp = with(density) { topPx.toDp() }

            val buttonModifier = Modifier.offset(x = leftDp, y = topDp)

            if (btn.isSystem) {
                when (btn.systemAction) {
                    "FPS" -> {
                        // FPS Indicator Pill (3-Mode Cycle: 1=Pin FPS, 2=Show Log, 3=Normal)
                        val shouldShowFps = when (fpsMode) {
                            FpsMode.NORMAL -> isControlVisible && !isConsoleVisible
                            FpsMode.PINNED -> !isConsoleVisible // Tetap tampil meski isControlVisible == false (GUI di-hide)!
                            FpsMode.SHOW_LOG -> false // Log console aktif (badge FPS ada di header console)
                        }

                        if (shouldShowFps) {
                            val isPinned = fpsMode == FpsMode.PINNED
                            Box(
                                modifier = buttonModifier
                                    .wrapContentWidth()
                                    .defaultMinSize(minWidth = btn.widthDp.dp, minHeight = btn.heightDp.dp)
                                    .alpha(btn.opacity)
                                    .background(
                                        if (isPinned) Color(0xCC091E2A) else Color(0x800A0E17),
                                        RoundedCornerShape(btn.cornerRadiusDp.dp)
                                    )
                                    .border(
                                        1.5.dp,
                                        if (isPinned) Color(0xFF38BDF8) else Color(0x3834D399),
                                        RoundedCornerShape(btn.cornerRadiusDp.dp)
                                    )
                                    .clickable { cycleFpsMode() },
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    if (isPinned) {
                                        Text(
                                            text = "📌",
                                            fontSize = 9.sp,
                                            modifier = Modifier.padding(end = 4.dp)
                                        )
                                    }
                                    Box(
                                        modifier = Modifier
                                            .size(7.dp)
                                            .background(
                                                when {
                                                    currentFps >= 50 -> Color(0xFF10B981)
                                                    currentFps >= 25 -> Color(0xFFF59E0B)
                                                    currentFps > 0 -> Color(0xFFEF4444)
                                                    else -> Color(0xFF6B7280)
                                                },
                                                CircleShape
                                            )
                                    )
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text(
                                        text = if (currentFps > 0) "$currentFps FPS" else "FPS: --",
                                        color = if (isPinned) Color(0xFFBAE6FD) else Color.White,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 11.sp,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                    "KEYBOARD" -> {
                        // Keyboard Toggle Button
                        if (isControlVisible) {
                            val active = isKeyboardRequested
                            Box(
                                modifier = buttonModifier
                                    .size(width = btn.widthDp.dp, height = btn.heightDp.dp)
                                    .alpha(btn.opacity)
                                    .background(
                                        if (active) Color(0xCC10B981) else Color(0x730A0E17),
                                        RoundedCornerShape(btn.cornerRadiusDp.dp)
                                    )
                                    .border(
                                        1.5.dp,
                                        if (active) Color(0xFF34D399) else Color(0x3834D399),
                                        RoundedCornerShape(btn.cornerRadiusDp.dp)
                                    )
                                    .clickable { isKeyboardRequested = !isKeyboardRequested },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "KEYBOARD",
                                    color = if (active) Color(0xFF022C22) else Color.White,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 10.sp,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                    "HIDE_GUI" -> {
                        // Hide/Show GUI Button: ALWAYS VISIBLE so user can restore touch controls!
                        Box(
                            modifier = buttonModifier
                                .size(width = btn.widthDp.dp, height = btn.heightDp.dp)
                                .alpha(btn.opacity)
                                .background(Color(0x730A0E17), RoundedCornerShape(btn.cornerRadiusDp.dp))
                                .border(1.5.dp, Color(0x3834D399), RoundedCornerShape(btn.cornerRadiusDp.dp))
                                .clickable { isControlVisible = !isControlVisible },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (isControlVisible) "HIDE GUI" else "SHOW GUI",
                                color = Color(0xFF34D399),
                                fontWeight = FontWeight.Black,
                                fontSize = 10.sp,
                                maxLines = 1
                            )
                        }
                    }
                    "CLOSE" -> {
                        // Exit Game Button
                        if (isControlVisible) {
                            Box(
                                modifier = buttonModifier
                                    .size(width = btn.widthDp.dp, height = btn.heightDp.dp)
                                    .alpha(btn.opacity)
                                    .background(Color(0x80EF4444).copy(alpha = 0.35f), RoundedCornerShape(btn.cornerRadiusDp.dp))
                                    .border(1.5.dp, Color(0x80EF4444), RoundedCornerShape(btn.cornerRadiusDp.dp))
                                    .clickable { showExitConfirmDialog = true },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "✕",
                                    color = Color(0xFFFCA5A5),
                                    fontWeight = FontWeight.Black,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }
            } else {
                // Regular Minecraft Touch Control Button
                if (isControlVisible) {
                    CustomVirtualButton(
                        button = btn,
                        modifier = buttonModifier
                    )
                }
            }
        }

        // Exit Confirmation Dialog
        if (showExitConfirmDialog) {
            Dialog(
                onDismissRequest = { showExitConfirmDialog = false },
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    NuxCard(
                        modifier = Modifier
                            .width(360.dp)
                            .wrapContentHeight(),
                        backgroundColor = NuxColors.SurfaceWhite,
                        shadowOffset = 5.dp,
                        cornerRadius = NuxSizes.CornerRadiusLarge,
                        fillMaxHeight = false
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp)
                        ) {
                            Text(
                                text = "KELUAR DARI MINECRAFT?",
                                color = NuxColors.DarkGray,
                                fontWeight = FontWeight.Black,
                                fontSize = 15.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Proses JVM Minecraft akan dihentikan dan Anda akan kembali ke menu launcher.",
                                color = NuxColors.GrayNeutral,
                                fontSize = 12.sp,
                                lineHeight = 16.sp
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                NuxButton(
                                    onClick = { showExitConfirmDialog = false },
                                    backgroundColor = NuxColors.SurfaceWhite,
                                    contentColor = NuxColors.DarkGray,
                                    shadowOffset = 2.dp,
                                    cornerRadius = 8.dp,
                                    modifier = Modifier.height(36.dp)
                                ) {
                                    Text("BATAL", fontWeight = FontWeight.Black, fontSize = 11.sp)
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                NuxButton(
                                    onClick = {
                                        showExitConfirmDialog = false
                                        onExit()
                                    },
                                    backgroundColor = NuxColors.Coral,
                                    contentColor = Color.White,
                                    shadowOffset = 2.dp,
                                    cornerRadius = 8.dp,
                                    modifier = Modifier.height(36.dp)
                                ) {
                                    Text("KELUAR ✕", fontWeight = FontWeight.Black, fontSize = 11.sp, color = Color.White)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun VirtualCursorPointer(
    x: Float,
    y: Float,
    isPressed: Boolean = false,
    cursorSizeDp: Int = 24,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val xDp = with(density) { x.toDp() }
    val yDp = with(density) { y.toDp() }

    Box(
        modifier = modifier
            .offset(x = xDp, y = yDp)
            .size(cursorSizeDp.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val scale = (cursorSizeDp / 24f) * (if (isPressed) 0.88f else 1.0f)
            scale(scale, pivot = Offset.Zero) {
                // Classic standard desktop arrow cursor
                val path = Path().apply {
                    moveTo(0f, 0f)
                    lineTo(0f, 22f * density.density)
                    lineTo(5.5f * density.density, 16.5f * density.density)
                    lineTo(10.5f * density.density, 24.5f * density.density)
                    lineTo(13.5f * density.density, 23f * density.density)
                    lineTo(8.5f * density.density, 15f * density.density)
                    lineTo(15.5f * density.density, 15f * density.density)
                    close()
                }

                // 1. Drop shadow
                val shadowPath = Path().apply {
                    addPath(path, Offset(2f * density.density, 2f * density.density))
                }
                drawPath(
                    path = shadowPath,
                    color = Color.Black.copy(alpha = 0.45f),
                    style = Fill
                )

                // 2. White fill
                drawPath(
                    path = path,
                    color = Color.White,
                    style = Fill
                )

                // 3. Crisp black outline
                drawPath(
                    path = path,
                    color = Color.Black,
                    style = Stroke(
                        width = 1.8f * density.density,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )
            }
        }
    }
}

@Composable
fun CustomVirtualButton(
    button: CustomControlButton,
    modifier: Modifier = Modifier
) {
    var isPressed by remember { mutableStateOf(false) }

    if (button.isScroll) {
        val density = LocalDensity.current
        val thresholdPx = with(density) { 14.dp.toPx() }

        Box(
            modifier = modifier
                .size(width = button.widthDp.dp, height = button.heightDp.dp)
                .alpha(button.opacity)
                .background(
                    if (isPressed) Color(0xCC10B981) else Color(0x730A0E17),
                    RoundedCornerShape(button.cornerRadiusDp.dp)
                )
                .border(
                    1.5.dp,
                    if (isPressed) Color(0xFF34D399) else Color(0x3834D399),
                    RoundedCornerShape(button.cornerRadiusDp.dp)
                )
                .pointerInput(button.id) {
                    awaitPointerEventScope {
                        while (true) {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            isPressed = true
                            val startY = down.position.y
                            var totalDragY = 0f
                            var hasDragged = false

                            while (true) {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull { it.id == down.id } ?: break
                                if (!change.pressed) {
                                    if (!hasDragged) {
                                        // Tap: top half scrolls UP, bottom half scrolls DOWN
                                        val h = size.height
                                        if (startY < h / 2f) {
                                            CallbackBridge.sendScroll(0.0, 1.0)
                                        } else {
                                            CallbackBridge.sendScroll(0.0, -1.0)
                                        }
                                    }
                                    break
                                }

                                val dy = change.position.y - change.previousPosition.y
                                totalDragY += dy
                                if (kotlin.math.abs(totalDragY) > 6f) {
                                    hasDragged = true
                                }

                                if (totalDragY <= -thresholdPx) {
                                    // Slide UP -> Scroll UP (hotbar prev)
                                    CallbackBridge.sendScroll(0.0, 1.0)
                                    totalDragY = 0f
                                } else if (totalDragY >= thresholdPx) {
                                    // Slide DOWN -> Scroll DOWN (hotbar next)
                                    CallbackBridge.sendScroll(0.0, -1.0)
                                    totalDragY = 0f
                                }
                                change.consume()
                            }
                            isPressed = false
                        }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(vertical = 4.dp)
            ) {
                Text(
                    text = "▲",
                    color = if (isPressed) Color(0xFF022C22) else Color(0xFF34D399),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = button.name,
                    color = if (isPressed) Color(0xFF022C22) else Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = 9.sp,
                    maxLines = 1
                )
                Text(
                    text = "▼",
                    color = if (isPressed) Color(0xFF022C22) else Color(0xFF34D399),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Black
                )
            }
        }
        return
    }

    fun sendPress(down: Boolean) {
        if (button.isMouseButton) {
            CallbackBridge.putMouseEvent(button.mouseButton, down)
        } else {
            CallbackBridge.sendKeyPress(button.keyCode, down)
        }
    }

    Box(
        modifier = modifier
            .size(width = button.widthDp.dp, height = button.heightDp.dp)
            .alpha(button.opacity)
            .background(
                if (isPressed) Color(0xCC10B981) else Color(0x730A0E17),
                RoundedCornerShape(button.cornerRadiusDp.dp)
            )
            .border(
                1.5.dp,
                if (isPressed) Color(0xFF34D399) else Color(0x3834D399),
                RoundedCornerShape(button.cornerRadiusDp.dp)
            )
            .pointerInput(button.id, button.isToggle) {
                detectTapGestures(
                    onPress = {
                        if (button.isToggle) {
                            isPressed = !isPressed
                            sendPress(isPressed)
                        } else {
                            isPressed = true
                            sendPress(true)
                            tryAwaitRelease()
                            isPressed = false
                            sendPress(false)
                        }
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = button.name,
                color = if (isPressed) Color(0xFF022C22) else Color.White,
                fontWeight = FontWeight.Black,
                fontSize = if (button.name.length > 5) 10.sp else 12.sp,
                maxLines = 1
            )
            if (button.isToggle && isPressed) {
                Spacer(modifier = Modifier.height(2.dp))
                Box(
                    modifier = Modifier
                        .size(4.dp)
                        .background(Color(0xFF022C22), CircleShape)
                )
            }
        }
    }
}

