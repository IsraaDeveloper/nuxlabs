package com.israadev.nuxlauncher

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.view.View
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.israadev.nuxlauncher.core.account.AccountManager
import com.israadev.nuxlauncher.core.instance.InstanceManager
import com.israadev.nuxlauncher.core.mods.NuxAddonImportManager
import com.israadev.nuxlauncher.core.mods.PendingAddonImport
import com.israadev.nuxlauncher.core.settings.SettingsManager
import com.israadev.nuxlauncher.ui.screens.AuthScreen
import com.israadev.nuxlauncher.ui.screens.DashboardScreen
import com.israadev.nuxlauncher.ui.theme.NuxColors
import com.israadev.nuxlauncher.ui.theme.NuxResponsiveTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        hideSystemBars()
        checkAndRequestStoragePermissions()

        handleIncomingAddonIntent(intent)

        // Initialize core engines
        AccountManager.init(this)
        InstanceManager.init(this)
        SettingsManager.init(this)
        com.israadev.nuxlauncher.core.device.PhysicalMouseChecker.initChecker(this)
        com.israadev.nuxlauncher.core.controls.ControlLayoutManager.init(this)
        com.israadev.nuxlauncher.core.renderer.NuxRendererPluginManager.scanPlugins(this)
        com.israadev.nuxlauncher.core.social.NuxVoiceManager.init(this)
        com.israadev.nuxlauncher.core.crash.CrashManager.checkAndNotify(this)

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), 1002)
        }

        setContent {
            val launcherUser by AccountManager.launcherUser.collectAsState()

            // Pengguna yang sudah login (baik Free maupun Premium) langsung masuk ke Dashboard
            val isLoggedIn = launcherUser != null

            NuxResponsiveTheme {
                MaterialTheme {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = NuxColors.Background
                    ) {
                        if (isLoggedIn) {
                            DashboardScreen()
                        } else {
                            AuthScreen(
                                onAuthSuccess = {
                                    // Sesi telah tersimpan di AccountManager, state compose otomatis ter-update
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        hideSystemBars()
        // Re-init instances in case storage permission was just granted
        InstanceManager.init(this)
        com.israadev.nuxlauncher.core.renderer.NuxRendererPluginManager.scanPlugins(this)
        com.israadev.nuxlauncher.core.crash.CrashManager.checkAndNotify(this)
        com.israadev.nuxlauncher.core.social.NuxSocialManager.setInGame(false)
        com.israadev.nuxlauncher.core.social.NuxSocialManager.onAppForeground()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            hideSystemBars()
        }
    }

    override fun onStop() {
        super.onStop()
        com.israadev.nuxlauncher.core.social.NuxVoiceManager.keepAliveInBackground()
        com.israadev.nuxlauncher.core.social.NuxSocialManager.onAppBackground()
    }

    override fun onDestroy() {
        super.onDestroy()
        com.israadev.nuxlauncher.core.skin.OfflineSkinServerManager.stopServer()
        com.israadev.nuxlauncher.core.social.NuxSocialManager.onAppBackground()
    }

    private fun hideSystemBars() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val insetsController = WindowCompat.getInsetsController(window, window.decorView)
        insetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        insetsController.hide(WindowInsetsCompat.Type.systemBars())

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val params = window.attributes
            params.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            window.clearFlags(WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN)
            window.addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN)
            window.attributes = params
        }

        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            or View.SYSTEM_UI_FLAG_FULLSCREEN
        )
    }

    private fun checkAndRequestStoragePermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                try {
                    val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                        data = Uri.parse("package:$packageName")
                    }
                    startActivity(intent)
                } catch (e: Exception) {
                    val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                    startActivity(intent)
                }
            }
        } else {
            val permissions = arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
            val needed = permissions.filter {
                ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
            }
            if (needed.isNotEmpty()) {
                ActivityCompat.requestPermissions(this, needed.toTypedArray(), 1001)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingAddonIntent(intent)
    }

    private fun handleIncomingAddonIntent(intent: Intent?) {
        if (intent == null) return
        val uri = intent.data ?: (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
        }) ?: return

        val aliasClass = intent.component?.className
        val fileName = NuxAddonImportManager.getFileName(this, uri)
        val resolvedType = NuxAddonImportManager.resolveTypeFromAliasOrFile(this, uri, aliasClass)

        NuxAddonImportManager.setPendingImport(
            PendingAddonImport(
                uri = uri,
                fileName = fileName,
                suggestedType = resolvedType
            )
        )
    }
}
